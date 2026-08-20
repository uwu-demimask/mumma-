package com.example.core.ai

import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.MessageEntity

interface AiProvider {
    suspend fun generateCompanionResponse(
        userMessage: String,
        conversationHistory: List<MessageEntity>,
        activeMemories: List<MemoryEntity>,
        isStudyMode: Boolean = false,
        studyTopic: String? = null,
        language: String = "ENGLISH"
    ): AiResponse

    suspend fun explainTopic(
        topic: String,
        language: String = "ENGLISH",
        relevantMemories: List<MemoryEntity> = emptyList()
    ): TopicExplanation

    suspend fun generateContextualGreeting(
        userName: String,
        language: String = "ENGLISH",
        activeMemories: List<MemoryEntity> = emptyList(),
        recentTopic: String = "Photosynthesis"
    ): String

    suspend fun analyzeTeachExplanation(
        topic: String,
        userExplanation: String,
        relevantMemories: List<MemoryEntity> = emptyList(),
        language: String = "ENGLISH"
    ): TeachAnalysisResult

    suspend fun generateQuiz(
        topic: String,
        relevantMemories: List<MemoryEntity> = emptyList(),
        language: String = "ENGLISH"
    ): QuizData

    suspend fun generateFlashcards(
        topic: String,
        language: String = "ENGLISH"
    ): List<FlashcardItem>

    suspend fun generateQuickRevision(
        topic: String,
        struggles: List<String> = emptyList(),
        language: String = "ENGLISH"
    ): RevisionSummary

    suspend fun extractMemories(
        userMessage: String
    ): List<ExtractedMemoryCandidate>
}
