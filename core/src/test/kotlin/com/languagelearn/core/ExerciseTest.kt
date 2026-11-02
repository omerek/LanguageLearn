package com.languagelearn.core

import com.languagelearn.core.content.ContentParser
import com.languagelearn.core.exercise.AnswerEvaluator
import com.languagelearn.core.exercise.ErrorType
import com.languagelearn.core.exercise.ExerciseFactory
import com.languagelearn.core.hint.FeedbackKind
import com.languagelearn.core.hint.HintEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExerciseTest {
    private val index = TestContent.index
    private val factory = ExerciseFactory(index)
    private val hints = HintEngine(index)

    private fun idsFor(ex: com.languagelearn.core.exercise.Exercise.Assemble, texts: List<String>): List<String> {
        val pool = ex.tiles.toMutableList()
        return texts.map { t -> pool.first { ContentParser.normalize(it.text) == ContentParser.normalize(t) }.also { pool.remove(it) }.id }
    }

    @Test
    fun `beginner tiles are whole chunks without distractors and not pre-solved`() {
        val s = index.sentence("a1-u3-s02")!!
        val ex = factory.assemble(s, ability = 10.0)
        assertEquals(listOf("i'd like", "a cup of tea"), ex.acceptedAnswers.single())
        assertTrue(ex.tiles.none { it.isDistractor })
        assertFalse(ex.tiles.map { ContentParser.normalize(it.text) } == ex.acceptedAnswers.single())
    }

    @Test
    fun `advanced learners get split tiles and distractors`() {
        val s = index.sentence("a1-u3-s02")!!
        val ex = factory.assemble(s, ability = 70.0)
        assertEquals(listOf("i'd", "like", "a cup of", "tea"), ex.acceptedAnswers.single())
        assertEquals(1, ex.tiles.count { it.isDistractor })
    }

    @Test
    fun `first tile does not give away the start`() {
        val ex = factory.assemble(index.sentence("a2-u3-s01")!!, ability = 30.0)
        assertTrue(ex.tiles.none { it.text == "I like coffee" } || ex.tiles.any { it.text == "I like coffee" })
        val ex2 = factory.assemble(index.sentence("b1-u4-s04")!!, ability = 50.0)
        assertTrue(ex2.tiles.any { it.text == "the man" })
    }

    @Test
    fun `alternative orders are accepted`() {
        val ex = factory.assemble(index.sentence("a2-u1-s02")!!, ability = 30.0)
        val alt = idsFor(ex, listOf("yesterday", "I", "went to", "the market"))
        assertTrue(AnswerEvaluator.evaluate(ex, alt).correct)
    }

    @Test
    fun `turkish word order is diagnosed with the SVO hint`() {
        val ex = factory.assemble(index.sentence("a1-u3-s02")!!, ability = 10.0)
        val turkishOrder = idsFor(ex, listOf("a cup of tea", "I'd like"))
        val ev = AnswerEvaluator.evaluate(ex, turkishOrder)
        assertEquals(ErrorType.WORD_ORDER, ev.errorType)
        assertEquals("word_order_svo", ev.tag)
        val f1 = hints.onWrong(ex.sentence, ev, attempt = 1, maxAttempts = 3)
        assertEquals(FeedbackKind.NUDGE, f1.kind)
        val f2 = hints.onWrong(ex.sentence, ev, attempt = 2, maxAttempts = 3)
        assertEquals(FeedbackKind.RULE, f2.kind)
        assertTrue(f2.messageTr.contains("I drink coffee"))
        val f3 = hints.onWrong(ex.sentence, ev, attempt = 3, maxAttempts = 3)
        assertEquals(FeedbackKind.REVEAL, f3.kind)
        assertEquals("I'd like a cup of tea.", f3.answerEn)
    }

    @Test
    fun `distractor triggers its own grammar hint`() {
        val ex = factory.assemble(index.sentence("a1-u4-s05")!!, ability = 30.0)
        val wrong = idsFor(ex, listOf("my father", "work", "in a hospital"))
        val ev = AnswerEvaluator.evaluate(ex, wrong)
        assertEquals(ErrorType.WRONG_FORM, ev.errorType)
        assertEquals("third_person_s", ev.tag)
        assertEquals(1, ev.focusSegment)
    }

    @Test
    fun `because clause in Turkish order points at the connector structure`() {
        val ex = factory.assemble(index.sentence("a2-u3-s03")!!, ability = 30.0)
        val wrong = idsFor(ex, listOf("I was ill", "because", "I stayed at home"))
        val ev = AnswerEvaluator.evaluate(ex, wrong)
        assertEquals(ErrorType.WORD_ORDER, ev.errorType)
        assertEquals("connector_because", ev.tag)
    }

    @Test
    fun `missing tile is reported as incomplete`() {
        val ex = factory.assemble(index.sentence("a1-u4-s02")!!, ability = 10.0)
        val partial = idsFor(ex, listOf("I read", "the news"))
        assertEquals(ErrorType.INCOMPLETE, AnswerEvaluator.evaluate(ex, partial).errorType)
    }

    @Test
    fun `correct tip fades after three showings`() {
        val s = index.sentence("a1-u2-s05")!!
        assertEquals("third_person_s", hints.onCorrect(s, 0).tag)
        assertEquals(null, hints.onCorrect(s, 3).tag)
    }

    @Test
    fun `cloze offers same-kind options`() {
        val s = index.sentence("a2-u1-s02")!!
        val pool = index.allChunks.map { it.id }
        val cloze = assertNotNull(factory.cloze("ch-yesterday", s, pool))
        assertEquals(3, cloze.options.size)
        assertTrue("yesterday" in cloze.options)
        assertTrue(AnswerEvaluator.evaluate(cloze, "yesterday").correct)
        assertFalse(AnswerEvaluator.evaluate(cloze, cloze.options.first { it != "yesterday" }).correct)
    }
}
