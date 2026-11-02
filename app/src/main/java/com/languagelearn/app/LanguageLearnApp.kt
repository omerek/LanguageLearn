package com.languagelearn.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.languagelearn.app.data.ContentRepository
import com.languagelearn.app.data.LearningRepository
import com.languagelearn.app.data.db.AppDatabase
import com.languagelearn.core.srs.Fsrs

/** Hand-wired dependencies; small enough that a DI framework would add more than it saves. */
class AppContainer(context: Context) {
    private val database: AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "languagelearn.db").build()

    val fsrs = Fsrs()
    val contentRepository = ContentRepository(context.assets)
    val learningRepository = LearningRepository(database)
}

class LanguageLearnApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
