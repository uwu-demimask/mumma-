package com.example.data.repository

import com.example.core.ai.AiResponse
import com.example.core.ai.FlashcardItem
import com.example.core.ai.HybridAiService
import com.example.core.ai.QuizData
import com.example.core.ai.RevisionSummary
import com.example.core.ai.TeachAnalysisResult
import com.example.core.desktop.DesktopCompanionBridge
import com.example.core.desktop.DesktopCompanionBridgeImpl
import com.example.core.memory.MemoryEngine
import com.example.core.study.StudyEngine
import com.example.core.voice.VoiceEngine
import com.example.data.local.AppDatabase
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.StudySessionEntity
import com.example.data.preferences.MummaPreferences
import kotlinx.coroutines.flow.Flow

class MummaRepository(
    private val database: AppDatabase,
    val preferences: MummaPreferences,
    val voiceEngine: VoiceEngine,
    val aiService: HybridAiService,
    val memoryEngine: MemoryEngine = MemoryEngine(database.memoryDao(), preferences),
    val studyEngine: StudyEngine = StudyEngine(database.studyDao(), aiService, memoryEngine),
    val desktopBridge: DesktopCompanionBridge = DesktopCompanionBridgeImpl()
) {
    val allMessages: Flow<List<MessageEntity>> = database.conversationDao().getAllMessages()
    val activeMemories: Flow<List<MemoryEntity>> = memoryEngine.activeMemories
    val memoryCount: Flow<Int> = memoryEngine.memoryCount
    val latestMessage: Flow<MessageEntity?> = database.conversationDao().getLatestMessage()
    val studySessions: Flow<List<StudySessionEntity>> = studyEngine.sessionHistory

    suspend fun sendMessage(userText: String, isStudyMode: Boolean = false, studyTopic: String? = null): AiResponse {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return AiResponse("")

        // 1. Save user message
        database.conversationDao().insertMessage(
            MessageEntity(
                role = "user",
                content = trimmed,
                isStudyMessage = isStudyMode,
                studyTopic = studyTopic
            )
        )

        // 2. Fetch history and memories
        val history = database.conversationDao().getRecentMessages(10).reversed()
        val memories = memoryEngine.getActiveMemoriesList()

        // 3. Generate response via AI Service
        val response = aiService.generateCompanionResponse(
            userMessage = trimmed,
            conversationHistory = history,
            activeMemories = memories,
            isStudyMode = isStudyMode,
            studyTopic = studyTopic
        )

        // 4. Save Mumma response
        database.conversationDao().insertMessage(
            MessageEntity(
                role = "mumma",
                content = response.text,
                isStudyMessage = isStudyMode,
                studyTopic = studyTopic
            )
        )

        // 5. Save any extracted memories
        if (response.extractedMemories.isNotEmpty()) {
            memoryEngine.storeMemories(response.extractedMemories, source = "conversation")
        }

        // 6. Speak response via VoiceEngine if appropriate
        voiceEngine.speak(response.text)

        return response
    }

    suspend fun explainTopic(topic: String): com.example.core.ai.TopicExplanation {
        val memories = memoryEngine.getActiveMemoriesList()
        val explanation = aiService.explainTopic(topic, "", memories)
        return explanation
    }

    suspend fun generateContextualGreeting(userName: String = "", recentTopic: String = "Photosynthesis"): String {
        val memories = memoryEngine.getActiveMemoriesList()
        return aiService.generateContextualGreeting(userName, "", memories, recentTopic)
    }

    suspend fun analyzeTeachExplanation(topic: String, explanation: String): TeachAnalysisResult {
        val result = studyEngine.analyzeAndSaveTeachExplanation(topic, explanation)
        if (result.summaryMessage.isNotBlank()) {
            voiceEngine.speak(result.summaryMessage)
        }
        return result
    }

    suspend fun getQuiz(topic: String): QuizData = studyEngine.getQuiz(topic)

    suspend fun getFlashcards(topic: String): List<FlashcardItem> = studyEngine.getFlashcards(topic)

    suspend fun getQuickRevision(topic: String): RevisionSummary = studyEngine.getQuickRevision(topic)

    suspend fun addMemory(category: String, content: String) {
        memoryEngine.addDirectMemory(category, content)
    }

    suspend fun deleteMemory(id: Long) {
        memoryEngine.deleteMemory(id)
    }

    suspend fun clearMemories() {
        memoryEngine.clearAll()
    }

    suspend fun clearChatHistory() {
        database.conversationDao().clearAllMessages()
    }
}
