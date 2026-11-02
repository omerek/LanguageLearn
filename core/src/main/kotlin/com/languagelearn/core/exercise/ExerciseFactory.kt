package com.languagelearn.core.exercise

import com.languagelearn.core.content.ContentIndex
import com.languagelearn.core.content.ContentParser
import com.languagelearn.core.model.Sentence
import kotlin.random.Random

class ExerciseFactory(private val index: ContentIndex) {

    /**
     * Tiles are whole chunks until the learner is advanced (ability >= 60) or the sentence is
     * well known (stability >= 21 days); then segments split into their parts.
     */
    fun assemble(
        sentence: Sentence,
        ability: Double,
        sentenceStabilityDays: Double = 0.0,
        newChunkId: String? = null,
        salt: Int = 0,
    ): Exercise.Assemble {
        val fine = ability >= FINE_TILES_ABILITY || sentenceStabilityDays >= FINE_TILES_STABILITY
        val tilesPerSegment: List<List<String>> = sentence.segments.mapIndexed { i, seg ->
            val texts = if (fine && seg.parts.isNotEmpty()) seg.parts else listOf(seg.text)
            if (i == 0 && !seg.keepCase) texts.mapIndexed { j, t -> if (j == 0) decapitalize(t) else t } else texts
        }
        val solution = tilesPerSegment.flatMapIndexed { i, texts ->
            texts.mapIndexed { j, t -> Tile(id = "s$i.$j", text = stripFinalPunctuation(t), segmentIndex = i) }
        }
        val distractors = sentence.distractors.take(distractorCount(ability)).mapIndexed { k, d ->
            Tile(id = "d$k", text = d.text, segmentIndex = null, distractorTag = d.tag)
        }
        val orders = listOf(sentence.segments.indices.toList()) + sentence.alternatives
        val accepted = orders.map { order ->
            order.flatMap { seg -> solution.filter { it.segmentIndex == seg }.map { ContentParser.normalize(it.text) } }
        }.distinct()
        val tiles = shuffle(solution + distractors, accepted, Random(sentence.id.hashCode() * 31 + salt))
        return Exercise.Assemble(sentence, tiles, accepted, newChunkId)
    }

    /**
     * Cloze for a chunk review. Needs a sentence whose segment text equals the chunk, otherwise
     * returns null and the caller falls back to [assemble].
     */
    fun cloze(chunkId: String, sentence: Sentence, optionPool: Collection<String>, salt: Int = 0): Exercise.Cloze? {
        val chunk = index.chunk(chunkId) ?: return null
        val segIndex = sentence.segments.indexOfFirst {
            it.chunkId == chunkId && ContentParser.normalize(it.text) == ContentParser.normalize(chunk.en)
        }
        if (segIndex < 0) return null
        val random = Random(sentence.id.hashCode() * 17 + salt)
        val sameKind = optionPool.mapNotNull { index.chunk(it) }
            .filter { it.id != chunkId && it.kind == chunk.kind && ContentParser.normalize(it.en) != ContentParser.normalize(chunk.en) }
            .map { it.en }.distinct().shuffled(random)
        val others = optionPool.mapNotNull { index.chunk(it) }
            .filter { it.id != chunkId && it.en !in sameKind && ContentParser.normalize(it.en) != ContentParser.normalize(chunk.en) }
            .map { it.en }.distinct().shuffled(random)
        val wrong = (sameKind + others).take(2)
        if (wrong.size < 2) return null
        val segs = sentence.segments
        val before = segs.take(segIndex).joinToString(" ") { it.text }
        val after = segs.drop(segIndex + 1).joinToString(" ") { it.text }
        val answer = segs[segIndex].text
        return Exercise.Cloze(
            sentence = sentence,
            chunkId = chunkId,
            before = before,
            after = after,
            options = (wrong + answer).shuffled(random),
            answer = answer,
        )
    }

    private fun shuffle(tiles: List<Tile>, accepted: List<List<String>>, random: Random): List<Tile> {
        if (tiles.size < 2) return tiles
        repeat(10) {
            val candidate = tiles.shuffled(random)
            val texts = candidate.filter { !it.isDistractor }.map { ContentParser.normalize(it.text) }
            if (texts !in accepted) return candidate
        }
        return tiles.drop(1) + tiles.first()
    }

    companion object {
        const val FINE_TILES_ABILITY = 60.0
        const val FINE_TILES_STABILITY = 21.0

        fun distractorCount(ability: Double): Int = when {
            ability < 20.0 -> 0
            ability < 60.0 -> 1
            else -> 2
        }

        private val KEEP_CAPITAL = Regex("^I($|'|\\s)")

        fun decapitalize(text: String): String =
            if (text.isEmpty() || KEEP_CAPITAL.containsMatchIn(text) ||
                (text.length > 1 && text[1].isUpperCase())
            ) text else text.replaceFirstChar { it.lowercaseChar() }

        fun stripFinalPunctuation(text: String): String = text.trimEnd('.', '!', '?', ',', ';')
    }
}
