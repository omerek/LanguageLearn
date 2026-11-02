package com.languagelearn.core.input

import com.languagelearn.core.content.ContentIndex
import com.languagelearn.core.content.ContentParser
import com.languagelearn.core.model.LessonUnit
import com.languagelearn.core.model.Reading

/** Known-word coverage for reading texts (comprehensible input needs about 95%). */
class CoverageCalculator(private val index: ContentIndex) {

    fun knownWords(seenSentenceIds: Set<String>, introducedChunkIds: Set<String>): Set<String> {
        val words = FUNCTION_WORDS.toMutableSet()
        seenSentenceIds.mapNotNull { index.sentence(it) }.forEach { words += ContentParser.words(it.en) }
        introducedChunkIds.mapNotNull { index.chunk(it) }.forEach { words += ContentParser.words(it.en) }
        return words
    }

    fun coverage(reading: Reading, known: Set<String>): Double {
        val glossed = reading.glosses.keys.flatMap { ContentParser.words(it) }.toSet()
        val tokens = reading.paragraphs.flatMap { ContentParser.words(it.en) }
        if (tokens.isEmpty()) return 1.0
        return tokens.count { it in known || it in glossed || it.toIntOrNull() != null }.toDouble() / tokens.size
    }

    /** First reading whose unit is at least half seen and whose coverage passes the threshold. */
    fun nextReading(
        seenSentenceIds: Set<String>,
        introducedChunkIds: Set<String>,
        completedReadingIds: Set<String>,
        threshold: Double = THRESHOLD,
    ): Pair<LessonUnit, Reading>? {
        val known = knownWords(seenSentenceIds, introducedChunkIds)
        return index.readings.firstOrNull { (unit, reading) ->
            reading.id !in completedReadingIds &&
                unit.sentences.count { it.id in seenSentenceIds } * 2 >= unit.sentences.size &&
                coverage(reading, known) >= threshold
        }
    }

    companion object {
        const val THRESHOLD = 0.95
        val FUNCTION_WORDS = setOf(
            "a", "an", "the", "and", "or", "but", "so", "to", "of", "in", "on", "at", "for", "with", "from",
            "is", "am", "are", "was", "were", "be", "been", "it", "it's", "i", "you", "he", "she", "we", "they",
            "my", "your", "his", "her", "our", "their", "this", "that", "these", "those", "there", "not",
            "do", "does", "did", "have", "has", "had", "very", "too", "me", "him", "us", "them", "its",
        )
    }
}
