package com.example.core.ai

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TeachAnalysisResult(
    val understanding: String,
    val missing: List<String> = emptyList(),
    val corrections: List<String> = emptyList(),
    val clarity: String,
    val confidenceScore: Int, // 0 to 100
    val followUpQuestions: List<String> = emptyList(),
    val summaryMessage: String
)

@JsonClass(generateAdapter = true)
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

@JsonClass(generateAdapter = true)
data class QuizData(
    val topic: String,
    val questions: List<QuizQuestion>
)

@JsonClass(generateAdapter = true)
data class FlashcardItem(
    val keyTerm: String,
    val front: String,
    val back: String
)

@JsonClass(generateAdapter = true)
data class RevisionSummary(
    val topic: String,
    val coreDefinition: String,
    val keyPoints: List<String>,
    val commonPitfalls: List<String>,
    val mummaStudyTip: String
)

@JsonClass(generateAdapter = true)
data class FlowchartNode(
    val id: String,
    val stepNumber: Int,
    val title: String,
    val description: String,
    val tag: String = "Process", // e.g. "Input", "Step", "Conversion", "Output", "Decision"
    val iconName: String = "bolt"
)

@JsonClass(generateAdapter = true)
data class FlowchartData(
    val topic: String,
    val title: String,
    val nodes: List<FlowchartNode>,
    val summary: String
)

@JsonClass(generateAdapter = true)
data class Visual3DElement(
    val name: String,
    val role: String,
    val radius: Float = 20f,
    val orbitRadius: Float = 80f,
    val orbitSpeed: Float = 1.0f,
    val colorHex: String = "#FFB74D",
    val zOffset: Float = 0f
)

@JsonClass(generateAdapter = true)
data class Visual3DModel(
    val type: String, // "ATOM_ORBIT", "SOLAR_SYSTEM", "DNA_HELIX", "CELL_ANATOMY", "NEURAL_NETWORK", "PHYSICS_VECTORS", "MOLECULE"
    val topic: String,
    val title: String,
    val description: String,
    val elements: List<Visual3DElement> = emptyList(),
    val particleCount: Int = 24
)

@JsonClass(generateAdapter = true)
data class DiagramLabel(
    val label: String,
    val description: String,
    val xPercent: Float = 0.5f,
    val yPercent: Float = 0.5f
)

@JsonClass(generateAdapter = true)
data class VisualDiagramData(
    val topic: String,
    val title: String,
    val description: String,
    val labels: List<DiagramLabel> = emptyList(),
    val promptForGeneration: String = "",
    val visualType: String = "diagram" // "photosynthesis", "cell", "atom", "circulatory", "generic"
)

@JsonClass(generateAdapter = true)
data class ProactiveFollowUp(
    val text: String,
    val actionType: String, // "EXPLAIN_FLOWCHART", "OPEN_3D", "GENERATE_DIAGRAM", "QUIZ", "PRACTICE", "SPEAK"
    val payload: String
)

@JsonClass(generateAdapter = true)
data class TopicExplanation(
    val topic: String,
    val title: String,
    val textExplanation: String,
    val hinglishSummary: String? = null,
    val hindiSummary: String? = null,
    val flowchart: FlowchartData? = null,
    val visual3D: Visual3DModel? = null,
    val diagram: VisualDiagramData? = null,
    val proactiveFollowUps: List<ProactiveFollowUp> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ExtractedMemoryCandidate(
    val category: String, // "PREFERENCE", "INSTRUCTION", "STRUGGLE", "MASTERY", "MISTAKE", "GOAL", "CONTEXT"
    val content: String
)

data class AiResponse(
    val text: String,
    val extractedMemories: List<ExtractedMemoryCandidate> = emptyList(),
    val isDirectMemoryCommand: Boolean = false,
    val memoryActionDescription: String? = null,
    val proactiveFollowUps: List<ProactiveFollowUp> = emptyList()
)

