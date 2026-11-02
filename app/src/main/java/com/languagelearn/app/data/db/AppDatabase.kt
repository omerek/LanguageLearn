package com.languagelearn.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CardEntity::class,
        ReviewLogEntity::class,
        ProfileEntity::class,
        TagStatEntity::class,
        CompletedReadingEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun profileDao(): ProfileDao
    abstract fun tagStatDao(): TagStatDao
    abstract fun readingDao(): ReadingDao
}
