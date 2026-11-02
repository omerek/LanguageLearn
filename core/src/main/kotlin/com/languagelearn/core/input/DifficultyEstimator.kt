package com.languagelearn.core.input

import com.languagelearn.core.content.ContentIndex
import com.languagelearn.core.content.ContentParser

/**
 * d = levelBandStart + 20 * ((unitPos - 1) + i/n) / unitsInLevel + clamp(0.25 * (words - 6), -2, 3)
 * Position in the curriculum carries most of the signal; length nudges it.
 */
object DifficultyEstimator {

    fun estimateAll(index: ContentIndex): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for (level in index.levels) {
            val units = level.units.sortedBy { it.order }
            units.forEachIndexed { u, unit ->
                val n = unit.sentences.size.coerceAtLeast(1)
                unit.sentences.forEachIndexed { i, s ->
                    val position = (u + i.toDouble() / n) / units.size
                    val words = ContentParser.words(s.en).size
                    val lengthAdj = (0.25 * (words - 6)).coerceIn(-2.0, 3.0)
                    result[s.id] = (level.level.bandStart + 20.0 * position + lengthAdj).coerceIn(0.0, 100.0)
                }
            }
        }
        return result
    }
}
