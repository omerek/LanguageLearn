package com.languagelearn.core.input

import com.languagelearn.core.content.ContentIndex
import com.languagelearn.core.model.LearnerSnapshot
import com.languagelearn.core.model.Sentence
import kotlin.math.abs

/**
 * Chooses new sentences at i+1: at most one new chunk and one new structure, difficulty a little
 * above the learner's ability. Relaxes step by step so the learner is never stuck.
 */
class InputSelector(private val index: ContentIndex) {

    private val position: Map<String, Int> =
        index.sentencesInOrder.withIndex().associate { (i, s) -> s.id to i }

    data class Candidate(val sentence: Sentence, val difficulty: Double, val newChunks: List<String>, val score: Double)

    private data class Tier(val below: Double, val above: Double, val enforceNovelty: Boolean)

    private val tiers = listOf(
        Tier(below = 5.0, above = 8.0, enforceNovelty = true),
        Tier(below = 10.0, above = 12.0, enforceNovelty = true),
        Tier(below = 10.0, above = 12.0, enforceNovelty = false),
        Tier(below = 100.0, above = 100.0, enforceNovelty = false),
    )

    fun select(learner: LearnerSnapshot, count: Int): List<Candidate> {
        val chosen = mutableListOf<Candidate>()
        var chunks = learner.introducedChunkIds
        var seen = learner.seenSentenceIds
        var tags = learner.seenTags
        repeat(count) {
            val next = pickOne(learner, chunks, seen, tags) ?: return chosen
            chosen += next
            // Simulate learning the pick so the next pick is i+1 relative to it.
            chunks = chunks + next.sentence.chunkIds
            seen = seen + next.sentence.id
            tags = tags + next.sentence.grammarTags
        }
        return chosen
    }

    private fun pickOne(learner: LearnerSnapshot, chunks: Set<String>, seen: Set<String>, tags: Set<String>): Candidate? {
        val theta = learner.ability
        val target = theta + TARGET_OFFSET
        val unseen = index.sentencesInOrder.filter { it.id !in seen }
        for ((tierIndex, tier) in tiers.withIndex()) {
            val lastResort = tierIndex == tiers.lastIndex
            var rank = 0
            val candidates = unseen.mapNotNull { s ->
                val d = index.difficultyOf(s.id)
                if (d < theta - tier.below || d > theta + tier.above) return@mapNotNull null
                val newChunks = s.chunkIds.filter { it !in chunks }
                val newTags = s.grammarTags.filter { it !in tags }
                if (tier.enforceNovelty && (newChunks.size > 1 || newTags.size > 1)) return@mapNotNull null
                // The curriculum is authored as a sequence of i+1 steps, so inside the window the
                // earliest unseen sentence is the next step. Weak chunks or structures may jump ahead.
                var score = if (lastResort) -abs(d - target) else -(rank++).toDouble()
                if (s.chunkIds.any { it in learner.weakChunkIds }) score += WEAK_CHUNK_BONUS
                if (s.grammarTags.any { it in learner.weakTags }) score += WEAK_TAG_BONUS
                if (!tier.enforceNovelty) score -= 4.0 * newChunks.size
                Candidate(s, d, newChunks, score)
            }
            val best = candidates.maxWithOrNull(compareBy<Candidate> { it.score }.thenBy { -(position[it.sentence.id] ?: 0) })
            if (best != null) return best
        }
        return null
    }

    companion object {
        const val TARGET_OFFSET = 4.0
        const val WEAK_CHUNK_BONUS = 2.0
        const val WEAK_TAG_BONUS = 1.0
    }
}
