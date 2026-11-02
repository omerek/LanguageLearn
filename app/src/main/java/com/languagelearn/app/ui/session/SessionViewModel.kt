package com.languagelearn.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.languagelearn.app.AppContainer
import com.languagelearn.core.content.ContentIndex
import com.languagelearn.core.exercise.AnswerEvaluator
import com.languagelearn.core.exercise.Evaluation
import com.languagelearn.core.exercise.Exercise
import com.languagelearn.core.exercise.ExerciseFactory
import com.languagelearn.core.hint.Feedback
import com.languagelearn.core.hint.FeedbackKind
import com.languagelearn.core.hint.HintEngine
import com.languagelearn.core.input.AbilityModel
import com.languagelearn.core.model.CardState
import com.languagelearn.core.model.CefrLevel
import com.languagelearn.core.model.ItemType
import com.languagelearn.core.model.Rating
import com.languagelearn.core.model.Sentence
import com.languagelearn.core.session.SessionItem
import com.languagelearn.core.session.SessionPlanner
import com.languagelearn.core.session.SessionQueue
import com.languagelearn.core.srs.RatingPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** What the session screen draws. Immutable; every user action produces a new value. */
data class SessionUiState(
    val loading: Boolean = true,
    val done: Int = 0,
    val total: Int = 0,
    val exercise: Exercise? = null,
    /** Assemble: tile ids placed in the answer tray, in order. */
    val trayTileIds: List<String> = emptyList(),
    /** Cloze: options already tried and wrong. */
    val wrongOptions: Set<String> = emptySet(),
    val feedback: Feedback? = null,
    /** True once the exercise is settled (correct or revealed); the button then says "Devam". */
    val settled: Boolean = false,
    val summary: SessionSummary? = null,
)

data class SessionSummary(val exercises: Int, val firstTry: Int, val newChunks: Int, val weakTagTitles: List<String>)

class SessionViewModel(container: AppContainer) : ViewModel() {
    private val content = container.contentRepository
    private val repo = container.learningRepository
    private val fsrs = container.fsrs

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private lateinit var index: ContentIndex
    private lateinit var factory: ExerciseFactory
    private lateinit var hints: HintEngine
    private lateinit var queue: SessionQueue
    private var ability = 0.0
    private var exercisesDone = 0

    // Per-exercise bookkeeping.
    private var startedAt = 0L
    private var wrongAttempts = 0
    private val failedTags = mutableSetOf<String>()
    private var lastEvaluation: Evaluation? = null

    // Session totals.
    private var graded = 0
    private var firstTry = 0
    private var newChunks = 0
    private val seenSentences = mutableSetOf<String>()
    private val failedTagsSession = mutableListOf<String>()
    private var lastRating: Rating? = null

    // Declared after every property so they are initialised before the coroutine can run.
    init {
        viewModelScope.launch { start() }
    }

    private suspend fun start() {
        index = content.index()
        factory = ExerciseFactory(index)
        hints = HintEngine(index)
        val profile = repo.getProfile() ?: return
        ability = profile.ability
        exercisesDone = profile.exercisesDone
        val snapshot = repo.snapshot(index)
        seenSentences += snapshot.seenSentenceIds
        val planner = SessionPlanner(index, fsrs)
        // Readings below the placement level count as done, like the content they are built from.
        val doneReadings = repo.completedReadings() + index.readingsBelow(CefrLevel.valueOf(profile.startLevel))
        val items = planner.plan(snapshot, repo.cards(), doneReadings, now(), profile.dailyNewLimit)
        queue = SessionQueue(items)
        showNext()
    }

    private suspend fun showNext() {
        val item = queue.peek()
        if (item == null) {
            finish()
            return
        }
        wrongAttempts = 0
        failedTags.clear()
        lastEvaluation = null
        startedAt = now()
        val exercise = buildExercise(item)
        if (exercise == null) {
            // Nothing sensible to show for this item (e.g. a chunk without a usable sentence).
            queue.complete(failed = false)
            showNext()
            return
        }
        _state.value = SessionUiState(
            loading = false,
            done = queue.completed,
            total = queue.completed + queue.remaining,
            exercise = exercise,
        )
    }

