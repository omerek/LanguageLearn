package com.languagelearn.core.model

import kotlinx.serialization.Serializable

/** CEFR levels covered by the app. Each level maps to a 20-point band on the 0-100 ability scale. */
@Serializable
enum class CefrLevel {
    A1, A2, B1, B2, C1;

    val bandStart: Double get() = ordinal * 20.0
    val bandEnd: Double get() = bandStart + 20.0

    companion object {
        fun forAbility(theta: Double): CefrLevel =
            entries[(theta / 20.0).toInt().coerceIn(0, entries.lastIndex)]
    }
}

@Serializable
enum class ChunkKind { FRAME, COLLOCATION, FIXED, CONNECTOR, TIME }

/** Grammatical function of a segment; used to say *where* an ordering error is. */
@Serializable
enum class SegmentRole { SUBJECT, VERB_FRAME, OBJECT, COMPLEMENT, TIME, PLACE, CONNECTOR, CLAUSE, OTHER }

/** One level's content file (a1.json ... c1.json). */
@Serializable
data class LevelFile(
    val level: CefrLevel,
    val units: List<LessonUnit>,
)

@Serializable
data class LessonUnit(
    val id: String,
    val order: Int,
    val titleTr: String,
    val descriptionTr: String,
    val chunks: List<Chunk>,
    val sentences: List<Sentence>,
    val readings: List<Reading> = emptyList(),
)

/** A lexical chunk: the unit of learning and of spaced repetition. */
@Serializable
data class Chunk(
    val id: String,
    val en: String,
    val tr: String,
    val kind: ChunkKind,
    val grammarTags: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
)

@Serializable
data class Sentence(
    val id: String,
    val tr: String,
    val en: String,
    val segments: List<Segment>,
    /** Other correct segment orders, as index permutations of [segments]. */
    val alternatives: List<List<Int>> = emptyList(),
    val distractors: List<Distractor> = emptyList(),
    val grammarTags: List<String>,
) {
    val chunkIds: List<String> get() = segments.mapNotNull { it.chunkId }.distinct()

    /** The structure this sentence is mainly practising (shown in "why correct" tips). */
    val mainTag: String? get() = grammarTags.firstOrNull()
}

@Serializable
data class Segment(
    val text: String,
    val chunkId: String? = null,
    /** Finer tiles used at higher levels. Empty means the segment is never split. */
    val parts: List<String> = emptyList(),
    val role: SegmentRole = SegmentRole.OTHER,
    /** Grammar tag for errors on this segment; falls back to a role-based tag. */
    val tag: String? = null,
    /** Keep the first letter's case on the tile (proper nouns at sentence start). */
    val keepCase: Boolean = false,
)

@Serializable
data class Distractor(
    val text: String,
    val tag: String,
    /** Index of the segment this distractor imitates, if any. */
    val replaces: Int? = null,
)

@Serializable
data class Reading(
    val id: String,
    val titleTr: String,
    val paragraphs: List<Paragraph>,
    val glosses: Map<String, String> = emptyMap(),
)

@Serializable
data class Paragraph(val en: String, val tr: String)

@Serializable
data class GrammarHint(
    val tag: String,
    val titleTr: String,
    val nudgeTr: String,
    val ruleTr: String,
    val onCorrectTr: String,
)

@Serializable
data class HintFile(val hints: List<GrammarHint>)
