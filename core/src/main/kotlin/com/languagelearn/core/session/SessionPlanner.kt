package com.languagelearn.core.session

import com.languagelearn.core.content.ContentIndex
import com.languagelearn.core.input.CoverageCalculator
import com.languagelearn.core.input.InputSelector
import com.languagelearn.core.model.CardState
import com.languagelearn.core.model.LearnerSnapshot
import com.languagelearn.core.srs.Fsrs

sealed interface SessionItem {
    data class Review(val card: CardState) : SessionItem
    data class NewChunk(val chunkId: String) : SessionItem
    data class NewSentence(val sentenceId: String, val newChunkId: String?) : SessionItem
    data class ReadText(val readingId: String) : SessionItem
}

class SessionPlanner(
    private val index: ContentIndex,
    private val fsrs: Fsrs = Fsrs(),
) {
    private val selector = InputSelector(index)
    private val coverage = CoverageCalculator(index)

    fun newLimit(dailyNew: Int, dueCount: Int): Int =
        (dailyNew - (dueCount - BACKLOG_FREE).coerceAtLeast(0) / BACKLOG_DIVISOR).coerceAtLeast(0)

    fun plan(
        learner: LearnerSnapshot,
        cards: List<CardState>,
        completedReadingIds: Set<String>,
        now: Long,
        dailyNew: Int = DEFAULT_DAILY_NEW,
    ): List<SessionItem> {
        val due = cards.filter { it.dueAt <= now }
        val reviews = due.sortedBy { fsrs.retrievabilityAt(it, now) }.take(MAX_REVIEWS)
        val picks = selector.select(learner, newLimit(dailyNew, due.size))

        val newBlocks: List<List<SessionItem>> = picks.map { c ->
            val newChunk = c.newChunks.firstOrNull()
            buildList {
                c.newChunks.forEach { add(SessionItem.NewChunk(it)) }
                add(SessionItem.NewSentence(c.sentence.id, newChunk))
            }
        }

        // Interleave: after every REVIEWS_PER_NEW reviews, one new block.
        val items = mutableListOf<SessionItem>()
        val reviewQueue = ArrayDeque(reviews.map { SessionItem.Review(it) })
        val newQueue = ArrayDeque(newBlocks)
        while (reviewQueue.isNotEmpty() || newQueue.isNotEmpty()) {
            repeat(REVIEWS_PER_NEW) { reviewQueue.removeFirstOrNull()?.let(items::add) }
            newQueue.removeFirstOrNull()?.let(items::addAll)
        }

        val seenAfter = learner.seenSentenceIds + picks.map { it.sentence.id }
        val chunksAfter = learner.introducedChunkIds + picks.flatMap { it.sentence.chunkIds }
        coverage.nextReading(seenAfter, chunksAfter, completedReadingIds)?.let { (_, r) ->
            items += SessionItem.ReadText(r.id)
        }
        return items
    }

    companion object {
        const val DEFAULT_DAILY_NEW = 8
        const val MAX_REVIEWS = 50
        const val REVIEWS_PER_NEW = 3
        const val BACKLOG_FREE = 30
        const val BACKLOG_DIVISOR = 5
    }
}

/** Runs a planned session; failed items come back a few steps later, at most twice. */
class SessionQueue(items: List<SessionItem>) {
    private val queue = ArrayDeque(items)
    private val requeues = mutableMapOf<SessionItem, Int>()
    var completed: Int = 0
        private set

    val remaining: Int get() = queue.size
    fun peek(): SessionItem? = queue.firstOrNull()

    fun complete(failed: Boolean) {
        val item = queue.removeFirstOrNull() ?: return
        completed++
        if (failed && item !is SessionItem.NewChunk && item !is SessionItem.ReadText) {
            val n = requeues.getOrDefault(item, 0)
            if (n < MAX_REQUEUES) {
                requeues[item] = n + 1
                queue.add(minOf(REQUEUE_GAP, queue.size), item)
            }
        }
    }

    companion object {
        const val REQUEUE_GAP = 3
        const val MAX_REQUEUES = 2
    }
}
