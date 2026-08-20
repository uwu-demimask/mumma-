package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "PREFERENCE", "INSTRUCTION", "STRUGGLE", "MASTERY", "MISTAKE", "GOAL", "CONTEXT"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "conversation", // "conversation", "study", "user_direct"
    val isActive: Boolean = true
)
