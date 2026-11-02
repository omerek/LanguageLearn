package com.languagelearn.core

import com.languagelearn.core.input.AbilityModel
import com.languagelearn.core.model.CardState
import com.languagelearn.core.model.CefrLevel
import com.languagelearn.core.model.ItemType
import com.languagelearn.core.model.LearnerSnapshot
import com.languagelearn.core.model.Rating
import com.languagelearn.core.session.SessionItem
import com.languagelearn.core.session.SessionPlanner
import com.languagelearn.core.session.SessionQueue
import com.languagelearn.core.srs.Fsrs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionPlannerTest {
    private val index = TestContent.index
    private val planner = SessionPlanner(index)
    private val now = 1_700_000_000_000L

    @Test
    fun `backlog throttles new items`() {
        assertEquals(8, planner.newLimit(8, 10))
        assertEquals(6, planner.newLimit(8, 40))
        assertEquals(0, planner.newLimit(8, 200))
    }

    @Test
    fun `first session introduces chunks before the sentences that use them`() {
        val learner = LearnerSnapshot(AbilityModel.initial(CefrLevel.A1), emptySet(), emptySet(), emptySet())
        val items = planner.plan(learner, emptyList(), emptySet(), now)
        val introduced = mutableSetOf<String>()
        items.forEach { item ->
            when (item) {
                is SessionItem.NewChunk -> introduced += item.chunkId
                is SessionItem.NewSentence -> index.sentence(item.sentenceId)!!.chunkIds.forEach { assertTrue(it in introduced, "$it before ${item.sentenceId}") }
                else -> Unit
            }
        }
        assertEquals(8, items.count { it is SessionItem.NewSentence })
    }

    @Test
    fun `reviews and new items are interleaved, most forgotten first`() {
        val fsrs = Fsrs()
        val cards = listOf("a1-u1-s01", "a1-u1-s02", "a1-u1-s03", "a1-u1-s04").mapIndexed { i, id ->
            fsrs.review(CardState(id, ItemType.SENTENCE), Rating.GOOD, now - (10 + i) * Fsrs.DAY)
        }
        val learner = LearnerSnapshot(10.0, index.chunksBelow(CefrLevel.A2) - setOf("ch-every-day"),
            setOf("a1-u1-s01", "a1-u1-s02", "a1-u1-s03", "a1-u1-s04"), setOf("be_verb", "article"))
        val items = planner.plan(learner, cards, emptySet(), now)
        val firstReviews = items.take(3)
        assertTrue(firstReviews.all { it is SessionItem.Review })
        assertEquals("a1-u1-s04", (items.first() as SessionItem.Review).card.itemId, "oldest review = lowest retrievability first")
        assertTrue(items[3] !is SessionItem.Review)
    }

    @Test
    fun `failed items come back three steps later at most twice`() {
        val items = (1..6).map { SessionItem.NewSentence("s$it", null) }
        val q = SessionQueue(items)
        q.complete(failed = true)
        assertEquals(listOf("s2", "s3", "s4", "s1"), (0 until 4).map { (q.peek() as SessionItem.NewSentence).sentenceId.also { q.complete(false) } })
    }
}
