package com.languagelearn.core.content

import com.languagelearn.core.input.DifficultyEstimator
import com.languagelearn.core.model.CefrLevel
import com.languagelearn.core.model.Chunk
import com.languagelearn.core.model.GrammarHint
import com.languagelearn.core.model.HintFile
import com.languagelearn.core.model.LessonUnit
import com.languagelearn.core.model.LevelFile
import com.languagelearn.core.model.Reading
import com.languagelearn.core.model.Sentence

/** Immutable, in-memory view of all content, ordered by curriculum position. */
class ContentIndex(levels: List<LevelFile>, hints: HintFile) {

    val levels: List<LevelFile> = levels.sortedBy { it.level.ordinal }

    /** Units in curriculum order, paired with their level. */
    val units: List<Pair<CefrLevel, LessonUnit>> =
        this.levels.flatMap { lf -> lf.units.sortedBy { it.order }.map { lf.level to it } }

    val sentencesInOrder: List<Sentence> = units.flatMap { it.second.sentences }

    private val chunkById: Map<String, Chunk> =
        units.flatMap { it.second.chunks }.associateBy { it.id }

    private val sentenceById: Map<String, Sentence> = sentencesInOrder.associateBy { it.id }

    private val unitBySentence: Map<String, LessonUnit> =
        units.flatMap { (_, u) -> u.sentences.map { it.id to u } }.toMap()

    private val levelByUnit: Map<String, CefrLevel> = units.associate { (l, u) -> u.id to l }

    private val levelByChunk: Map<String, CefrLevel> =
        units.flatMap { (l, u) -> u.chunks.map { it.id to l } }.toMap()

    private val hintByTag: Map<String, GrammarHint> = hints.hints.associateBy { it.tag }

    val allChunks: Collection<Chunk> get() = chunkById.values
    val allHints: Collection<GrammarHint> get() = hintByTag.values
    val readings: List<Pair<LessonUnit, Reading>> =
        units.flatMap { (_, u) -> u.readings.map { u to it } }

    val difficulty: Map<String, Double> = DifficultyEstimator.estimateAll(this)

    fun chunk(id: String): Chunk? = chunkById[id]
    fun sentence(id: String): Sentence? = sentenceById[id]
    fun unitOf(sentenceId: String): LessonUnit? = unitBySentence[sentenceId]
    fun levelOfUnit(unitId: String): CefrLevel? = levelByUnit[unitId]
    fun levelOfChunk(chunkId: String): CefrLevel? = levelByChunk[chunkId]
    fun hint(tag: String): GrammarHint? = hintByTag[tag]
    fun difficultyOf(sentenceId: String): Double = difficulty[sentenceId] ?: 0.0

    fun sentencesWithChunk(chunkId: String): List<Sentence> =
        sentencesInOrder.filter { s -> s.segments.any { it.chunkId == chunkId } }

    /** Chunks and tags assumed known when a learner starts above A1 (placement). */
    fun chunksBelow(level: CefrLevel): Set<String> =
        units.filter { it.first < level }.flatMap { it.second.chunks.map { c -> c.id } }.toSet()

    fun sentencesBelow(level: CefrLevel): Set<String> =
        units.filter { it.first < level }.flatMap { it.second.sentences.map { s -> s.id } }.toSet()

    fun readingsBelow(level: CefrLevel): Set<String> =
        units.filter { it.first < level }.flatMap { it.second.readings.map { r -> r.id } }.toSet()

    fun tagsBelow(level: CefrLevel): Set<String> =
        units.filter { it.first < level }.flatMap { it.second.sentences.flatMap { s -> s.grammarTags } }.toSet()
}
