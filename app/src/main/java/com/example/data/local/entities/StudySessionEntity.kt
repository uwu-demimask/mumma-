package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,
    val mode: String, // "TEACH", "QUIZ", "EXPLAIN", "FLASHCARD", "REVISION"
    val userExplanation: String,
    val understandingSummary: String,
    val missingConcepts: String, // newline or comma-separated
    val corrections: String,
    val clarityRating: String, // "Excellent", "Good", "Needs Structure", etc.
    val confidenceScore: Int, // 0 - 100
    val followUpQuestions: String, // newline or JSON
    val timestamp: Long = System.currentTimeMillis()
)
