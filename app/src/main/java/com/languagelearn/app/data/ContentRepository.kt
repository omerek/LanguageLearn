package com.languagelearn.app.data

import android.content.res.AssetManager
import com.languagelearn.core.content.ContentIndex
import com.languagelearn.core.content.ContentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Loads the bundled JSON once and keeps the index in memory (about 100 KB). */
class ContentRepository(private val assets: AssetManager) {
    private val mutex = Mutex()
    private var cached: ContentIndex? = null

    suspend fun index(): ContentIndex = mutex.withLock {
        cached ?: withContext(Dispatchers.IO) {
            val levels = LEVEL_FILES.map { ContentParser.parseLevel(read("content/$it")) }
            ContentIndex(levels, ContentParser.parseHints(read("content/hints.json")))
        }.also { cached = it }
    }

    private fun read(path: String): String = assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    companion object {
        val LEVEL_FILES = listOf("a1.json", "a2.json", "b1.json", "b2.json", "c1.json")
    }
}
