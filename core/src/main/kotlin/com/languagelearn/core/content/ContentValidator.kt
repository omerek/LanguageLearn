package com.languagelearn.core.content

/**
 * Checks the authoring rules the algorithms rely on. Run by the unit tests over the shipped JSON,
 * so a content mistake fails the build instead of confusing a learner.
 */
object ContentValidator {

    fun validate(index: ContentIndex): List<String> {
        val problems = mutableListOf<String>()
        val ids = mutableSetOf<String>()
        fun unique(id: String) { if (!ids.add(id)) problems += "Duplicate id: $id" }

        val introducedChunks = mutableSetOf<String>()
        val seenTags = mutableSetOf<String>()

        for ((_, unit) in index.units) {
            unique(unit.id)
            unit.chunks.forEach { unique(it.id) }
            for (s in unit.sentences) {
                unique(s.id)
                val joined = ContentParser.normalize(s.segments.joinToString(" ") { it.text })
                if (joined != ContentParser.normalize(s.en)) {
                    problems += "${s.id}: segments '$joined' do not spell '${s.en}'"
                }
                s.segments.forEachIndexed { i, seg ->
                    if (seg.parts.isNotEmpty() &&
                        ContentParser.normalize(seg.parts.joinToString(" ")) != ContentParser.normalize(seg.text)
                    ) problems += "${s.id}: parts of segment $i do not spell '${seg.text}'"
                    if (seg.chunkId != null && index.chunk(seg.chunkId) == null)
                        problems += "${s.id}: unknown chunk ${seg.chunkId}"
                    seg.tag?.let { if (index.hint(it) == null) problems += "${s.id}: no hint for tag $it" }
                }
                (s.grammarTags + s.distractors.map { it.tag }).forEach {
                    if (index.hint(it) == null) problems += "${s.id}: no hint for tag $it"
                }
                s.alternatives.forEach { alt ->
                    if (alt.sorted() != s.segments.indices.toList())
                        problems += "${s.id}: alternative $alt is not a permutation"
                }
                s.distractors.forEach { d ->
                    if (s.segments.any { ContentParser.normalize(it.text) == ContentParser.normalize(d.text) })
                        problems += "${s.id}: distractor '${d.text}' equals a correct tile"
                }
                // i+1 authoring rule: in curriculum order a sentence brings at most one new chunk
                // and at most one new grammar structure.
                val newChunks = s.chunkIds.filter { it !in introducedChunks }
                if (newChunks.size > 1) problems += "${s.id}: introduces ${newChunks.size} new chunks $newChunks"
                val newTags = s.grammarTags.filter { it !in seenTags }
                if (newTags.size > 1) problems += "${s.id}: introduces ${newTags.size} new tags $newTags"
                introducedChunks += s.chunkIds
                seenTags += s.grammarTags
            }
            for (c in unit.chunks) {
                if (index.sentencesWithChunk(c.id).isEmpty()) problems += "Chunk ${c.id} is never used in a sentence"
                c.grammarTags.forEach { if (index.hint(it) == null) problems += "${c.id}: no hint for tag $it" }
            }
            unit.readings.forEach { unique(it.id) }
        }
        return problems
    }
}
