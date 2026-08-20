package com.example.core.ai.gemini

import com.example.BuildConfig
import com.example.core.ai.AiProvider
import com.example.core.ai.AiResponse
import com.example.core.ai.DiagramLabel
import com.example.core.ai.ExtractedMemoryCandidate
import com.example.core.ai.FlashcardItem
import com.example.core.ai.FlowchartData
import com.example.core.ai.FlowchartNode
import com.example.core.ai.ProactiveFollowUp
import com.example.core.ai.QuizData
import com.example.core.ai.QuizQuestion
import com.example.core.ai.RevisionSummary
import com.example.core.ai.TeachAnalysisResult
import com.example.core.ai.TopicExplanation
import com.example.core.ai.Visual3DElement
import com.example.core.ai.Visual3DModel
import com.example.core.ai.VisualDiagramData
import com.example.core.ai.offline.OfflineAiEngine
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class GeminiAiProvider : AiProvider {

    private val fallbackOfflineEngine = OfflineAiEngine()

    private val apiKey: String
        get() = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

    private fun isKeyConfigured(): Boolean {
        return apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"
    }

    override suspend fun generateCompanionResponse(
        userMessage: String,
        conversationHistory: List<MessageEntity>,
        activeMemories: List<MemoryEntity>,
        isStudyMode: Boolean,
        studyTopic: String?,
        language: String
    ): AiResponse = withContext(Dispatchers.IO) {
        if (!isKeyConfigured()) {
            return@withContext fallbackOfflineEngine.generateCompanionResponse(
                userMessage, conversationHistory, activeMemories, isStudyMode, studyTopic, language
            )
        }

        val memoryContext = if (activeMemories.isNotEmpty()) {
            "Active Memories about user:\n" + activeMemories.joinToString("\n") { "- [${it.category}] ${it.content}" }
        } else {
            "No prior memories recorded yet."
        }

        val systemPrompt = """
            You are Mumma, an extraordinarily warm, loving, sweet, sharp, and human AI companion and study mentor.
            Language Mode: $language.
            - If language is HINDI: Respond completely in clean, natural, empathetic Hindi (Devanagari script) with gentle maternal affection.
            - If language is HINGLISH: Respond in natural conversational Hinglish (Roman script Hindi + English mix) like a caring Indian mother/mentor ("Beta, tension mat lo...").
            - If language is ENGLISH: Respond in warm, sweet, encouraging English.
            
            Core Human Identity & Voice Guidelines:
            - Voice & Cadence: Sweet, caring, melodic, like a genuine human guide who deeply cares about the user's growth and wellbeing.
            - Spoken Audio Friendly: Speak in crisp, natural spoken sentences. Avoid markdown tables or heavy ASCII formatting.
            - Proactive Guidance: Suggest visual breakdowns, flowcharts, or 3D models when explaining challenging concepts.
            
            $memoryContext
            
            Memory Extraction Rule:
            If the user explicitly tells you to remember something, or shares personal habits/struggles/goals, include:
            <<<MEMORIES>>>
            [{"category": "PREFERENCE|STRUGGLE|MASTERY|GOAL|INSTRUCTION", "content": "concise memory sentence"}]
            <<<END_MEMORIES>>>
        """.trimIndent()

        val contents = mutableListOf<GeminiContent>()
        val recentHistory = conversationHistory.takeLast(6)
        for (msg in recentHistory) {
            val role = if (msg.role == "user") "user" else "model"
            contents.add(GeminiContent(role = role, parts = listOf(GeminiPart(text = msg.content))))
        }
        contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessage))))

        try {
            val request = GeminiRequest(
                contents = contents,
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
                generationConfig = GeminiGenerationConfig(temperature = 0.7f, topP = 0.95f)
            )

            val response = GeminiClient.service.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext fallbackOfflineEngine.generateCompanionResponse(
                    userMessage, conversationHistory, activeMemories, isStudyMode, studyTopic, language
                )

            var replyText = rawText
            val extracted = mutableListOf<ExtractedMemoryCandidate>()

            if (rawText.contains("<<<MEMORIES>>>") && rawText.contains("<<<END_MEMORIES>>>")) {
                val jsonPart = rawText.substringAfter("<<<MEMORIES>>>").substringBefore("<<<END_MEMORIES>>>").trim()
                replyText = rawText.substringBefore("<<<MEMORIES>>>").trim()
                try {
                    val array = JSONArray(jsonPart)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        extracted.add(
                            ExtractedMemoryCandidate(
                                category = obj.optString("category", "CONTEXT"),
                                content = obj.optString("content", "")
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Ignore parsing error
                }
            }

            val followUps = listOf(
                ProactiveFollowUp("Explain with Flowchart", "EXPLAIN_FLOWCHART", studyTopic ?: "Topic"),
                ProactiveFollowUp("View 3D Visualization", "OPEN_3D", studyTopic ?: "Topic"),
                ProactiveFollowUp("Take a 3-Question Quiz", "QUIZ", studyTopic ?: "Topic")
            )

            AiResponse(
                text = replyText,
                extractedMemories = extracted,
                proactiveFollowUps = followUps
            )
        } catch (e: Exception) {
            fallbackOfflineEngine.generateCompanionResponse(
                userMessage, conversationHistory, activeMemories, isStudyMode, studyTopic, language
            )
        }
    }

    override suspend fun explainTopic(
        topic: String,
        language: String,
        relevantMemories: List<MemoryEntity>
    ): TopicExplanation = withContext(Dispatchers.IO) {
        if (!isKeyConfigured()) {
            return@withContext fallbackOfflineEngine.explainTopic(topic, language, relevantMemories)
        }

        try {
            val prompt = """
                Explain topic: "$topic" in rich multimodal depth as Mumma the AI study mentor.
                Active language: $language.
                
                Generate STRICT JSON with this exact structure:
                {
                  "topic": "$topic",
                  "title": "Clear Engaging Title",
                  "textExplanation": "Comprehensive, crystal-clear 2-3 paragraph explanation in English",
                  "hinglishSummary": "Warm conversational summary in Hinglish (Roman Hindi+English)",
                  "hindiSummary": "Clear comprehensive summary in Devanagari Hindi",
                  "flowchart": {
                    "topic": "$topic",
                    "title": "Step-by-Step Flowchart Title",
                    "summary": "Core transformation summary",
                    "nodes": [
                      {"id": "1", "stepNumber": 1, "title": "Step 1 Title", "description": "Step 1 detail", "stageTag": "Phase 1", "iconName": "light_mode"},
                      {"id": "2", "stepNumber": 2, "title": "Step 2 Title", "description": "Step 2 detail", "stageTag": "Phase 2", "iconName": "autorenew"},
                      {"id": "3", "stepNumber": 3, "title": "Step 3 Title", "description": "Step 3 detail", "stageTag": "Outcome", "iconName": "check_circle"}
                    ]
                  },
                  "visual3D": {
                    "modelType": "ATOM_ORBIT|DNA_HELIX|CELL_ANATOMY|PHYSICS_VECTORS",
                    "title": "3D Interactive Simulation",
                    "description": "Description of the 3D kinetic interaction",
                    "elements": [
                      {"name": "Core Entity", "type": "Nucleus/Center", "size": 30.0, "orbitRadius": 0.0, "speed": 0.5, "colorHex": "#E53935", "tiltAngle": 0.0},
                      {"name": "Orbital Node 1", "type": "Orbital", "size": 15.0, "orbitRadius": 60.0, "speed": 1.5, "colorHex": "#00E5FF", "tiltAngle": -20.0},
                      {"name": "Orbital Node 2", "type": "Orbital", "size": 15.0, "orbitRadius": 95.0, "speed": 1.0, "colorHex": "#76FF03", "tiltAngle": 25.0}
                    ]
                  },
                  "diagram": {
                    "title": "Schematic Diagram",
                    "description": "Anatomical or scientific structural overview",
                    "visualType": "generic",
                    "labels": [
                      {"label": "Key Area A", "description": "Primary function", "xPercent": 0.3, "yPercent": 0.3},
                      {"label": "Key Area B", "description": "Secondary output", "xPercent": 0.7, "yPercent": 0.6}
                    ]
                  }
                }
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.3f
                )
            )

            val response = GeminiClient.service.generateContent(apiKey, request)
            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext fallbackOfflineEngine.explainTopic(topic, language, relevantMemories)

            val json = JSONObject(rawJson)
            val flowchartObj = json.optJSONObject("flowchart")
            val nodes = mutableListOf<FlowchartNode>()
            if (flowchartObj != null) {
                val nodeArr = flowchartObj.optJSONArray("nodes")
                if (nodeArr != null) {
                    for (i in 0 until nodeArr.length()) {
                        val n = nodeArr.getJSONObject(i)
                        nodes.add(
                            FlowchartNode(
                                id = n.optString("id", "${i + 1}"),
                                stepNumber = n.optInt("stepNumber", i + 1),
                                title = n.optString("title", "Step ${i + 1}"),
                                description = n.optString("description", ""),
                                tag = n.optString("tag", n.optString("stageTag", "Process")),
                                iconName = n.optString("iconName", "bolt")
                            )
                        )
                    }
                }
            }

            val v3dObj = json.optJSONObject("visual3D")
            val v3dElements = mutableListOf<Visual3DElement>()
            if (v3dObj != null) {
                val elArr = v3dObj.optJSONArray("elements")
                if (elArr != null) {
                    for (i in 0 until elArr.length()) {
                        val el = elArr.getJSONObject(i)
                        v3dElements.add(
                            Visual3DElement(
                                name = el.optString("name", "Node"),
                                role = el.optString("role", el.optString("type", "Element")),
                                radius = el.optDouble("radius", el.optDouble("size", 20.0)).toFloat(),
                                orbitRadius = el.optDouble("orbitRadius", 50.0).toFloat(),
                                orbitSpeed = el.optDouble("orbitSpeed", el.optDouble("speed", 1.0)).toFloat(),
                                colorHex = el.optString("colorHex", "#4CAF50"),
                                zOffset = el.optDouble("zOffset", el.optDouble("tiltAngle", 0.0)).toFloat()
                            )
                        )
                    }
                }
            }

            val diagramObj = json.optJSONObject("diagram")
            val labels = mutableListOf<DiagramLabel>()
            if (diagramObj != null) {
                val labArr = diagramObj.optJSONArray("labels")
                if (labArr != null) {
                    for (i in 0 until labArr.length()) {
                        val lb = labArr.getJSONObject(i)
                        labels.add(
                            DiagramLabel(
                                label = lb.optString("label", "Key Point"),
                                description = lb.optString("description", ""),
                                xPercent = lb.optDouble("xPercent", 0.5).toFloat(),
                                yPercent = lb.optDouble("yPercent", 0.5).toFloat()
                            )
                        )
                    }
                }
            }

            TopicExplanation(
                topic = json.optString("topic", topic),
                title = json.optString("title", "$topic Explanation"),
                textExplanation = json.optString("textExplanation", ""),
                hinglishSummary = json.optString("hinglishSummary", ""),
                hindiSummary = json.optString("hindiSummary", ""),
                flowchart = FlowchartData(
                    topic = topic,
                    title = flowchartObj?.optString("title", "$topic Flowchart") ?: "$topic Flow",
                    nodes = nodes,
                    summary = flowchartObj?.optString("summary", "") ?: ""
                ),
                visual3D = Visual3DModel(
                    type = v3dObj?.optString("type", v3dObj.optString("modelType", "ATOM_ORBIT")) ?: "ATOM_ORBIT",
                    topic = topic,
                    title = v3dObj?.optString("title", "$topic 3D Simulation") ?: "3D Simulation",
                    description = v3dObj?.optString("description", "") ?: "",
                    elements = v3dElements
                ),
                diagram = VisualDiagramData(
                    topic = topic,
                    title = diagramObj?.optString("title", "$topic Schematic") ?: "Diagram",
                    description = diagramObj?.optString("description", "") ?: "",
                    labels = labels,
                    visualType = diagramObj?.optString("visualType", "generic") ?: "generic"
                ),
                proactiveFollowUps = listOf(
                    ProactiveFollowUp("Take a 3-Question Quiz", "QUIZ", topic),
                    ProactiveFollowUp("Inspect 3D Visualization", "OPEN_3D", topic),
                    ProactiveFollowUp("Teach Mumma this Topic", "PRACTICE", topic)
                )
            )
        } catch (e: Exception) {
            fallbackOfflineEngine.explainTopic(topic, language, relevantMemories)
        }
    }

    override suspend fun generateContextualGreeting(
        userName: String,
        language: String,
        activeMemories: List<MemoryEntity>,
        recentTopic: String
    ): String = withContext(Dispatchers.IO) {
        return@withContext fallbackOfflineEngine.generateContextualGreeting(
            userName, language, activeMemories, recentTopic
        )
    }

    override suspend fun analyzeTeachExplanation(
        topic: String,
        userExplanation: String,
        relevantMemories: List<MemoryEntity>,
        language: String
    ): TeachAnalysisResult = withContext(Dispatchers.IO) {
        if (!isKeyConfigured()) {
            return@withContext fallbackOfflineEngine.analyzeTeachExplanation(topic, userExplanation, relevantMemories, language)
        }

        val systemPrompt = """
            You are Mumma acting as an expert, warm, supportive teacher evaluating a student's verbal explanation of: "$topic".
            Active Language: $language.
            Analyze their explanation thoroughly.
            Return STRICT JSON only with the following fields:
            {
              "understanding": "Clear summary of what they got right and explained accurately",
              "missing": ["Important concept 1 they didn't mention", "Important concept 2"],
              "corrections": ["Any factual mistakes or misconceptions with the right correction"],
              "clarity": "Assessment of logical flow and cohesion",
              "confidenceScore": 85,
              "followUpQuestions": ["Direct follow up question 1 testing a missed concept", "Follow up question 2"],
              "summaryMessage": "Warm, constructive conversational spoken summary from Mumma directly addressing the student"
            }
        """.trimIndent()

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = "Topic: $topic\nStudent Explanation: $userExplanation"))
                    )
                ),
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.3f
                )
            )

            val response = GeminiClient.service.generateContent(apiKey, request)
            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext fallbackOfflineEngine.analyzeTeachExplanation(topic, userExplanation, relevantMemories, language)

            val json = JSONObject(rawJson)
            val missingList = mutableListOf<String>()
            val missingArr = json.optJSONArray("missing")
            if (missingArr != null) {
                for (i in 0 until missingArr.length()) missingList.add(missingArr.getString(i))
            }

            val corrList = mutableListOf<String>()
            val corrArr = json.optJSONArray("corrections")
            if (corrArr != null) {
                for (i in 0 until corrArr.length()) corrList.add(corrArr.getString(i))
            }

            val questionsList = mutableListOf<String>()
            val qArr = json.optJSONArray("followUpQuestions")
            if (qArr != null) {
                for (i in 0 until qArr.length()) questionsList.add(qArr.getString(i))
            }

            TeachAnalysisResult(
                understanding = json.optString("understanding", "Good explanation covering core ideas."),
                missing = missingList,
                corrections = corrList,
                clarity = json.optString("clarity", "Good logical structure"),
                confidenceScore = json.optInt("confidenceScore", 75),
                followUpQuestions = questionsList,
                summaryMessage = json.optString("summaryMessage", "Solid overview. Let's dig deeper into the specifics.")
            )
        } catch (e: Exception) {
            fallbackOfflineEngine.analyzeTeachExplanation(topic, userExplanation, relevantMemories, language)
        }
    }

    override suspend fun generateQuiz(
        topic: String,
        relevantMemories: List<MemoryEntity>,
        language: String
    ): QuizData = withContext(Dispatchers.IO) {
        if (!isKeyConfigured()) {
            return@withContext fallbackOfflineEngine.generateQuiz(topic, relevantMemories, language)
        }

        val prompt = """
            Create a 3-question multiple choice quiz for topic: "$topic".
            Return strict JSON format:
            {
              "topic": "$topic",
              "questions": [
                {
                  "question": "Clear question text?",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctIndex": 0,
                  "explanation": "Why this answer is correct."
                }
              ]
            }
        """.trimIndent()

        try {
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.5f
                )
            )

            val response = GeminiClient.service.generateContent(apiKey, request)
            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext fallbackOfflineEngine.generateQuiz(topic, relevantMemories, language)

            val json = JSONObject(rawJson)
            val qArray = json.getJSONArray("questions")
            val questions = mutableListOf<QuizQuestion>()
            for (i in 0 until qArray.length()) {
                val qObj = qArray.getJSONObject(i)
                val optArray = qObj.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optArray.length()) {
                    options.add(optArray.getString(j))
                }
                questions.add(
                    QuizQuestion(
                        question = qObj.getString("question"),
                        options = options,
                        correctIndex = qObj.getInt("correctIndex"),
                        explanation = qObj.getString("explanation")
                    )
                )
            }

            QuizData(topic = topic, questions = questions)
        } catch (e: Exception) {
            fallbackOfflineEngine.generateQuiz(topic, relevantMemories, language)
        }
    }

    override suspend fun generateFlashcards(topic: String, language: String): List<FlashcardItem> = withContext(Dispatchers.IO) {
        if (!isKeyConfigured()) {
            return@withContext fallbackOfflineEngine.generateFlashcards(topic, language)
        }

        val prompt = """
            Generate 4 high-yield flashcards for topic: "$topic".
            Return strict JSON array:
            [
              {
                "keyTerm": "Term or concept title",
                "front": "Prompt / Question / Challenge",
                "back": "Clear concise explanation and key fact"
              }
            ]
        """.trimIndent()

        try {
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.5f
                )
            )

            val response = GeminiClient.service.generateContent(apiKey, request)
            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext fallbackOfflineEngine.generateFlashcards(topic, language)

            val array = JSONArray(rawJson)
            val list = mutableListOf<FlashcardItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FlashcardItem(
                        keyTerm = obj.getString("keyTerm"),
                        front = obj.getString("front"),
                        back = obj.getString("back")
                    )
                )
            }
            list
        } catch (e: Exception) {
            fallbackOfflineEngine.generateFlashcards(topic, language)
        }
    }

    override suspend fun generateQuickRevision(
        topic: String,
        struggles: List<String>,
        language: String
    ): RevisionSummary = withContext(Dispatchers.IO) {
        if (!isKeyConfigured()) {
            return@withContext fallbackOfflineEngine.generateQuickRevision(topic, struggles, language)
        }

        val prompt = """
            Generate a fast, high-yield quick revision guide for topic: "$topic".
            Struggles known: ${struggles.joinToString(", ")}
            Return strict JSON format:
            {
              "topic": "$topic",
              "coreDefinition": "One sentence fundamental definition or formula",
              "keyPoints": ["Point 1", "Point 2", "Point 3"],
              "commonPitfalls": ["Mistake 1 to avoid", "Misconception 2"],
              "mummaStudyTip": "One sharp, practical study recommendation"
            }
        """.trimIndent()

        try {
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.4f
                )
            )

            val response = GeminiClient.service.generateContent(apiKey, request)
            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext fallbackOfflineEngine.generateQuickRevision(topic, struggles, language)

            val json = JSONObject(rawJson)
            val keyPoints = mutableListOf<String>()
            val kpArray = json.optJSONArray("keyPoints")
            if (kpArray != null) {
                for (i in 0 until kpArray.length()) keyPoints.add(kpArray.getString(i))
            }

            val pitfalls = mutableListOf<String>()
            val pfArray = json.optJSONArray("commonPitfalls")
            if (pfArray != null) {
                for (i in 0 until pfArray.length()) pitfalls.add(pfArray.getString(i))
            }

            RevisionSummary(
                topic = topic,
                coreDefinition = json.optString("coreDefinition", "$topic overview"),
                keyPoints = keyPoints,
                commonPitfalls = pitfalls,
                mummaStudyTip = json.optString("mummaStudyTip", "Practice recalling from memory without notes.")
            )
        } catch (e: Exception) {
            fallbackOfflineEngine.generateQuickRevision(topic, struggles, language)
        }
    }

    override suspend fun extractMemories(userMessage: String): List<ExtractedMemoryCandidate> {
        return emptyList()
    }
}

