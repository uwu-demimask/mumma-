package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MummaPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mumma_prefs", Context.MODE_PRIVATE)

    private val _isMemoryEnabled = MutableStateFlow(prefs.getBoolean(KEY_MEMORY_ENABLED, true))
    val isMemoryEnabled: StateFlow<Boolean> = _isMemoryEnabled.asStateFlow()

    private val _isTtsAutoPlay = MutableStateFlow(prefs.getBoolean(KEY_TTS_AUTO_PLAY, true))
    val isTtsAutoPlay: StateFlow<Boolean> = _isTtsAutoPlay.asStateFlow()

    private val _speechRate = MutableStateFlow(prefs.getFloat(KEY_SPEECH_RATE, 1.0f))
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(prefs.getFloat(KEY_SPEECH_PITCH, 1.05f))
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _isForceOfflineAi = MutableStateFlow(prefs.getBoolean(KEY_FORCE_OFFLINE_AI, false))
    val isForceOfflineAi: StateFlow<Boolean> = _isForceOfflineAi.asStateFlow()

    private val _voicePreset = MutableStateFlow(prefs.getString(KEY_VOICE_PRESET, "SWEET_MUMMA") ?: "SWEET_MUMMA")
    val voicePreset: StateFlow<String> = _voicePreset.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "ENGLISH") ?: "ENGLISH")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _pitchVariation = MutableStateFlow(prefs.getBoolean(KEY_PITCH_VARIATION, true))
    val pitchVariation: StateFlow<Boolean> = _pitchVariation.asStateFlow()

    private val _breatheTiming = MutableStateFlow(prefs.getBoolean(KEY_BREATHE_TIMING, true))
    val breatheTiming: StateFlow<Boolean> = _breatheTiming.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString(KEY_USER_NAME, "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _language.value = lang
    }

    fun setPitchVariation(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PITCH_VARIATION, enabled).apply()
        _pitchVariation.value = enabled
    }

    fun setBreatheTiming(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BREATHE_TIMING, enabled).apply()
        _breatheTiming.value = enabled
    }

    fun setMemoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MEMORY_ENABLED, enabled).apply()
        _isMemoryEnabled.value = enabled
    }

    fun setTtsAutoPlay(autoPlay: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_AUTO_PLAY, autoPlay).apply()
        _isTtsAutoPlay.value = autoPlay
    }

    fun setSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
        _speechRate.value = rate
    }

    fun setSpeechPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_SPEECH_PITCH, pitch).apply()
        _speechPitch.value = pitch
    }

    fun setVoicePreset(preset: String) {
        prefs.edit().putString(KEY_VOICE_PRESET, preset).apply()
        _voicePreset.value = preset
        when (preset) {
            "SWEET_MUMMA" -> {
                setSpeechRate(0.94f)
                setSpeechPitch(1.16f)
            }
            "WARM" -> {
                setSpeechRate(0.98f)
                setSpeechPitch(1.06f)
            }
            "GENTLE" -> {
                setSpeechRate(0.88f)
                setSpeechPitch(1.14f)
            }
            "COACH" -> {
                setSpeechRate(1.05f)
                setSpeechPitch(0.98f)
            }
            "EXPRESSIVE" -> {
                setSpeechRate(0.98f)
                setSpeechPitch(1.12f)
            }
        }
    }

    fun setForceOfflineAi(offline: Boolean) {
        prefs.edit().putBoolean(KEY_FORCE_OFFLINE_AI, offline).apply()
        _isForceOfflineAi.value = offline
    }

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
        _userName.value = name
    }

    companion object {
        private const val KEY_MEMORY_ENABLED = "key_memory_enabled"
        private const val KEY_TTS_AUTO_PLAY = "key_tts_auto_play"
        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"
        private const val KEY_VOICE_PRESET = "key_voice_preset"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_PITCH_VARIATION = "key_pitch_variation"
        private const val KEY_BREATHE_TIMING = "key_breathe_timing"
        private const val KEY_FORCE_OFFLINE_AI = "key_force_offline_ai"
        private const val KEY_USER_NAME = "key_user_name"
    }
}
