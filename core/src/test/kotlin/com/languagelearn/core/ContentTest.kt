package com.languagelearn.core

import com.languagelearn.core.content.ContentValidator
import com.languagelearn.core.input.CoverageCalculator
import com.languagelearn.core.model.CefrLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentTest {
    private val index = TestContent.index

    @Test
    fun `content follows the authoring rules`() {
        val problems = ContentValidator.validate(index)
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `curriculum covers A1 to C1 in 17 units`() {
        assertEquals(CefrLevel.entries.toSet(), index.levels.map { it.level }.toSet())
        assertEquals(17, index.units.size)
    }

    @Test
    fun `difficulty rises through the curriculum`() {
        val perLevel = index.levels.map { lf -> lf.units.flatMap { u -> u.sentences.map { index.difficultyOf(it.id) } }.average() }
        assertEquals(perLevel.sorted(), perLevel)
    }

    @Test
    fun `every reading reaches 95 percent coverage once its unit is done`() {
        val coverage = CoverageCalculator(index)
        for ((unit, reading) in index.readings) {
            val upTo = index.units.indexOfFirst { it.second.id == unit.id }
            val seen = index.units.take(upTo + 1).flatMap { it.second.sentences.map { s -> s.id } }.toSet()
            val chunks = index.units.take(upTo + 1).flatMap { it.second.chunks.map { c -> c.id } }.toSet()
            val known = coverage.knownWords(seen, chunks)
            val c = coverage.coverage(reading, known)
            assertTrue(c >= CoverageCalculator.THRESHOLD, "${reading.id} coverage ${"%.3f".format(c)}")
        }
    }
}
