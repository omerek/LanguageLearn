package com.languagelearn.app.data

import com.languagelearn.app.data.db.AppDatabase
import com.languagelearn.app.data.db.CardEntity
import com.languagelearn.app.data.db.CompletedReadingEntity
import com.languagelearn.app.data.db.ProfileEntity
import com.languagelearn.app.data.db.ReviewLogEntity
import com.languagelearn.app.data.db.TagStatEntity
import com.languagelearn.core.content.ContentIndex
import com.languagelearn.core.input.AbilityModel
import com.languagelearn.core.model.CardState
import com.languagelearn.core.model.CardStatus
import com.languagelearn.core.model.CefrLevel
import com.languagelearn.core.model.ItemType
import com.languagelearn.core.model.LearnerSnapshot
import com.languagelearn.core.model.Rating
import com.languagelearn.core.model.TagStat
import com.languagelearn.core.session.SessionPlanner
import com.languagelearn.core.srs.Fsrs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** All learner state. Maps Room rows to the storage-free models in :core. */
class LearningRepository(private val db: AppDatabase) {
    private val cards = db.cardDao()
    private val logs = db.reviewLogDao()
    private val profiles = db.profileDao()
    private val tags = db.tagStatDao()
    private val readings = db.readingDao()

    val profile: Flow<ProfileEntity?> = profiles.observe()
    val allCards: Flow<List<CardState>> = cards.observeAll().map { list -> list.map { it.toModel() } }
    val allTagStats: Flow<List<TagStat>> = tags.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getProfile(): ProfileEntity? = profiles.get()

    suspend fun savePlacement(level: CefrLevel, today: Long) {
        profiles.upsert(
            ProfileEntity(
                ability = AbilityModel.initial(level),
                startLevel = level.name,
                dailyNewLimit = SessionPlanner.DEFAULT_DAILY_NEW,
                streakDays = 0,
                lastActiveDay = today - 1,
                exercisesDone = 0,
                placementDone = true,
            ),
        )
    }

    /**
     * What the algorithms see. Content below the placement level counts as known (it is not put
     * into spaced repetition) so a B1 starter is not sent back to A1.
     */
    suspend fun snapshot(index: ContentIndex): LearnerSnapshot {
        val profile = profiles.get() ?: error("Placement not done")
        val start = CefrLevel.valueOf(profile.startLevel)
        val all = cards.all().map { it.toModel() }
        val stats = tags.all().map { it.toModel() }
        return LearnerSnapshot(
            ability = profile.ability,
            introducedChunkIds = all.filter { it.itemType == ItemType.CHUNK }.map { it.itemId }.toSet() + index.chunksBelow(start),
            seenSentenceIds = all.filter { it.itemType == ItemType.SENTENCE }.map { it.itemId }.toSet() + index.sentencesBelow(start),
            seenTags = stats.filter { it.attempts > 0 }.map { it.tag }.toSet() + index.tagsBelow(start),
            weakTags = stats.filter { it.attempts >= WEAK_MIN_ATTEMPTS && it.errorRate > WEAK_ERROR_RATE }.map { it.tag }.toSet(),
            weakChunkIds = all.filter { it.itemType == ItemType.CHUNK && (it.lapses >= 2 || it.difficulty > 7.0) }.map { it.itemId }.toSet(),
        )
    }

    suspend fun cards(): List<CardState> = cards.all().map { it.toModel() }

    suspend fun card(type: ItemType, itemId: String): CardState? = cards.get(CardState.cardId(type, itemId))?.toModel()

    /** Creates the card for a newly introduced item, due now so its first exercise grades it. */
    suspend fun introduce(type: ItemType, itemId: String, now: Long) {
        if (card(type, itemId) == null) {
            cards.upsert(CardState(itemId, type, status = CardStatus.NEW, dueAt = now).toEntity())
        }
    }

    suspend fun saveReview(
        before: CardState,
        after: CardState,
        rating: Rating,
        now: Long,
        responseMs: Long,
        hintsUsed: Int,
        errorType: String?,
    ) {
        cards.upsert(after.toEntity())
        logs.insert(
            ReviewLogEntity(
                cardId = after.id,
                rating = rating.value,
                reviewedAt = now,
                elapsedDays = before.lastReviewAt?.let { Fsrs.daysBetween(it, now) } ?: 0.0,
                scheduledDays = (after.dueAt - now).toDouble() / Fsrs.DAY,
                responseMs = responseMs,
                hintsUsed = hintsUsed,
                errorType = errorType,
            ),
        )
    }

    suspend fun tagStat(tag: String): TagStat = tags.get(tag)?.toModel() ?: TagStat(tag)

    suspend fun recordTagResults(practised: Collection<String>, failed: Collection<String>) {
        for (tag in (practised + failed).toSet()) {
            val s = tagStat(tag)
            tags.upsert(s.copy(attempts = s.attempts + 1, errors = s.errors + if (tag in failed) 1 else 0).toEntity())
        }
    }

    suspend fun markCorrectTipShown(tag: String) {
        val s = tagStat(tag)
        tags.upsert(s.copy(correctTipsShown = s.correctTipsShown + 1).toEntity())
    }

    suspend fun updateAbility(ability: Double) {
        val p = profiles.get() ?: return
        profiles.upsert(p.copy(ability = ability, exercisesDone = p.exercisesDone + 1))
    }

    /** Streak counts consecutive days with at least one finished session. */
    suspend fun touchStreak(today: Long) {
        val p = profiles.get() ?: return
        val streak = when (p.lastActiveDay) {
            today -> p.streakDays
            today - 1 -> p.streakDays + 1
            else -> 1
        }
        profiles.upsert(p.copy(streakDays = streak, lastActiveDay = today))
    }

    suspend fun completedReadings(): Set<String> = readings.completedIds().toSet()

    suspend fun completeReading(id: String, now: Long) = readings.upsert(CompletedReadingEntity(id, now))

    companion object {
        const val WEAK_MIN_ATTEMPTS = 3
        const val WEAK_ERROR_RATE = 0.3
    }
}

private fun CardEntity.toModel() = CardState(
    itemId = itemId,
    itemType = ItemType.valueOf(itemType),
    status = CardStatus.valueOf(status),
    stability = stability,
    difficulty = difficulty,
    dueAt = dueAt,
    lastReviewAt = lastReviewAt,
    reps = reps,
    lapses = lapses,
)

private fun CardState.toEntity() = CardEntity(
    id = id,
    itemId = itemId,
    itemType = itemType.name,
    status = status.name,
    stability = stability,
    difficulty = difficulty,
    dueAt = dueAt,
    lastReviewAt = lastReviewAt,
    reps = reps,
    lapses = lapses,
)

private fun TagStatEntity.toModel() = TagStat(tag, attempts, errors, correctTipsShown)
private fun TagStat.toEntity() = TagStatEntity(tag, attempts, errors, correctTipsShown)
