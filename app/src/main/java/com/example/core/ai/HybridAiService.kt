package com.example.core.ai

import com.example.core.ai.gemini.GeminiAiProvider
import com.example.core.ai.offline.OfflineAiEngine
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.preferences.MummaPreferences
import kotlinx.coroutines.flow.first

class HybridAiService(
    private val preferences: MummaPreferences,
    private val geminiProvider: GeminiAiProvider = GeminiAiProvider(),
    private val offlineEngine: OfflineAiEngine = OfflineAiEngine()
) : AiProvider {

    private suspend fun shouldUseOfflineOnly(): Boolean {
        return preferences.isForceOfflineAi.first()
    }

    private suspend fun getLanguage(): String {
        return preferences.language.first()
    }

    override suspend fun generateCompanionResponse(
        userMessage: String,
        conversationHistory: List<MessageEntity>,
        activeMemories: List<MemoryEntity>,
        isStudyMode: Boolean,
        studyTopic: String?,
        language: String
    ): AiResponse {
        val activeLang = if (language.isNotBlank()) language else getLanguage()
        if (!shouldUseOfflineOnly()) {
            try {
                return geminiProvider.generateCompanionResponse(
                    userMessage = userMessage,
                    conversationHistory = conversationHistory,
                    activeMemories = activeMemories,
                    isStudyMode = isStudyMode,
                    studyTopic = studyTopic,
                    language = activeLang
                )
            } catch (e: Exception) {
                // Seamlessly fall back to offline engine
            }
        }
        return offlineEngine.generateCompanionResponse(
            userMessage = userMessage,
            conversationHistory = conversationHistory,
            activeMemories = activeMemories,
            isStudyMode = isStudyMode,
            studyTopic = studyTopic,
            language = activeLang
        )
    }

    override suspend fun explainTopic(
        topic: String,
        language: String,
        relevantMemories: List<MemoryEntity>
    ): TopicExplanation {
        val activeLang = if (language.isNotBlank()) language else getLanguage()
        if (!shouldUseOfflineOnly()) {
            try {
                return geminiProvider.explainTopic(topic, activeLang, relevantMemories)
            } catch (e: Exception) {
                // Fall back
            }
        }
        return offlineEngine.explainTopic(topic, activeLang, relevantMemories)
    }

    override suspend fun generateContextualGreeting(
        userName: String,
        language: String,
        activeMemories: List<MemoryEntity>,
        recentTopic: String
    ): String {
        val activeLang = if (language.isNotBlank()) language else getLanguage()
        if (!shouldUseOfflineOnly()) {
            try {
                return geminiProvider.generateContextualGreeting(userName, activeLang, activeMemories, recentTopic)
            } catch (e: Exception) {
                // Fall back
            }
        }
        return offlineEngine.generateContextualGreeting(userName, activeLang, activeMemories, recentTopic)
    }

    override suspend fun analyzeTeachExplanation(
        topic: String,
        userExplanation: String,
        relevantMemories: List<MemoryEntity>,
        language: String
    ): TeachAnalysisResult {
        val activeLang = if (language.isNotBlank()) language else getLanguage()
        if (!shouldUseOfflineOnly()) {
            try {
                return geminiProvider.analyzeTeachExplanation(topic, userExplanation, relevantMemories, activeLang)
            } catch (e: Exception) {
                // Fall back
            }
        }
        return offlineEngine.analyzeTeachExplanation(topic, userExplanation, relevantMemories, activeLang)
    }

    override suspend fun generateQuiz(
        topic: String,
        relevantMemories: List<MemoryEntity>,
        language: String
    ): QuizData {
        val activeLang = if (language.isNotBlank()) language else getLanguage()
        if (!shouldUseOfflineOnly()) {
            try {
                return geminiProvider.generateQuiz(topic, relevantMemories, activeLang)
            } catch (e: Exception) {
                // Fall back
            }
        }
        return offlineEngine.generateQuiz(topic, relevantMemories, activeLang)
    }

    override suspend fun generateFlashcards(topic: String, language: String): List<FlashcardItem> {
        val activeLang = if (language.isNotBlank()) language else getLanguage()
        if (!shouldUseOfflineOnly()) {
            try {
                return geminiProvider.generateFlashcards(topic, activeLang)
            } catch (e: Exception) {
                // Fall back
            }
        }
        return offlineEngine.generateFlashcards(topic, activeLang)
    }

    override suspend fun generateQuickRevision(
        topic: String,
        struggles: List<String>,
        language: String
    ): RevisionSummary {
        val activeLang = if (language.isNotBlank()) language else getLanguage()
        if (!shouldUseOfflineOnly()) {
            try {
                return geminiProvider.generateQuickRevision(topic, struggles, activeLang)
            } catch (e: Exception) {
                // Fall back
            }
        }
        return offlineEngine.generateQuickRevision(topic, struggles, activeLang)
    }

    override suspend fun extractMemories(userMessage: String): List<ExtractedMemoryCandidate> {
        return offlineEngine.extractMemories(userMessage)
    }
}

