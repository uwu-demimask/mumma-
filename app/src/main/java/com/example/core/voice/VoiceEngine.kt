package com.example.core.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.preferences.MummaPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceEngine(
    private val context: Context,
    private val preferences: MummaPreferences,
    private val scope: CoroutineScope
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    // TTS State
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var speechCompletionCallback: (() -> Unit)? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // STT State
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioRmsDb = MutableStateFlow(0f)
    val audioRmsDb: StateFlow<Float> = _audioRmsDb.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    // 1. Audio Attributes for crisp voice playback
                    try {
                        val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        engine.setAudioAttributes(audioAttributes)
                    } catch (e: Exception) {
                        Log.w("VoiceEngine", "Audio attributes not supported: ${e.message}")
                    }

                    // 2. Select initial language
                    val locale = Locale("en", "IN")
                    val langResult = engine.setLanguage(locale)
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.setLanguage(Locale.US)
                    }
                    isTtsInitialized = true

                    // 3. Sweet human voice selection: Prioritize natural/neural female voices
                    applyBestVoice(engine, "ENGLISH")

                    engine.setPitch(1.16f) // Sweet Mumma tone default
                    engine.setSpeechRate(0.94f) // Thoughtful, relaxed cadence

                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                            mainHandler.post {
                                speechCompletionCallback?.invoke()
                                speechCompletionCallback = null
                            }
                        }

                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                            mainHandler.post {
                                speechCompletionCallback?.invoke()
                                speechCompletionCallback = null
                            }
                        }
                    })
                }
            }
        }
    }

    private fun applyBestVoice(engine: TextToSpeech, language: String) {
        try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val targetLang = when (language) {
                    "HINDI" -> "hi"
                    "HINGLISH" -> "en"
                    else -> "en"
                }

                val preferredVoice = voices.find { voice ->
                    val name = voice.name.lowercase(Locale.ROOT)
                    voice.locale.language == targetLang &&
                            (name.contains("female") || name.contains("sweet") || name.contains("sfg") || name.contains("tpd") || name.contains("en-in") || name.contains("hi-in"))
                } ?: voices.find { voice ->
                    voice.locale.language == targetLang && voice.quality >= Voice.QUALITY_HIGH
                } ?: voices.find { voice ->
                    voice.locale.language == targetLang
                }

                if (preferredVoice != null) {
                    engine.voice = preferredVoice
                    Log.d("VoiceEngine", "Selected voice for $language: ${preferredVoice.name}")
                }
            }
        } catch (e: Exception) {
            Log.w("VoiceEngine", "Voice selection fallback: ${e.message}")
        }
    }

    /**
     * Speak text with intelligent sweet phonetics, multi-language switching, pitch variation, and breathe timing.
     */
    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        if (text.isBlank()) {
            onFinished?.invoke()
            return
        }

        speechCompletionCallback = onFinished

        scope.launch(Dispatchers.Main) {
            val autoPlay = preferences.isTtsAutoPlay.first()
            if (!autoPlay && onFinished == null) return@launch

            if (!isTtsInitialized || tts == null) {
                onFinished?.invoke()
                return@launch
            }

            val langPref = preferences.language.first()
            val preset = preferences.voicePreset.first()
            val baseRate = preferences.speechRate.first()
            val basePitch = preferences.speechPitch.first()
            val pitchVariation = preferences.pitchVariation.first()
            val breatheTiming = preferences.breatheTiming.first()

            val engine = tts ?: return@launch

            // Detect if text contains pure Devnagari Hindi or if language is HINDI
            val containsDevnagari = text.any { it in '\u0900'..'\u097F' }
            if (langPref == "HINDI" || containsDevnagari) {
                val hiLocale = Locale("hi", "IN")
                val res = engine.setLanguage(hiLocale)
                if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                    applyBestVoice(engine, "HINDI")
                }
            } else if (langPref == "HINGLISH") {
                val enInLocale = Locale("en", "IN")
                val res = engine.setLanguage(enInLocale)
                if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                    applyBestVoice(engine, "HINGLISH")
                }
            } else {
                val enUsLocale = Locale.US
                engine.setLanguage(enUsLocale)
                applyBestVoice(engine, "ENGLISH")
            }

            // Calculate sweet pitch with dynamic pitch variation
            var finalPitch = basePitch
            if (preset == "SWEET_MUMMA") {
                finalPitch = (basePitch * 1.04f).coerceIn(0.8f, 1.4f)
            }
            if (pitchVariation) {
                if (text.trim().endsWith("?")) {
                    finalPitch *= 1.05f // Curious, inquisitive sweet lift
                } else if (text.trim().endsWith("!")) {
                    finalPitch *= 1.03f // Warm encouraging lift
                }
            }

            engine.setSpeechRate(baseRate)
            engine.setPitch(finalPitch)

            val humanSpokenText = normalizeForHumanSpeech(text, breatheTiming, langPref)
            if (humanSpokenText.isBlank()) {
                onFinished?.invoke()
                return@launch
            }

            val utteranceId = "mumma_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            engine.speak(humanSpokenText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }

    /**
     * Preview sample voice with selected settings in English, Hinglish, or Hindi.
     */
    fun previewVoice(
        presetName: String = "SWEET_MUMMA",
        language: String = "ENGLISH",
        onFinished: (() -> Unit)? = null
    ) {
        val sample = when (language) {
            "HINDI" -> when (presetName) {
                "SWEET_MUMMA" -> "नमस्ते बेटा! मैं तुम्हारी मम्मा हूँ। आराम से बैठो, गहरी साँस लो और आज की पढ़ाई साथ में करते हैं।"
                "GENTLE" -> "चिंता मत करो बेटा, धीरे-धीरे सब समझ आ जाएगा। मैं हमेशा तुम्हारे साथ हूँ।"
                "COACH" -> "चलो बेटा! पूरा ध्यान लगाओ। आज का लक्ष्य हासिल करना है!"
                else -> "नमस्ते! मैं मम्मा हूँ, तुम्हारी पढ़ाई और हर कदम की साथी।"
            }
            "HINGLISH" -> when (presetName) {
                "SWEET_MUMMA" -> "Hello beta! Main Mumma hoon. Bilkul relax ho jao, aur batao aaj kya revise karein?"
                "GENTLE" -> "Don't take tension beta. Ek deep breath lo, step by step sab clear ho jayega."
                "COACH" -> "Alright beta, full focus mode on! Let's conquer this topic together."
                else -> "Hey beta! Main aapki personal study companion hoon. I'm always here for you."
            }
            else -> when (presetName) {
                "SWEET_MUMMA" -> "Hello sweetheart! I'm Mumma, your companion. Take a gentle breath, and let's learn together with ease."
                "GENTLE" -> "Hello sweetheart. I'm right here with you. Take a deep breath, and let's study calmly."
                "COACH" -> "Alright, focus time! Let's conquer this topic step by step. You've got this."
                "EXPRESSIVE" -> "Hey! I'm Mumma, your study partner. How does my sweet voice sound to you?"
                else -> "Hey there! I'm Mumma, your personal study partner and companion. I'm ready whenever you are."
            }
        }
        speak(sample, onFinished)
    }

    /**
     * Normalizes written text into warm, sweet, human-flowing speech phonetics with breathe timing.
     */
    private fun normalizeForHumanSpeech(raw: String, breatheTiming: Boolean, language: String): String {
        var text = raw

        // 1. Remove memory tags, json blocks, and code blocks
        text = text.replace(Regex("<<<.*?>>>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("```[\\s\\S]*?```"), " ")
        text = text.replace(Regex("`[^`]*`"), " ")

        // 2. Remove markdown symbols and headers
        text = text.replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
        text = text.replace(Regex("[*_~]"), "")
        text = text.replace(Regex("^\r?\n?[-*•]\\s*", RegexOption.MULTILINE), " ")

        // 3. Expand scientific & educational acronyms for natural human cadence
        val replacements = listOf(
            Regex("\\bCO2\\b", RegexOption.IGNORE_CASE) to "C O 2",
            Regex("\\bH2O\\b", RegexOption.IGNORE_CASE) to "H 2 O",
            Regex("\\bATP\\b") to "A T P",
            Regex("\\bNADPH\\b") to "N A D P H",
            Regex("\\bDNA\\b") to "D N A",
            Regex("\\bRNA\\b") to "R N A",
            Regex("\\bpH\\b") to "p H",
            Regex("\\bF\\s*=\\s*ma\\b", RegexOption.IGNORE_CASE) to "F equals m a",
            Regex("\\be\\.g\\.,?|\\beg\\b", RegexOption.IGNORE_CASE) to "for example,",
            Regex("\\bi\\.e\\.,?|\\bie\\b", RegexOption.IGNORE_CASE) to "that is,",
            Regex("\\bvs\\.?\\b", RegexOption.IGNORE_CASE) to "versus",
            Regex("\\betc\\.?\\b", RegexOption.IGNORE_CASE) to "et cetera",
            Regex("\\bapprox\\.?\\b", RegexOption.IGNORE_CASE) to "approximately",
            Regex("\\bw/\\b", RegexOption.IGNORE_CASE) to "with",
            Regex("\\bAI\\b") to "A I",
            Regex("\\bUI\\b") to "U I",
            Regex("%") to " percent",
            Regex("&") to " and ",
            Regex("\\+") to " plus ",
            Regex("=") to " equals ",
            Regex("->|→") to " leads to "
        )

        for ((regex, replacement) in replacements) {
            text = text.replace(regex, replacement)
        }

        // 4. Breathe Timing: Insert gentle rhythmic pauses between clauses
        if (breatheTiming) {
            // Add subtle comma pauses after conjunctions for natural human respiration
            text = text.replace(Regex("([.!?])\\s+"), "$1 , ")
                .replace(Regex("\\b(However|Therefore|Moreover|Basically|Remember|In fact|Aur haan|Toh beta)\\b"), "$1,")
        }

        // 5. Clean consecutive whitespace and extra punctuation
        text = text.replace(Regex("\\s+"), " ")
            .replace(Regex("([.!?])\\s*([.!?])+"), "$1")
            .trim()

        return text
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
        speechCompletionCallback?.invoke()
        speechCompletionCallback = null
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError?.invoke("Speech recognition is not available or voice service is disabled on this device.")
            return
        }

        scope.launch(Dispatchers.Main) {
            val langPref = preferences.language.first()

            mainHandler.post {
                try {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {
                                _isListening.value = true
                            }

                            override fun onBeginningOfSpeech() {
                                _isListening.value = true
                            }

                            override fun onRmsChanged(rmsdB: Float) {
                                _audioRmsDb.value = (rmsdB.coerceIn(0f, 10f) / 10f)
                            }

                            override fun onBufferReceived(buffer: ByteArray?) {}

                            override fun onEndOfSpeech() {
                                _isListening.value = false
                                _audioRmsDb.value = 0f
                            }

                            override fun onError(error: Int) {
                                _isListening.value = false
                                _audioRmsDb.value = 0f
                                when (error) {
                                    SpeechRecognizer.ERROR_NO_MATCH -> {}
                                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {}
                                    SpeechRecognizer.ERROR_AUDIO -> onError?.invoke("Microphone audio recording error.")
                                    SpeechRecognizer.ERROR_NETWORK -> onError?.invoke("Network issue with speech recognizer.")
                                    else -> {}
                                }
                            }

                            override fun onResults(results: Bundle?) {
                                _isListening.value = false
                                _audioRmsDb.value = 0f
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                val text = matches?.firstOrNull() ?: ""
                                if (text.isNotBlank()) {
                                    _recognizedText.value = text
                                    onResult(text)
                                }
                            }

                            override fun onPartialResults(partialResults: Bundle?) {
                                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                val text = matches?.firstOrNull() ?: ""
                                if (text.isNotBlank()) {
                                    _recognizedText.value = text
                                }
                            }

                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }

                    val recognizerLocale = when (langPref) {
                        "HINDI" -> "hi-IN"
                        "HINGLISH" -> "en-IN"
                        else -> "en-US"
                    }

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognizerLocale)
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    }

                    speechRecognizer?.startListening(intent)
                } catch (e: Exception) {
                    _isListening.value = false
                    onError?.invoke("Microphone error: ${e.message}")
                }
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore
            }
            _isListening.value = false
            _audioRmsDb.value = 0f
        }
    }

    fun release() {
        stopSpeaking()
        mainHandler.post {
            tts?.shutdown()
            tts = null
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }
}
