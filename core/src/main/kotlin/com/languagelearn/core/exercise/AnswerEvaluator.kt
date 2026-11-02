package com.languagelearn.core.exercise

import com.languagelearn.core.content.ContentParser
import com.languagelearn.core.model.SegmentRole
import com.languagelearn.core.model.Sentence

enum class ErrorType { WRONG_FORM, WORD_ORDER, INCOMPLETE }

data class Evaluation(
    val correct: Boolean,
    val errorType: ErrorType? = null,
    /** Segment the error is on, for underlining and for the role named in the nudge. */
    val focusSegment: Int? = null,
    /** Grammar tag whose hint explains the error. */
    val tag: String? = null,
)

object AnswerEvaluator {

    private val CLAUSE_TAG_PREFIXES = listOf("connector_", "relative_", "time_clause", "conditional", "inversion", "cleft", "participle", "discourse_", "passive")

    fun evaluate(exercise: Exercise.Assemble, chosenTileIds: List<String>): Evaluation {
        val byId = exercise.tiles.associateBy { it.id }
        val chosen = chosenTileIds.mapNotNull { byId[it] }
        val texts = chosen.map { ContentParser.normalize(it.text) }
        if (exercise.acceptedAnswers.any { it == texts }) return Evaluation(correct = true)

        val sentence = exercise.sentence
        chosen.firstOrNull { it.isDistractor }?.let { d ->
            val replaces = sentence.distractors.firstOrNull { it.text == d.text }?.replaces
            return Evaluation(false, ErrorType.WRONG_FORM, replaces, d.distractorTag)
        }

        val solutionTiles = exercise.tiles.filter { !it.isDistractor }
        if (chosen.size < solutionTiles.size) {
            val chosenIds = chosen.map { it.id }.toSet()
            val missing = solutionTiles.first { it.id !in chosenIds }
            val seg = missing.segmentIndex!!
            return Evaluation(false, ErrorType.INCOMPLETE, seg, tagFor(sentence, seg))
        }

        // Same tiles, wrong order: compare with the closest accepted order and find the segments
        // that moved. Blame the most telling one: the verb (Turkish puts it last), then a
        // connector or clause, then time/place, then anything else.
        val closest = exercise.acceptedAnswers.maxBy { lcsKeep(texts, it).count { k -> k } }
        val displaced = displacedSegments(chosen.map { it.segmentIndex ?: -1 }, texts, closest)
        val seg = displaced.minByOrNull { ROLE_PRIORITY.indexOf(sentence.segments[it].role).let { p -> if (p < 0) ROLE_PRIORITY.size else p } }
            ?: chosen.first().segmentIndex ?: 0
        return Evaluation(false, ErrorType.WORD_ORDER, seg, orderTagFor(sentence, seg))
    }

    private val ROLE_PRIORITY = listOf(SegmentRole.VERB_FRAME, SegmentRole.CONNECTOR, SegmentRole.CLAUSE, SegmentRole.TIME, SegmentRole.PLACE)

    /** Segments whose tiles sit at a different position than in [target]. */
    private fun displacedSegments(segments: List<Int>, answer: List<String>, target: List<String>): Set<Int> {
        val used = BooleanArray(target.size)
        val result = mutableSetOf<Int>()
        answer.forEachIndexed { i, text ->
            val j = target.indices.firstOrNull { !used[it] && target[it] == text } ?: return@forEachIndexed
            used[j] = true
            if (i != j && segments[i] >= 0) result += segments[i]
        }
        return result
    }

    fun evaluate(exercise: Exercise.Cloze, choice: String): Evaluation {
        if (ContentParser.normalize(choice) == ContentParser.normalize(exercise.answer)) return Evaluation(true)
        val seg = exercise.sentence.segments.indexOfFirst { it.chunkId == exercise.chunkId }
        return Evaluation(false, ErrorType.WRONG_FORM, seg, tagFor(exercise.sentence, seg))
    }

    /** For form errors: the segment's own tag, else one derived from its role. */
    fun tagFor(sentence: Sentence, segIndex: Int): String? {
        val seg = sentence.segments.getOrNull(segIndex) ?: return sentence.mainTag
        return seg.tag ?: orderTagFor(sentence, segIndex)
    }

    /** For ordering errors the position matters, not the form, so the tag comes from the role. */
    fun orderTagFor(sentence: Sentence, segIndex: Int): String? {
        val seg = sentence.segments.getOrNull(segIndex) ?: return sentence.mainTag
        return when (seg.role) {
            SegmentRole.TIME, SegmentRole.PLACE -> "word_order_time_place"
            SegmentRole.CONNECTOR, SegmentRole.CLAUSE ->
                sentence.grammarTags.firstOrNull { t -> CLAUSE_TAG_PREFIXES.any { t.startsWith(it) } } ?: sentence.mainTag
            else -> "word_order_svo"
        }
    }

    /** Marks which items of [a] belong to one longest common subsequence with [b]. */
    internal fun lcsKeep(a: List<String>, b: List<String>): BooleanArray {
        val dp = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) for (j in b.indices.reversed()) {
            dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
        }
        val keep = BooleanArray(a.size)
        var i = 0
        var j = 0
        while (i < a.size && j < b.size) {
            when {
                a[i] == b[j] -> { keep[i] = true; i++; j++ }
                dp[i + 1][j] >= dp[i][j + 1] -> i++
                else -> j++
            }
        }
        return keep
    }
}
