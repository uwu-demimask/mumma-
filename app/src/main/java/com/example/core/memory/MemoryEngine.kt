package com.example.core.memory

import com.example.core.ai.ExtractedMemoryCandidate
import com.example.data.local.dao.MemoryDao
import com.example.data.local.entities.MemoryEntity
import com.example.data.preferences.MummaPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MemoryEngine(
    private val memoryDao: MemoryDao,
    private val preferences: MummaPreferences
) {
    val activeMemories: Flow<List<MemoryEntity>> = memoryDao.getActiveMemories()
    val memoryCount: Flow<Int> = memoryDao.getActiveMemoryCount()

    suspend fun getActiveMemoriesList(): List<MemoryEntity> {
        val enabled = preferences.isMemoryEnabled.first()
        if (!enabled) return emptyList()
        return memoryDao.getActiveMemoriesList()
    }

    suspend fun storeMemories(candidates: List<ExtractedMemoryCandidate>, source: String = "conversation") {
        val enabled = preferences.isMemoryEnabled.first()
        if (!enabled || candidates.isEmpty()) return

        val existing = memoryDao.getActiveMemoriesList()
        val toInsert = mutableListOf<MemoryEntity>()

        for (candidate in candidates) {
            val contentTrimmed = candidate.content.trim()
            if (contentTrimmed.length < 3) continue

            // Deduplication check
            val isDuplicate = existing.any {
                it.content.equals(contentTrimmed, ignoreCase = true) ||
                (it.category == candidate.category && it.content.contains(contentTrimmed, ignoreCase = true))
            }

            if (!isDuplicate) {
                toInsert.add(
                    MemoryEntity(
                        category = candidate.category,
                        content = contentTrimmed,
                        source = source
                    )
                )
            }
        }

        if (toInsert.isNotEmpty()) {
            memoryDao.insertMemories(toInsert)
        }
    }

    suspend fun addDirectMemory(category: String, content: String) {
        val enabled = preferences.isMemoryEnabled.first()
        if (!enabled) return
        memoryDao.insertMemory(
            MemoryEntity(
                category = category,
                content = content.trim(),
                source = "user_direct"
            )
        )
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun clearAll() {
        memoryDao.clearAllMemories()
    }

    suspend fun toggleMemoryEnabled(enabled: Boolean) {
        preferences.setMemoryEnabled(enabled)
    }
}
