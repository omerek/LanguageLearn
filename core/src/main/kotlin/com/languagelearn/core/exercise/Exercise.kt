package com.languagelearn.core.exercise

import com.languagelearn.core.model.Chunk
import com.languagelearn.core.model.LessonUnit
import com.languagelearn.core.model.Reading
import com.languagelearn.core.model.Sentence

data class Tile(
    val id: String,
    val text: String,
    /** Segment this tile belongs to; null for distractors. */
    val segmentIndex: Int?,
    val distractorTag: String? = null,
) {
    val isDistractor: Boolean get() = segmentIndex == null
}

sealed interface Exercise {
    val maxAttempts: Int

    /** Shows a new chunk with its meaning and examples. Creates the chunk's card. */
    data class ChunkIntro(val chunk: Chunk) : Exercise {
        override val maxAttempts = 1
    }

    /** Build the English sentence from shuffled chunk tiles, given the Turkish sentence. */
    data class Assemble(
        val sentence: Sentence,
        val tiles: List<Tile>,
        /** Every accepted answer as a sequence of normalised tile texts. */
        val acceptedAnswers: List<List<String>>,
        val newChunkId: String?,
        override val maxAttempts: Int = 3,
    ) : Exercise {
        val solutionTileCount: Int get() = tiles.count { !it.isDistractor }
    }

    /** Choose the missing chunk in a known sentence. */
    data class Cloze(
        val sentence: Sentence,
        val chunkId: String,
        val before: String,
        val after: String,
        val options: List<String>,
        val answer: String,
        override val maxAttempts: Int = 2,
    ) : Exercise

    /** Comprehensible-input reading with tap-to-gloss. Not graded. */
    data class Read(val unit: LessonUnit, val reading: Reading) : Exercise {
        override val maxAttempts = 1
    }
}
