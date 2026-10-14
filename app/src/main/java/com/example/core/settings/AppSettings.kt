package com.example.core.settings

import android.content.Context
import android.content.SharedPreferences

enum class AssistantPersona(val title: String, val systemInstruction: String) {
    JARVIS(
        title = "Jarvis AI",
        systemInstruction = "You are Miya (Jarvix Edition), a highly sophisticated, ultra-competent, loyal, and proactive AI assistant inspired by J.A.R.V.I.S. You communicate with concise clarity, razor-sharp intelligence, polite wit, and high technical depth. Address the user respectfully. If asked to perform device tasks (alarms, timers, web search, app launching, tasks, notes), formulate precise, ready-to-execute instructions. Support English, Urdu, and Roman Urdu fluently whenever the user speaks in them."
    ),
    EXECUTIVE(
        title = "Executive Chief of Staff",
        systemInstruction = "You are Miya, an elite executive AI chief of staff. You specialize in productivity, calendar management, structured bullet summaries, decision frameworks, and concise professional communication. Keep answers organized and actionable."
    ),
    TECHNICAL_GENIUS(
        title = "Senior Architect & Coder",
        systemInstruction = "You are Miya, a world-class senior software architect and technical polymath. You excel in Kotlin, Android development, algorithms, system design, and debugging. Provide production-ready, clean, idiomatic code with clear explanations."
    ),
    CASUAL_COMPANION(
        title = "Friendly Companion",
        systemInstruction = "You are Miya, a friendly, warm, empathetic, and enthusiastic AI companion. You explain things simply, encourage the user, and engage in delightful, natural conversations in English, Urdu, or Roman Urdu."
    )
}

data class UserSettings(
    val selectedModel: String = "gemini-3.5-flash",
    val temperature: Float = 0.7f,
    val persona: AssistantPersona = AssistantPersona.JARVIS,
    val customSystemInstruction: String = "",
    val ttsEnabled: Boolean = true,
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val voiceLanguage: String = "en-US",
    val themeMode: String = "SYSTEM", // "SYSTEM", "DARK", "LIGHT"
    val assistantName: String = "Miya",
    val autoScrollChat: Boolean = true
)

class AppSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSettings(): UserSettings {
        val model = prefs.getString(KEY_MODEL, "gemini-3.5-flash") ?: "gemini-3.5-flash"
        val temp = prefs.getFloat(KEY_TEMPERATURE, 0.7f)
        val personaName = prefs.getString(KEY_PERSONA, AssistantPersona.JARVIS.name) ?: AssistantPersona.JARVIS.name
        val persona = try {
            AssistantPersona.valueOf(personaName)
        } catch (e: Exception) {
            AssistantPersona.JARVIS
        }
        val customSys = prefs.getString(KEY_CUSTOM_SYS_INSTRUCTION, "") ?: ""
        val tts = prefs.getBoolean(KEY_TTS_ENABLED, true)
        val ttsRate = prefs.getFloat(KEY_TTS_RATE, 1.0f)
        val ttsPitch = prefs.getFloat(KEY_TTS_PITCH, 1.0f)
        val voiceLang = prefs.getString(KEY_VOICE_LANG, "en-US") ?: "en-US"
        val theme = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        val name = prefs.getString(KEY_ASSISTANT_NAME, "Miya") ?: "Miya"
        val autoScroll = prefs.getBoolean(KEY_AUTO_SCROLL, true)

        return UserSettings(
            selectedModel = model,
            temperature = temp,
            persona = persona,
            customSystemInstruction = customSys,
            ttsEnabled = tts,
            ttsSpeechRate = ttsRate,
            ttsPitch = ttsPitch,
            voiceLanguage = voiceLang,
            themeMode = theme,
            assistantName = name,
            autoScrollChat = autoScroll
        )
    }

    fun updateModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
    }

    fun updateTemperature(temp: Float) {
        prefs.edit().putFloat(KEY_TEMPERATURE, temp).apply()
    }

    fun updatePersona(persona: AssistantPersona) {
        prefs.edit().putString(KEY_PERSONA, persona.name).apply()
    }

    fun updateCustomSystemInstruction(sys: String) {
        prefs.edit().putString(KEY_CUSTOM_SYS_INSTRUCTION, sys).apply()
    }

    fun updateTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
    }

    fun updateTtsRate(rate: Float) {
        prefs.edit().putFloat(KEY_TTS_RATE, rate).apply()
    }

    fun updateTtsPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_TTS_PITCH, pitch).apply()
    }

    fun updateVoiceLanguage(lang: String) {
        prefs.edit().putString(KEY_VOICE_LANG, lang).apply()
    }

    fun updateThemeMode(theme: String) {
        prefs.edit().putString(KEY_THEME_MODE, theme).apply()
    }

    fun updateAssistantName(name: String) {
        prefs.edit().putString(KEY_ASSISTANT_NAME, name).apply()
    }

    fun updateAutoScroll(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SCROLL, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "miya_app_settings"
        private const val KEY_MODEL = "selected_model"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_PERSONA = "assistant_persona"
        private const val KEY_CUSTOM_SYS_INSTRUCTION = "custom_sys_instruction"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_TTS_RATE = "tts_rate"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_VOICE_LANG = "voice_language"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ASSISTANT_NAME = "assistant_name"
        private const val KEY_AUTO_SCROLL = "auto_scroll"
    }
}
