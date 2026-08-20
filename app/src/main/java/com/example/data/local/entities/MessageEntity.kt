package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String = "default_session",
    val role: String, // "user" or "mumma" or "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStudyMessage: Boolean = false,
    val studyTopic: String? = null
)
