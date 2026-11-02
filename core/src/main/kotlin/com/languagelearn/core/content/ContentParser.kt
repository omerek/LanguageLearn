package com.languagelearn.core.content

import com.languagelearn.core.model.HintFile
import com.languagelearn.core.model.LevelFile
import kotlinx.serialization.json.Json

object ContentParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun parseLevel(text: String): LevelFile = json.decodeFromString(LevelFile.serializer(), text)

    fun parseHints(text: String): HintFile = json.decodeFromString(HintFile.serializer(), text)

    /** Lower-case, strip punctuation, collapse spaces. Used for every answer comparison. */
    fun normalize(text: String): String =
        text.lowercase()
            .replace('’', '\'')
            .replace(Regex("[.,!?;:\"“”]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun words(text: String): List<String> =
        normalize(text).split(' ').filter { it.isNotBlank() }
}
