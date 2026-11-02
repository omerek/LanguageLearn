package com.languagelearn.core.srs

import com.languagelearn.core.model.Rating

/**
 * Derives the FSRS grade from what the learner did, so there is no self-rating button.
 * - revealed after the last attempt -> AGAIN
 * - correct after a wrong attempt or a hint -> HARD
 * - correct first time -> GOOD, or EASY when clearly faster than expected
 */
object RatingPolicy {
    const val BASE_MS = 1_500L
    const val PER_TILE_MS = 1_200L
    const val EASY_FRACTION = 0.6

    fun expectedMs(tileCount: Int): Long = BASE_MS + PER_TILE_MS * tileCount

    fun rate(solved: Boolean, wrongAttempts: Int, responseMs: Long, tileCount: Int): Rating = when {
        !solved -> Rating.AGAIN
        wrongAttempts > 0 -> Rating.HARD
        responseMs < expectedMs(tileCount) * EASY_FRACTION -> Rating.EASY
        else -> Rating.GOOD
    }

    /** Elo outcome for the ability model: 1, 0.5 or 0. */
    fun outcome(rating: Rating): Double = when (rating) {
        Rating.AGAIN -> 0.0
        Rating.HARD -> 0.5
        Rating.GOOD, Rating.EASY -> 1.0
    }
}
