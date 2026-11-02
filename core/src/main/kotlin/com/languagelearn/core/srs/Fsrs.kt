package com.languagelearn.core.srs

import com.languagelearn.core.model.CardState
import com.languagelearn.core.model.CardStatus
import com.languagelearn.core.model.Rating
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/** FSRS-5 default weights (open-spaced-repetition). Can be re-fitted from review_logs later. */
data class FsrsParameters(
    val w: DoubleArray = doubleArrayOf(
        0.40255, 1.18385, 3.173, 15.69105, 7.1949, 0.5345, 1.4604, 0.0046, 1.54575, 0.1192,
        1.01925, 1.9395, 0.11, 0.29605, 2.2698, 0.2315, 2.9898, 0.51655, 0.6621,
    ),
    val desiredRetention: Double = 0.9,
    val maximumIntervalDays: Int = 365,
    /** After "Again" the card comes back this soon (the session also re-queues it). */
    val relearnDelayMinutes: Int = 10,
) {
    init { require(w.size == 19) { "FSRS-5 needs 19 weights" } }

    override fun equals(other: Any?) = other is FsrsParameters && w.contentEquals(other.w) &&
        desiredRetention == other.desiredRetention && maximumIntervalDays == other.maximumIntervalDays &&
        relearnDelayMinutes == other.relearnDelayMinutes

    override fun hashCode() = w.contentHashCode()
}

class Fsrs(private val p: FsrsParameters = FsrsParameters()) {
    private val w = p.w

    /** Probability of recall after [elapsedDays] for stability [stability]. */
    fun retrievability(elapsedDays: Double, stability: Double): Double =
        if (stability <= 0.0) 0.0 else (1.0 + FACTOR * elapsedDays / stability).pow(DECAY)

    fun retrievabilityAt(card: CardState, now: Long): Double {
        val last = card.lastReviewAt ?: return 0.0
        return retrievability(daysBetween(last, now), card.stability)
    }

    /** Interval (days) at which recall probability falls to the desired retention. */
    fun nextIntervalDays(stability: Double): Long {
        val raw = stability / FACTOR * (p.desiredRetention.pow(1.0 / DECAY) - 1.0)
        return raw.roundToLong().coerceIn(1L, p.maximumIntervalDays.toLong())
    }

    fun initialStability(r: Rating): Double = max(w[r.value - 1], 0.1)

    fun initialDifficulty(r: Rating): Double = clampD(w[4] - exp(w[5] * (r.value - 1)) + 1.0)

    fun nextDifficulty(d: Double, r: Rating): Double {
        val delta = -w[6] * (r.value - 3)
        val damped = d + delta * (10.0 - d) / 9.0
        val reverted = w[7] * initialDifficulty(Rating.EASY) + (1.0 - w[7]) * damped
        return clampD(reverted)
    }

    fun stabilityAfterSuccess(d: Double, s: Double, r: Double, rating: Rating): Double {
        val hardPenalty = if (rating == Rating.HARD) w[15] else 1.0
        val easyBonus = if (rating == Rating.EASY) w[16] else 1.0
        return s * (1.0 + exp(w[8]) * (11.0 - d) * s.pow(-w[9]) * (exp(w[10] * (1.0 - r)) - 1.0) *
            hardPenalty * easyBonus)
    }

    fun stabilityAfterLapse(d: Double, s: Double, r: Double): Double {
        val sf = w[11] * d.pow(-w[12]) * ((s + 1.0).pow(w[13]) - 1.0) * exp(w[14] * (1.0 - r))
        return min(sf, s)
    }

    fun shortTermStability(s: Double, rating: Rating): Double =
        s * exp(w[17] * (rating.value - 3 + w[18]))

    /** Applies one review and returns the new card state. Pure: [now] is passed in. */
    fun review(card: CardState, rating: Rating, now: Long): CardState {
        val first = card.status == CardStatus.NEW || card.lastReviewAt == null
        val (s, d) = if (first) {
            initialStability(rating) to initialDifficulty(rating)
        } else {
            val elapsed = daysBetween(card.lastReviewAt!!, now)
            val r = retrievability(elapsed, card.stability)
            val newD = nextDifficulty(card.difficulty, rating)
            val newS = when {
                elapsed < 1.0 -> shortTermStability(card.stability, rating)
                rating == Rating.AGAIN -> stabilityAfterLapse(card.difficulty, card.stability, r)
                else -> stabilityAfterSuccess(card.difficulty, card.stability, r, rating)
            }
            newS to newD
        }
        val stability = s.coerceIn(0.1, 36500.0)
        val again = rating == Rating.AGAIN
        val due = if (again) now + p.relearnDelayMinutes * MINUTE else now + nextIntervalDays(stability) * DAY
        val status = when {
            again && (card.status == CardStatus.REVIEW || card.status == CardStatus.RELEARNING) -> CardStatus.RELEARNING
            again -> CardStatus.LEARNING
            else -> CardStatus.REVIEW
        }
        return card.copy(
            status = status,
            stability = stability,
            difficulty = d,
            dueAt = due,
            lastReviewAt = now,
            reps = card.reps + 1,
            lapses = card.lapses + if (again && !first) 1 else 0,
        )
    }

    private fun clampD(d: Double) = d.coerceIn(1.0, 10.0)

    companion object {
        const val DECAY = -0.5
        const val FACTOR = 19.0 / 81.0
        const val MINUTE = 60_000L
        const val DAY = 86_400_000L
        fun daysBetween(from: Long, to: Long): Double = max(0.0, (to - from).toDouble() / DAY)
    }
}
