package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE topic = :topic ORDER BY timestamp DESC")
    fun getSessionsForTopic(topic: String): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSession(): Flow<StudySessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM study_sessions")
    suspend fun clearAllSessions()
}
