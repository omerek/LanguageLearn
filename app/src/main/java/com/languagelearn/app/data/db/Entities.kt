package com.languagelearn.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cards", indices = [Index("dueAt"), Index("itemType")])
data class CardEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val itemType: String,
    val status: String,
    val stability: Double,
    val difficulty: Double,
    val dueAt: Long,
    val lastReviewAt: Long?,
    val reps: Int,
    val lapses: Int,
)

@Entity(tableName = "review_logs", indices = [Index("cardId")])
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    val rating: Int,
    val reviewedAt: Long,
    val elapsedDays: Double,
    val scheduledDays: Double,
    val responseMs: Long,
    val hintsUsed: Int,
    val errorType: String?,
)

@Entity(tableName = "learner_profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val ability: Double,
    val startLevel: String,
    val dailyNewLimit: Int,
    val streakDays: Int,
    val lastActiveDay: Long,
    val exercisesDone: Int,
    val placementDone: Boolean,
)

@Entity(tableName = "tag_stats")
data class TagStatEntity(
    @PrimaryKey val tag: String,
    val attempts: Int,
    val errors: Int,
    val correctTipsShown: Int,
)

@Entity(tableName = "completed_readings")
data class CompletedReadingEntity(
    @PrimaryKey val readingId: String,
    val completedAt: Long,
)
