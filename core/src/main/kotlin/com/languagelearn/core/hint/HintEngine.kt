package com.languagelearn.core.hint

import com.languagelearn.core.content.ContentIndex
import com.languagelearn.core.exercise.Evaluation
import com.languagelearn.core.model.SegmentRole
import com.languagelearn.core.model.Sentence

enum class FeedbackKind { CORRECT, NUDGE, RULE, REVEAL }

data class Feedback(
    val kind: FeedbackKind,
    val titleTr: String,
    val messageTr: String,
    /** Full correct sentence, shown on REVEAL and after a correct answer. */
    val answerEn: String? = null,
    val focusSegment: Int? = null,
    val tag: String? = null,
)

/**
 * Short, contextual grammar feedback. Escalates with each wrong attempt
 * (nudge -> rule -> reveal) and fades the "why correct" note after it was shown a few times.
 */
class HintEngine(private val index: ContentIndex) {

    fun onCorrect(sentence: Sentence, correctTipsShownForMainTag: Int): Feedback {
        val tag = sentence.mainTag
        val hint = tag?.let { index.hint(it) }
        val showTip = hint != null && correctTipsShownForMainTag < MAX_CORRECT_TIPS
        return Feedback(
            kind = FeedbackKind.CORRECT,
            titleTr = "Doğru!",
            messageTr = if (showTip) hint!!.onCorrectTr else sentence.tr,
            answerEn = sentence.en,
            tag = if (showTip) tag else null,
        )
    }

    /** [attempt] is the 1-based number of the wrong attempt just made. */
    fun onWrong(sentence: Sentence, evaluation: Evaluation, attempt: Int, maxAttempts: Int): Feedback {
        val hint = evaluation.tag?.let { index.hint(it) } ?: sentence.mainTag?.let { index.hint(it) }
        val title = hint?.let { "İpucu · ${it.titleTr}" } ?: "İpucu"
        val where = evaluation.focusSegment?.let { sentence.segments.getOrNull(it) }?.let { roleNameTr(it.role) }
        return when {
            attempt >= maxAttempts -> Feedback(
                kind = FeedbackKind.REVEAL,
                titleTr = "Doğrusu",
                messageTr = hint?.ruleTr ?: "Doğru cümleyi incele, birazdan tekrar soracağım.",
                answerEn = sentence.en,
                focusSegment = evaluation.focusSegment,
                tag = hint?.tag,
            )
            attempt == 1 -> Feedback(
                kind = FeedbackKind.NUDGE,
                titleTr = title,
                messageTr = listOfNotNull(hint?.nudgeTr, where?.let { "Bak: $it." }).joinToString(" ")
                    .ifBlank { "Bir daha dene." },
                focusSegment = evaluation.focusSegment,
                tag = hint?.tag,
            )
            else -> Feedback(
                kind = FeedbackKind.RULE,
                titleTr = title,
                messageTr = hint?.ruleTr ?: "Öbeklerin sırasını Türkçe cümleyle karşılaştır.",
                focusSegment = evaluation.focusSegment,
                tag = hint?.tag,
            )
        }
    }

    companion object {
        const val MAX_CORRECT_TIPS = 3

        fun roleNameTr(role: SegmentRole): String = when (role) {
            SegmentRole.SUBJECT -> "özne"
            SegmentRole.VERB_FRAME -> "fiil öbeği"
            SegmentRole.OBJECT -> "nesne"
            SegmentRole.COMPLEMENT -> "tümleç"
            SegmentRole.TIME -> "zaman ifadesi"
            SegmentRole.PLACE -> "yer ifadesi"
            SegmentRole.CONNECTOR -> "bağlaç"
            SegmentRole.CLAUSE -> "yan tümce"
            SegmentRole.OTHER -> "işaretli öbek"
        }
    }
}
