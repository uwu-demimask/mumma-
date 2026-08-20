package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.FlashcardItem
import com.example.core.ai.HybridAiService
import com.example.core.ai.QuizData
import com.example.core.ai.RevisionSummary
import com.example.core.ai.TeachAnalysisResult
import com.example.core.voice.VoiceEngine
import com.example.data.local.AppDatabase
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.StudySessionEntity
import com.example.data.preferences.MummaPreferences
import com.example.data.repository.MummaRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TeachUiState {
    object Idle : TeachUiState()
    data class RecordingExplanation(val partialText: String) : TeachUiState()
    object Analyzing : TeachUiState()
    data class AnalysisReady(val result: TeachAnalysisResult) : TeachUiState()
}

sealed class QuizUiState {
    object Idle : QuizUiState()
    object Loading : QuizUiState()
    data class InProgress(
        val data: QuizData,
        val currentIndex: Int = 0,
        val selectedOption: Int? = null,
        val isAnswered: Boolean = false,
        val score: Int = 0
    ) : QuizUiState()
    data class Completed(val score: Int, val total: Int, val topic: String) : QuizUiState()
}

sealed class FlashcardsUiState {
    object Idle : FlashcardsUiState()
    object Loading : FlashcardsUiState()
    data class Active(
        val cards: List<FlashcardItem>,
        val currentIndex: Int = 0,
        val isFlipped: Boolean = false
    ) : FlashcardsUiState()
}

sealed class RevisionUiState {
    object Idle : RevisionUiState()
    object Loading : RevisionUiState()
    data class Ready(val summary: RevisionSummary) : RevisionUiState()
}

sealed class TopicExplanationUiState {
    object Idle : TopicExplanationUiState()
    object Loading : TopicExplanationUiState()
    data class Success(val explanation: com.example.core.ai.TopicExplanation) : TopicExplanationUiState()
    data class Error(val message: String) : TopicExplanationUiState()
}

class MummaViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = MummaPreferences(application)
    private val database = AppDatabase.getInstance(application)
    val voiceEngine = VoiceEngine(application, preferences, viewModelScope)
    private val aiService = HybridAiService(preferences)

    val repository = MummaRepository(
        database = database,
        preferences = preferences,
        voiceEngine = voiceEngine,
        aiService = aiService
    )

    // Data streams from Room
    val messages: StateFlow<List<MessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = repository.activeMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memoryCount: StateFlow<Int> = repository.memoryCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val latestMessage: StateFlow<MessageEntity?> = repository.latestMessage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val studySessions: StateFlow<List<StudySessionEntity>> = repository.studySessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice states
    val isSpeaking: StateFlow<Boolean> = voiceEngine.isSpeaking
    val isListening: StateFlow<Boolean> = voiceEngine.isListening
    val audioRmsDb: StateFlow<Float> = voiceEngine.audioRmsDb
    val recognizedText: StateFlow<String> = voiceEngine.recognizedText

    // Settings & Voice Personality
    val isMemoryEnabled: StateFlow<Boolean> = preferences.isMemoryEnabled
    val isTtsAutoPlay: StateFlow<Boolean> = preferences.isTtsAutoPlay
    val speechRate: StateFlow<Float> = preferences.speechRate
    val speechPitch: StateFlow<Float> = preferences.speechPitch
    val voicePreset: StateFlow<String> = preferences.voicePreset
    val isForceOfflineAi: StateFlow<Boolean> = preferences.isForceOfflineAi
    val language: StateFlow<String> = preferences.language
    val pitchVariation: StateFlow<Boolean> = preferences.pitchVariation
    val breatheTiming: StateFlow<Boolean> = preferences.breatheTiming
    val userName: StateFlow<String> = preferences.userName

    // Processing & Input states
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _currentInputText = MutableStateFlow("")
    val currentInputText: StateFlow<String> = _currentInputText.asStateFlow()

    private val _topicExplanationState = MutableStateFlow<TopicExplanationUiState>(TopicExplanationUiState.Idle)
    val topicExplanationState: StateFlow<TopicExplanationUiState> = _topicExplanationState.asStateFlow()

    private val _greetingMessage = MutableStateFlow<String>("")
    val greetingMessage: StateFlow<String> = _greetingMessage.asStateFlow()

    init {
        // Stream recognized voice text to current input in real-time
        viewModelScope.launch {
            voiceEngine.recognizedText.collect { partial ->
                if (isListening.value && partial.isNotBlank()) {
                    if (_teachState.value is TeachUiState.RecordingExplanation) {
                        _teachState.value = TeachUiState.RecordingExplanation(partial)
                    } else {
                        _currentInputText.value = partial
                    }
                }
            }
        }

        // Initialize contextual greeting on launch
        viewModelScope.launch {
            loadContextualGreeting()
        }
    }

    fun loadContextualGreeting() {
        viewModelScope.launch {
            try {
                val greeting = repository.generateContextualGreeting(
                    userName = userName.value,
                    recentTopic = _selectedStudyTopic.value
                )
                _greetingMessage.value = greeting
            } catch (e: Exception) {
                _greetingMessage.value = "Namaste beta! I'm here for your study session."
            }
        }
    }

    fun explainTopic(topic: String = _selectedStudyTopic.value) {
        _topicExplanationState.value = TopicExplanationUiState.Loading
        viewModelScope.launch {
            try {
                val result = repository.explainTopic(topic)
                _topicExplanationState.value = TopicExplanationUiState.Success(result)
                val spokenText = if (language.value == "HINDI" && !result.hindiSummary.isNullOrBlank()) {
                    result.hindiSummary
                } else if (language.value == "HINGLISH" && !result.hinglishSummary.isNullOrBlank()) {
                    result.hinglishSummary
                } else {
                    result.textExplanation.take(200)
                }
                voiceEngine.speak(spokenText)
            } catch (e: Exception) {
                _topicExplanationState.value = TopicExplanationUiState.Error(e.message ?: "Failed to load explanation")
                _toastEvent.emit("Could not load explanation: ${e.message}")
            }
        }
    }

    fun resetTopicExplanation() {
        _topicExplanationState.value = TopicExplanationUiState.Idle
    }

    // Study Feature States
    private val _selectedStudyTopic = MutableStateFlow("Photosynthesis")
    val selectedStudyTopic: StateFlow<String> = _selectedStudyTopic.asStateFlow()

    private val _teachState = MutableStateFlow<TeachUiState>(TeachUiState.Idle)
    val teachState: StateFlow<TeachUiState> = _teachState.asStateFlow()

    private val _quizState = MutableStateFlow<QuizUiState>(QuizUiState.Idle)
    val quizState: StateFlow<QuizUiState> = _quizState.asStateFlow()

    private val _flashcardState = MutableStateFlow<FlashcardsUiState>(FlashcardsUiState.Idle)
    val flashcardState: StateFlow<FlashcardsUiState> = _flashcardState.asStateFlow()

    private val _revisionState = MutableStateFlow<RevisionUiState>(RevisionUiState.Idle)
    val revisionState: StateFlow<RevisionUiState> = _revisionState.asStateFlow()

    // Transient UI toast notifications
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun onInputTextChanged(text: String) {
        _currentInputText.value = text
    }

    fun setSelectedStudyTopic(topic: String) {
        _selectedStudyTopic.value = topic
    }

    fun sendMessage(text: String = _currentInputText.value, isStudyMode: Boolean = false, studyTopic: String? = null) {
        val message = text.trim()
        if (message.isBlank()) return

        _currentInputText.value = ""
        _isThinking.value = true

        viewModelScope.launch {
            try {
                val response = repository.sendMessage(message, isStudyMode, studyTopic)
                if (response.isDirectMemoryCommand && response.memoryActionDescription != null) {
                    _toastEvent.emit(response.memoryActionDescription)
                }
            } catch (e: Exception) {
                _toastEvent.emit("Error sending message: ${e.message}")
            } finally {
                _isThinking.value = false
            }
        }
    }

    fun startVoiceInput(onCompletedText: ((String) -> Unit)? = null) {
        voiceEngine.startListening(
            onResult = { text ->
                _currentInputText.value = text
                onCompletedText?.invoke(text)
            },
            onError = { error ->
                viewModelScope.launch { _toastEvent.emit(error) }
            }
        )
    }

    fun stopVoiceInput() {
        voiceEngine.stopListening()
    }

    fun stopSpeaking() {
        voiceEngine.stopSpeaking()
    }

    fun speakText(text: String) {
        voiceEngine.speak(text)
    }

    // --- Teach Mumma Flows ---
    fun startTeachRecording() {
        _teachState.value = TeachUiState.RecordingExplanation("")
        voiceEngine.startListening(
            onResult = { explanation ->
                _teachState.value = TeachUiState.RecordingExplanation(explanation)
            },
            onError = { error ->
                viewModelScope.launch { _toastEvent.emit(error) }
            }
        )
    }

    fun stopTeachRecordingAndAnalyze(customText: String? = null) {
        voiceEngine.stopListening()
        val currentExplanation = customText ?: when (val state = _teachState.value) {
            is TeachUiState.RecordingExplanation -> state.partialText
            else -> ""
        }

        if (currentExplanation.isBlank()) {
            _teachState.value = TeachUiState.Idle
            viewModelScope.launch { _toastEvent.emit("Explanation was empty. Try again.") }
            return
        }

        _teachState.value = TeachUiState.Analyzing
        viewModelScope.launch {
            try {
                val result = repository.analyzeTeachExplanation(_selectedStudyTopic.value, currentExplanation)
                _teachState.value = TeachUiState.AnalysisReady(result)
            } catch (e: Exception) {
                _teachState.value = TeachUiState.Idle
                _toastEvent.emit("Analysis failed: ${e.message}")
            }
        }
    }

    fun resetTeachMode() {
        _teachState.value = TeachUiState.Idle
    }

    // --- Quiz Mode Flows ---
    fun startQuiz(topic: String = _selectedStudyTopic.value) {
        _quizState.value = QuizUiState.Loading
        viewModelScope.launch {
            try {
                val quizData = repository.getQuiz(topic)
                _quizState.value = QuizUiState.InProgress(data = quizData)
            } catch (e: Exception) {
                _quizState.value = QuizUiState.Idle
                _toastEvent.emit("Could not load quiz: ${e.message}")
            }
        }
    }

    fun selectQuizOption(index: Int) {
        val current = _quizState.value as? QuizUiState.InProgress ?: return
        if (current.isAnswered) return

        val isCorrect = index == current.data.questions[current.currentIndex].correctIndex
        val newScore = if (isCorrect) current.score + 1 else current.score

        _quizState.value = current.copy(
            selectedOption = index,
            isAnswered = true,
            score = newScore
        )
    }

    fun nextQuizQuestion() {
        val current = _quizState.value as? QuizUiState.InProgress ?: return
        if (current.currentIndex + 1 < current.data.questions.size) {
            _quizState.value = current.copy(
                currentIndex = current.currentIndex + 1,
                selectedOption = null,
                isAnswered = false
            )
        } else {
            _quizState.value = QuizUiState.Completed(
                score = current.score,
                total = current.data.questions.size,
                topic = current.data.topic
            )
        }
    }

    fun resetQuiz() {
        _quizState.value = QuizUiState.Idle
    }

    // --- Flashcards Flows ---
    fun loadFlashcards(topic: String = _selectedStudyTopic.value) {
        _flashcardState.value = FlashcardsUiState.Loading
        viewModelScope.launch {
            try {
                val cards = repository.getFlashcards(topic)
                _flashcardState.value = FlashcardsUiState.Active(cards = cards)
            } catch (e: Exception) {
                _flashcardState.value = FlashcardsUiState.Idle
                _toastEvent.emit("Could not load flashcards: ${e.message}")
            }
        }
    }

    fun flipCurrentFlashcard() {
        val current = _flashcardState.value as? FlashcardsUiState.Active ?: return
        _flashcardState.value = current.copy(isFlipped = !current.isFlipped)
    }

    fun nextFlashcard() {
        val current = _flashcardState.value as? FlashcardsUiState.Active ?: return
        if (current.currentIndex + 1 < current.cards.size) {
            _flashcardState.value = current.copy(
                currentIndex = current.currentIndex + 1,
                isFlipped = false
            )
        }
    }

    fun previousFlashcard() {
        val current = _flashcardState.value as? FlashcardsUiState.Active ?: return
        if (current.currentIndex > 0) {
            _flashcardState.value = current.copy(
                currentIndex = current.currentIndex - 1,
                isFlipped = false
            )
        }
    }

    fun resetFlashcards() {
        _flashcardState.value = FlashcardsUiState.Idle
    }

    // --- Revision Flows ---
    fun loadQuickRevision(topic: String = _selectedStudyTopic.value) {
        _revisionState.value = RevisionUiState.Loading
        viewModelScope.launch {
            try {
                val summary = repository.getQuickRevision(topic)
                _revisionState.value = RevisionUiState.Ready(summary)
            } catch (e: Exception) {
                _revisionState.value = RevisionUiState.Idle
                _toastEvent.emit("Could not load revision: ${e.message}")
            }
        }
    }

    fun resetRevision() {
        _revisionState.value = RevisionUiState.Idle
    }

    // --- Memory Operations ---
    fun addMemory(category: String, content: String) {
        viewModelScope.launch {
            repository.addMemory(category, content)
            _toastEvent.emit("Memory saved")
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
            _toastEvent.emit("Memory deleted")
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearMemories()
            _toastEvent.emit("All memories cleared")
        }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        preferences.setMemoryEnabled(enabled)
    }

    fun clearConversationHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
            _toastEvent.emit("Chat history cleared")
        }
    }

    // --- Preferences Operations ---
    fun setTtsAutoPlay(autoPlay: Boolean) = preferences.setTtsAutoPlay(autoPlay)
    fun setSpeechRate(rate: Float) = preferences.setSpeechRate(rate)
    fun setSpeechPitch(pitch: Float) = preferences.setSpeechPitch(pitch)
    fun setVoicePreset(preset: String) = preferences.setVoicePreset(preset)
    fun previewVoice(preset: String = voicePreset.value) = voiceEngine.previewVoice(preset)
    fun setForceOfflineAi(offline: Boolean) = preferences.setForceOfflineAi(offline)
    fun setLanguage(lang: String) = preferences.setLanguage(lang)
    fun setPitchVariation(enabled: Boolean) = preferences.setPitchVariation(enabled)
    fun setBreatheTiming(enabled: Boolean) = preferences.setBreatheTiming(enabled)
    fun setUserName(name: String) = preferences.setUserName(name)

    override fun onCleared() {
        super.onCleared()
        voiceEngine.release()
    }
}
