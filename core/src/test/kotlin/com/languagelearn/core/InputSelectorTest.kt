package com.languagelearn.core

import com.languagelearn.core.input.AbilityModel
import com.languagelearn.core.input.InputSelector
import com.languagelearn.core.model.CefrLevel
import com.languagelearn.core.model.LearnerSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InputSelectorTest {
    private val index = TestContent.index
    private val selector = InputSelector(index)

    private fun beginner() = LearnerSnapshot(AbilityModel.initial(CefrLevel.A1), emptySet(), emptySet(), emptySet())

    @Test
    fun `a new learner starts with the first daily pattern`() {
        val picks = selector.select(beginner(), 3)
        assertEquals("a1-u1-s01", picks.first().sentence.id)
        picks.forEach { assertTrue(it.newChunks.size <= 1) }
    }

    @Test
    fun `each pick brings at most one new chunk`() {
        val picks = selector.select(beginner(), 8)
        assertEquals(8, picks.size)
        assertTrue(picks.all { it.newChunks.size <= 1 })
    }

    @Test
    fun `picks stay inside the i+1 window`() {
        val theta = 45.0
        val learner = LearnerSnapshot(
            theta,
            index.chunksBelow(CefrLevel.B1),
            index.units.filter { it.first < CefrLevel.B1 }.flatMap { it.second.sentences.map { s -> s.id } }.toSet(),
            index.tagsBelow(CefrLevel.B1),
        )
        val picks = selector.select(learner, 5)
        assertTrue(picks.isNotEmpty())
        picks.forEach { assertTrue(it.difficulty in (theta - 5)..(theta + 8), "${it.sentence.id} d=${it.difficulty}") }
    }

    @Test
    fun `a weak known chunk pulls its sentence forward`() {
        val a2Start = LearnerSnapshot(
            AbilityModel.initial(CefrLevel.A2),
            index.chunksBelow(CefrLevel.A2) + "ch-yesterday",
            index.units.filter { it.first < CefrLevel.A2 }.flatMap { it.second.sentences.map { s -> s.id } }.toSet(),
            index.tagsBelow(CefrLevel.A2),
        )
        assertEquals("a2-u1-s01", selector.select(a2Start, 1).first().sentence.id)
        val weak = a2Start.copy(weakChunkIds = setOf("ch-yesterday"))
        assertEquals("a2-u1-s02", selector.select(weak, 1).first().sentence.id)
    }

    @Test
    fun `content far below the learner is skipped`() {
        val strong = LearnerSnapshot(50.0, emptySet(), emptySet(), emptySet())
        val pick = selector.select(strong, 1).first()
        assertTrue(pick.difficulty >= 45.0, "picked ${pick.sentence.id} d=${pick.difficulty}")
    }

    @Test
    fun `ability rises after success and falls after failure`() {
        val up = AbilityModel.update(30.0, 34.0, 1.0, 0)
        val down = AbilityModel.update(30.0, 34.0, 0.0, 0)
        assertTrue(up > 30.0 && down < 30.0)
        assertTrue(AbilityModel.update(30.0, 34.0, 1.0, 100) - 30.0 < up - 30.0, "K shrinks after calibration")
    }
}
