package com.languagelearn.core

import com.languagelearn.core.content.ContentIndex
import com.languagelearn.core.content.ContentParser
import java.io.File

object TestContent {
    private val dir: File by lazy {
        File(System.getProperty("contentDir") ?: "../app/src/main/assets/content")
    }

    val index: ContentIndex by lazy {
        val levels = listOf("a1", "a2", "b1", "b2", "c1").map { ContentParser.parseLevel(File(dir, "$it.json").readText()) }
        ContentIndex(levels, ContentParser.parseHints(File(dir, "hints.json").readText()))
    }
}
