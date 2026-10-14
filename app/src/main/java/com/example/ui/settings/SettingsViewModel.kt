package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.settings.AssistantPersona
import com.example.core.settings.UserSettings
import com.example.data.remote.ConnectionTestResult
import com.example.data.repository.MiyaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ApiConnectionStatus {
    object Idle : ApiConnectionStatus()
    object Testing : ApiConnectionStatus()
    data class Connected(val latencyMs: Long, val model: String) : ApiConnectionStatus()
    data class Error(val message: String) : ApiConnectionStatus()
}

class SettingsViewModel(
    application: Application,
    private val repository: MiyaRepository
) : AndroidViewModel(application) {

    private val _userSettings = MutableStateFlow(repository.getSettings())
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    private val _apiKeyInput = MutableStateFlow("")
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    private val _showApiKey = MutableStateFlow(false)
    val showApiKey: StateFlow<Boolean> = _showApiKey.asStateFlow()

    private val _hasStoredKey = MutableStateFlow(repository.hasCustomKey())
    val hasStoredKey: StateFlow<Boolean> = _hasStoredKey.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ApiConnectionStatus>(ApiConnectionStatus.Idle)
    val connectionStatus: StateFlow<ApiConnectionStatus> = _connectionStatus.asStateFlow()

    init {
        loadCurrentKey()
    }

    private fun loadCurrentKey() {
        val key = repository.getEffectiveApiKey()
        _apiKeyInput.value = key
        _hasStoredKey.value = repository.hasCustomKey()
    }

    fun onApiKeyChanged(newKey: String) {
        _apiKeyInput.value = newKey
        _connectionStatus.value = ApiConnectionStatus.Idle
    }

    fun toggleShowApiKey() {
        _showApiKey.value = !_showApiKey.value
    }

    fun saveApiKey() {
        val key = _apiKeyInput.value.trim()
        if (key.isBlank()) {
            _connectionStatus.value = ApiConnectionStatus.Error("Please enter a valid API key.")
            return
        }
        repository.saveApiKey(key)
        _hasStoredKey.value = true
        testConnection()
    }

    fun clearApiKey() {
        repository.clearApiKey()
        _apiKeyInput.value = ""
        _hasStoredKey.value = false
        _connectionStatus.value = ApiConnectionStatus.Idle
    }

    fun testConnection() {
        val key = _apiKeyInput.value.trim().ifEmpty { repository.getEffectiveApiKey() }
        if (key.isBlank()) {
            _connectionStatus.value = ApiConnectionStatus.Error("No API key available to test. Please enter one above.")
            return
        }

        viewModelScope.launch {
            _connectionStatus.value = ApiConnectionStatus.Testing
            val model = _userSettings.value.selectedModel
            when (val result = repository.testConnection(key, model)) {
                is ConnectionTestResult.Success -> {
                    _connectionStatus.value = ApiConnectionStatus.Connected(result.latencyMs, result.model)
                }
                is ConnectionTestResult.Failure -> {
                    _connectionStatus.value = ApiConnectionStatus.Error(result.errorMessage)
                }
            }
        }
    }

    fun selectModel(model: String) {
        val updated = _userSettings.value.copy(selectedModel = model)
        _userSettings.value = updated
        repository.updateSettings(updated)
    }

    fun setTemperature(temp: Float) {
        val updated = _userSettings.value.copy(temperature = temp)
        _userSettings.value = updated
        repository.updateSettings(updated)
    }

    fun selectPersona(persona: AssistantPersona) {
        val updated = _userSettings.value.copy(persona = persona)
        _userSettings.value = updated
        repository.updateSettings(updated)
    }

    fun setCustomSystemInstruction(sys: String) {
        val updated = _userSettings.value.copy(customSystemInstruction = sys)
        _userSettings.value = updated
        repository.updateSettings(updated)
    }

    fun setTtsEnabled(enabled: Boolean) {
        val updated = _userSettings.value.copy(ttsEnabled = enabled)
        _userSettings.value = updated
        repository.updateSettings(updated)
    }

    fun setTtsSpeechRate(rate: Float) {
        val updated = _userSettings.value.copy(ttsSpeechRate = rate)
        _userSettings.value = updated
        repository.updateSettings(updated)
    }

    fun setTtsPitch(pitch: Float) {
        val updated = _userSettings.value.copy(ttsPitch = pitch)
        _userSettings.value = updated
        repository.updateSettings(updated)
    }

    fun setVoiceLanguage(lang: String) {
        val updated = _userSettings.value.copy(voiceLanguage = lang)
        _userSettings.value = updated
        repository.updateSettings(updated)
    }

    fun setThemeMode(theme: String) {
        val updated = _userSettings.value.copy(themeMode = theme)
        _userSettings.value = updated
        repository.updateSettings(updated)
    }

    fun setAssistantName(name: String) {
        val updated = _userSettings.value.copy(assistantName = name)
        _userSettings.value = updated
        repository.updateSettings(updated)
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            repository.clearAllConversations()
        }
    }

    fun clearAllTasksAndNotes() {
        viewModelScope.launch {
            repository.clearAllTasks()
            repository.clearAllNotes()
        }
    }
}
