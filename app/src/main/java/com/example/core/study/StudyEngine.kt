package com.example.core.study

import com.example.core.ai.AiProvider
import com.example.core.ai.ExtractedMemoryCandidate
import com.example.core.ai.FlashcardItem
import com.example.core.ai.QuizData
import com.example.core.ai.RevisionSummary
import com.example.core.ai.TeachAnalysisResult
import com.example.core.memory.MemoryEngine
import com.example.data.local.dao.StudyDao
import com.example.data.local.entities.StudySessionEntity
import kotlinx.coroutines.flow.Flow

class StudyEngine(
    private val studyDao: StudyDao,
    private val aiProvider: AiProvider,
    private val memoryEngine: MemoryEngine
) {
    val sessionHistory: Flow<List<StudySessionEntity>> = studyDao.getAllSessions()
    val latestSession: Flow<StudySessionEntity?> = studyDao.getLatestSession()

    suspend fun analyzeAndSaveTeachExplanation(
        topic: String,
        userExplanation: String
    ): TeachAnalysisResult {
        val memories = memoryEngine.getActiveMemoriesList()
        val result = aiProvider.analyzeTeachExplanation(topic, userExplanation, memories)

        // Save study session to Room
        studyDao.insertSession(
            StudySessionEntity(
                topic = topic,
                mode = "TEACH",
                userExplanation = userExplanation,
                understandingSummary = result.understanding,
                missingConcepts = result.missing.joinToString("\n"),
                corrections = result.corrections.joinToString("\n"),
                clarityRating = result.clarity,
                confidenceScore = result.confidenceScore,
                followUpQuestions = result.followUpQuestions.joinToString("\n")
            )
        )

        // Automatically update memories with struggles or masteries learned during the session
        val memoryCandidates = mutableListOf<ExtractedMemoryCandidate>()
        if (result.confidenceScore >= 80) {
            memoryCandidates.add(
                ExtractedMemoryCandidate("MASTERY", "Understands $topic well (confidence ${result.confidenceScore}%)")
            )
        } else if (result.confidenceScore < 60 && result.missing.isNotEmpty()) {
            memoryCandidates.add(
                ExtractedMemoryCandidate("STRUGGLE", "Needs revision on $topic (${result.missing.take(2).joinToString(", ")})")
            )
        }
        if (result.corrections.isNotEmpty() && !result.corrections.first().contains("No major factual errors")) {
            memoryCandidates.add(
                ExtractedMemoryCandidate("MISTAKE", "Common pitfall in $topic: ${result.corrections.first()}")
            )
        }

        memoryEngine.storeMemories(memoryCandidates, source = "study")

        return result
    }

    suspend fun getQuiz(topic: String): QuizData {
        val memories = memoryEngine.getActiveMemoriesList()
        return aiProvider.generateQuiz(topic, memories)
    }

    suspend fun getFlashcards(topic: String): List<FlashcardItem> {
        return aiProvider.generateFlashcards(topic)
    }

    suspend fun getQuickRevision(topic: String): RevisionSummary {
        val memories = memoryEngine.getActiveMemoriesList()
        val struggles = memories.filter { it.category == "STRUGGLE" }.map { it.content }
        return aiProvider.generateQuickRevision(topic, struggles)
    }
}
