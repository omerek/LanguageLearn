package com.languagelearn.core.model

enum class ItemType { CHUNK, SENTENCE }

enum class CardStatus { NEW, LEARNING, REVIEW, RELEARNING }

/** FSRS grades. The app derives them from performance; the learner never self-rates. */
enum class Rating(val value: Int) { AGAIN(1), HARD(2), GOOD(3), EASY(4) }

/** Memory state of one chunk or sentence. Times are epoch milliseconds. */
data class CardState(
    val itemId: String,
    val itemType: ItemType,
    val status: CardStatus = CardStatus.NEW,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val dueAt: Long = 0L,
    val lastReviewAt: Long? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
) {
    val id: String get() = cardId(itemType, itemId)

    companion object {
        fun cardId(type: ItemType, itemId: String) = "${type.name}:$itemId"
    }
}

/** Per grammar tag performance; drives weak-structure focus and fading of "why correct" tips. */
data class TagStat(
    val tag: String,
    val attempts: Int = 0,
    val errors: Int = 0,
    val correctTipsShown: Int = 0,
) {
    val errorRate: Double get() = if (attempts == 0) 0.0 else errors.toDouble() / attempts
}

/** What the selection algorithms need to know about the learner, independent of storage. */
data class LearnerSnapshot(
    val ability: Double,
    val introducedChunkIds: Set<String>,
    val seenSentenceIds: Set<String>,
    val seenTags: Set<String>,
    val weakTags: Set<String> = emptySet(),
    val weakChunkIds: Set<String> = emptySet(),
)
