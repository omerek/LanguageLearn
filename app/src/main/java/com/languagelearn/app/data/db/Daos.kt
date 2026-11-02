package com.languagelearn.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards")
    suspend fun all(): List<CardEntity>

    @Query("SELECT * FROM cards")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun get(id: String): CardEntity?

    @Upsert
    suspend fun upsert(card: CardEntity)
}

@Dao
interface ReviewLogDao {
    @Insert
    suspend fun insert(log: ReviewLogEntity)

    @Query("SELECT COUNT(*) FROM review_logs WHERE reviewedAt >= :since")
    fun countSince(since: Long): Flow<Int>
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM learner_profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM learner_profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)
}

@Dao
interface TagStatDao {
    @Query("SELECT * FROM tag_stats")
    suspend fun all(): List<TagStatEntity>

    @Query("SELECT * FROM tag_stats")
    fun observeAll(): Flow<List<TagStatEntity>>

    @Query("SELECT * FROM tag_stats WHERE tag = :tag")
    suspend fun get(tag: String): TagStatEntity?

    @Upsert
    suspend fun upsert(stat: TagStatEntity)
}

@Dao
interface ReadingDao {
    @Query("SELECT readingId FROM completed_readings")
    suspend fun completedIds(): List<String>

    @Upsert
    suspend fun upsert(reading: CompletedReadingEntity)
}