    private suspend fun buildExercise(item: SessionItem): Exercise? = when (item) {
        is SessionItem.NewChunk -> index.chunk(item.chunkId)?.let { Exercise.ChunkIntro(it) }
        is SessionItem.NewSentence -> index.sentence(item.sentenceId)?.let {
            factory.assemble(it, ability, 0.0, item.newChunkId, salt = queue.completed)
        }
        is SessionItem.ReadText -> index.readings.firstOrNull { it.second.id == item.readingId }
            ?.let { (u, r) -> Exercise.Read(u, r) }
        is SessionItem.Review -> {
            val card = repo.card(item.card.itemType, item.card.itemId) ?: item.card
            when (card.itemType) {
                ItemType.SENTENCE -> index.sentence(card.itemId)?.let {
                    factory.assemble(it, ability, card.stability, salt = queue.completed)
                }
                ItemType.CHUNK -> chunkReview(card)
            }
        }
    }

    /** A chunk is reviewed in context: cloze in a known sentence, else rebuilding such a sentence. */
    private suspend fun chunkReview(card: CardState): Exercise? {
        val known = index.sentencesWithChunk(card.itemId).filter { it.id in seenSentences }
        val pool = repo.cards().filter { it.itemType == ItemType.CHUNK }.map { it.itemId }
        known.forEach { s -> factory.cloze(card.itemId, s, pool, salt = queue.completed)?.let { return it } }
        known.firstOrNull()?.let { return factory.assemble(it, ability, 0.0, salt = queue.completed) }
        return index.chunk(card.itemId)?.let { Exercise.ChunkIntro(it) }
    }

    // ---- User actions -------------------------------------------------------------------------

    fun onTileTapped(tileId: String) = _state.update { s ->
        if (s.settled || s.exercise !is Exercise.Assemble) s
        else if (tileId in s.trayTileIds) s.copy(trayTileIds = s.trayTileIds - tileId, feedback = s.feedback?.takeIf { it.kind != FeedbackKind.NUDGE })
        else s.copy(trayTileIds = s.trayTileIds + tileId)
    }

    fun onCheck() {
        val s = _state.value
        val ex = s.exercise as? Exercise.Assemble ?: return
        if (s.settled || s.trayTileIds.isEmpty()) return
        val evaluation = AnswerEvaluator.evaluate(ex, s.trayTileIds)
        handleEvaluation(ex.sentence, evaluation, ex.maxAttempts, ex.solutionTileCount)
    }

    fun onClozeOption(option: String) {
        val s = _state.value
        val ex = s.exercise as? Exercise.Cloze ?: return
        if (s.settled || option in s.wrongOptions) return
        val evaluation = AnswerEvaluator.evaluate(ex, option)
        if (!evaluation.correct) _state.update { it.copy(wrongOptions = it.wrongOptions + option) }
        handleEvaluation(ex.sentence, evaluation, ex.maxAttempts, tileCount = 1)
    }

    private fun handleEvaluation(sentence: Sentence, evaluation: Evaluation, maxAttempts: Int, tileCount: Int) {
        viewModelScope.launch {
            if (evaluation.correct) {
                val tipsShown = sentence.mainTag?.let { repo.tagStat(it).correctTipsShown } ?: 0
                val feedback = hints.onCorrect(sentence, tipsShown)
                feedback.tag?.let { repo.markCorrectTipShown(it) }
                _state.update { it.copy(feedback = feedback, settled = true) }
                settle(solved = true, tileCount = tileCount)
            } else {
                wrongAttempts++
                lastEvaluation = evaluation
                evaluation.tag?.let {
                    failedTags += it
                    failedTagsSession += it
                }
                val feedback = hints.onWrong(sentence, evaluation, wrongAttempts, maxAttempts)
                val revealed = feedback.kind == FeedbackKind.REVEAL
                _state.update { it.copy(feedback = feedback, settled = revealed) }
                if (revealed) settle(solved = false, tileCount = tileCount)
            }
        }
    }

