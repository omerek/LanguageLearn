package com.languagelearn.core

import com.languagelearn.core.model.CardState
import com.languagelearn.core.model.CardStatus
import com.languagelearn.core.model.ItemType
import com.languagelearn.core.model.Rating
import com.languagelearn.core.srs.Fsrs
import com.languagelearn.core.srs.RatingPolicy
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FsrsTest {
    private val fsrs = Fsrs()
    private val day = Fsrs.DAY
    private val t0 = 1_700_000_000_000L
    private val new = CardState("ch-id-like", ItemType.CHUNK)

    @Test
    fun `retrievability is 90 percent after one stability`() {
        assertTrue(abs(fsrs.retrievability(10.0, 10.0) - 0.9) < 1e-9)
        assertEquals(1.0, fsrs.retrievability(0.0, 5.0))
    }

    @Test
    fun `first review uses initial stability per grade`() {
        val good = fsrs.review(new, Rating.GOOD, t0)
        assertEquals(3.173, good.stability, 1e-9)
        assertEquals(CardStatus.REVIEW, good.status)
        assertEquals(t0 + 3 * day, good.dueAt)
        val again = fsrs.review(new, Rating.AGAIN, t0)
        assertEquals(CardStatus.LEARNING, again.status)
        assertEquals(t0 + 10 * Fsrs.MINUTE, again.dueAt)
    }

    @Test
    fun `better grades give longer intervals`() {
        val base = fsrs.review(new, Rating.GOOD, t0)
        val at = base.dueAt
        val intervals = Rating.entries.map { fsrs.review(base, it, at).dueAt - at }
        assertEquals(intervals.sorted(), intervals)
        assertTrue(intervals[2] > 3 * day, "a successful on-time review must grow the interval")
    }

    @Test
    fun `lapse lowers stability and counts`() {
        val learned = fsrs.review(fsrs.review(new, Rating.GOOD, t0), Rating.GOOD, t0 + 3 * day)
        val lapsed = fsrs.review(learned, Rating.AGAIN, learned.dueAt)
        assertTrue(lapsed.stability < learned.stability)
        assertEquals(1, lapsed.lapses)
        assertEquals(CardStatus.RELEARNING, lapsed.status)
    }

    @Test
    fun `difficulty stays within 1 and 10`() {
        var c = fsrs.review(new, Rating.AGAIN, t0)
        repeat(20) { c = fsrs.review(c, Rating.AGAIN, c.dueAt + day) }
        assertTrue(c.difficulty in 1.0..10.0)
        repeat(20) { c = fsrs.review(c, Rating.EASY, c.dueAt) }
        assertTrue(c.difficulty in 1.0..10.0)
    }

    @Test
    fun `rating policy derives grades from performance`() {
        assertEquals(Rating.AGAIN, RatingPolicy.rate(solved = false, wrongAttempts = 3, responseMs = 9000, tileCount = 3))
        assertEquals(Rating.HARD, RatingPolicy.rate(true, 1, 3000, 3))
        assertEquals(Rating.GOOD, RatingPolicy.rate(true, 0, 4000, 3))
        assertEquals(Rating.EASY, RatingPolicy.rate(true, 0, 2000, 3))
    }
}