    /** Grades the exercise and writes cards, logs, tag stats and ability. */
    private suspend fun settle(solved: Boolean, tileCount: Int) {
        val now = now()
        val responseMs = now - startedAt
        val rating = RatingPolicy.rate(solved, wrongAttempts, responseMs, tileCount)
        graded++
        if (solved && wrongAttempts == 0) firstTry++

        val item = queue.peek() ?: return
        val exercise = _state.value.exercise
        val sentence = when (exercise) {
            is Exercise.Assemble -> exercise.sentence
            is Exercise.Cloze -> exercise.sentence
            else -> null
        } ?: return

        // 1. The item's own card.
        val primary: Pair<ItemType, String> = when (item) {
            is SessionItem.Review -> item.card.itemType to item.card.itemId
            else -> ItemType.SENTENCE to sentence.id
        }
        review(primary.first, primary.second, rating, now, responseMs)

        // 2. Chunks of the sentence: due ones are reviewed in context; the chunk the error was on lapses.
        val errorChunk = lastEvaluation?.focusSegment?.let { sentence.segments.getOrNull(it)?.chunkId }
        for (chunkId in sentence.chunkIds) {
            if (primary.first == ItemType.CHUNK && primary.second == chunkId) continue
            val card = repo.card(ItemType.CHUNK, chunkId) ?: continue
            when {
                chunkId == errorChunk -> review(ItemType.CHUNK, chunkId, Rating.AGAIN, now, responseMs)
                card.dueAt <= now -> review(ItemType.CHUNK, chunkId, rating, now, responseMs)
            }
        }

        // 3. Ability, tag statistics.
        if (exercise is Exercise.Assemble) {
            ability = AbilityModel.update(ability, index.difficultyOf(sentence.id), RatingPolicy.outcome(rating), exercisesDone)
            exercisesDone++
            repo.updateAbility(ability)
        }
        repo.recordTagResults(practised = sentence.grammarTags, failed = failedTags)
        seenSentences += sentence.id
        lastRating = rating
    }

    private suspend fun review(type: ItemType, itemId: String, rating: Rating, now: Long, responseMs: Long) {
        val before = repo.card(type, itemId) ?: CardState(itemId, type)
        val after = fsrs.review(before, rating, now)
        repo.saveReview(before, after, rating, now, responseMs, hintsUsed = wrongAttempts, errorType = lastEvaluation?.errorType?.name)
    }

    /** "Devam" on intro, reading, or a settled exercise. */
    fun onContinue() {
        viewModelScope.launch {
            val s = _state.value
            when (val ex = s.exercise) {
                is Exercise.ChunkIntro -> {
                    if (repo.card(ItemType.CHUNK, ex.chunk.id) == null) newChunks++
                    repo.introduce(ItemType.CHUNK, ex.chunk.id, now())
                    queue.complete(failed = false)
                }
                is Exercise.Read -> {
                    repo.completeReading(ex.reading.id, now())
                    queue.complete(failed = false)
                }
                else -> {
                    if (!s.settled) return@launch
                    queue.complete(failed = lastRating == Rating.AGAIN)
                }
            }
            lastRating = null
            showNext()
        }
    }

    private suspend fun finish() {
        repo.touchStreak(LocalDate.now().toEpochDay())
        val weak = failedTagsSession.mapNotNull { index.hint(it)?.titleTr }.distinct().take(3)
        _state.value = SessionUiState(
            loading = false,
            done = queue.completed,
            total = queue.completed,
            summary = SessionSummary(graded, firstTry, newChunks, weak),
        )
    }

    private fun now() = System.currentTimeMillis()
}
