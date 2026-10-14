package com.example.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.TaskEntity
import com.example.data.repository.MiyaRepository
import com.example.domain.actions.AssistantAction
import com.example.domain.actions.DeviceActionHandler
import com.example.domain.voice.VoiceAssistantManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class GenerationState {
    object Idle : GenerationState()
    object Thinking : GenerationState()
    data class Streaming(val partialText: String) : GenerationState()
    data class Error(val message: String) : GenerationState()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatViewModel(
    application: Application,
    private val repository: MiyaRepository,
    private val deviceActionHandler: DeviceActionHandler
) : AndroidViewModel(application) {

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId.flatMapLatest { id ->
        if (id != null) repository.getMessagesForSession(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    private val _attachedImageUri = MutableStateFlow<Uri?>(null)
    val attachedImageUri: StateFlow<Uri?> = _attachedImageUri.asStateFlow()

    private val _isApiKeyConfigured = MutableStateFlow(true)
    val isApiKeyConfigured: StateFlow<Boolean> = _isApiKeyConfigured.asStateFlow()

    private var generationJob: Job? = null

    // Voice Assistant
    val voiceAssistant = VoiceAssistantManager(
        context = application.applicationContext,
        onSpeechRecognized = { speechText ->
            sendMessage(speechText)
        },
        onError = { errorMsg ->
            _generationState.value = GenerationState.Error(errorMsg)
        }
    )

    init {
        checkApiKey()
        initDefaultSession()
    }

    fun checkApiKey() {
        val key = repository.getEffectiveApiKey()
        _isApiKeyConfigured.value = key.isNotBlank()
    }

    private fun initDefaultSession() {
        viewModelScope.launch {
            repository.getAllSessions().collect { list ->
                if (_currentSessionId.value == null) {
                    if (list.isNotEmpty()) {
                        _currentSessionId.value = list.first().id
                    } else {
                        val newId = repository.createNewSession("Miya AI Session")
                        _currentSessionId.value = newId
                        // Initial welcome greeting
                        repository.insertMessage(
                            ChatMessageEntity(
                                sessionId = newId,
                                role = "assistant",
                                content = "Hello! I'm Miya, your personal AI assistant. What can I do for you today?"
                            )
                        )
                    }
                }
            }
        }
    }

    fun selectSession(sessionId: Long) {
        stopGeneration()
        voiceAssistant.stopSpeaking()
        _currentSessionId.value = sessionId
    }

    fun createNewSession() {
        viewModelScope.launch {
            stopGeneration()
            voiceAssistant.stopSpeaking()
            val newId = repository.createNewSession("Conversation ${System.currentTimeMillis() % 1000}")
            _currentSessionId.value = newId
            repository.insertMessage(
                ChatMessageEntity(
                    sessionId = newId,
                    role = "assistant",
                    content = "Hello! I'm Miya, your personal AI assistant. What can I do for you today?"
                )
            )
        }
    }

    fun attachImage(uri: Uri?) {
        _attachedImageUri.value = uri
    }

    fun sendMessage(promptText: String) {
        val trimmed = promptText.trim()
        val currentImage = _attachedImageUri.value
        if (trimmed.isEmpty() && currentImage == null) return

        val apiKey = repository.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            _isApiKeyConfigured.value = false
            _generationState.value = GenerationState.Error(
                "Gemini API key is not configured. Please enter your API key in Settings."
            )
            return
        }

        val sessionId = _currentSessionId.value ?: return

        // Clear input attachment
        _attachedImageUri.value = null

        viewModelScope.launch {
            // Check direct local actions
            val detectedAction = deviceActionHandler.detectActionFromText(trimmed)

            // Save user message
            repository.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    content = trimmed,
                    imageUri = currentImage?.toString()
                )
            )

            // Auto-update session title if it was default
            val currentMessages = messages.value
            if (currentMessages.size <= 2 && trimmed.isNotEmpty()) {
                val shortTitle = if (trimmed.length > 28) trimmed.take(25) + "..." else trimmed
                repository.updateSessionTitle(sessionId, shortTitle)
            }

            // Stream AI Response
            streamResponse(sessionId, trimmed, currentImage, detectedAction)
        }
    }

    private fun streamResponse(
        sessionId: Long,
        prompt: String,
        imageUri: Uri?,
        detectedAction: AssistantAction?
    ) {
        generationJob?.cancel()
        _generationState.value = GenerationState.Thinking

        val apiKey = repository.getEffectiveApiKey()
        val settings = repository.getSettings()

        generationJob = viewModelScope.launch {
            try {
                val request = repository.prepareGeminiRequest(sessionId, prompt, imageUri)
                val fullResponseBuilder = StringBuilder()

                repository.streamGenerateContent(apiKey, settings.selectedModel, request).collect { chunk ->
                    fullResponseBuilder.append(chunk)
                    _generationState.value = GenerationState.Streaming(fullResponseBuilder.toString())
                }

                val finalOutput = fullResponseBuilder.toString().ifEmpty {
                    "I processed your request, sir."
                }

                _generationState.value = GenerationState.Idle

                // Save assistant message to Room
                val actionType = detectedAction?.type
                val actionData = when (detectedAction) {
                    is AssistantAction.OpenApp -> detectedAction.appName
                    is AssistantAction.WebSearch -> detectedAction.query
                    is AssistantAction.OpenUrl -> detectedAction.url
                    is AssistantAction.SetAlarm -> "%02d:%02d".format(detectedAction.hour, detectedAction.minute)
                    is AssistantAction.SetTimer -> "${detectedAction.durationSeconds / 60}m"
                    is AssistantAction.CreateTask -> detectedAction.taskTitle
                    else -> null
                }

                repository.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = finalOutput,
                        actionType = actionType,
                        actionData = actionData
                    )
                )

                // If user has TTS enabled, speak response
                if (settings.ttsEnabled) {
                    voiceAssistant.speak(finalOutput, settings.ttsSpeechRate, settings.ttsPitch)
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    _generationState.value = GenerationState.Idle
                } else {
                    val errMsg = e.localizedMessage ?: "Failed to generate response."
                    _generationState.value = GenerationState.Error(errMsg)
                }
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        _generationState.value = GenerationState.Idle
    }

    fun regenerateLastResponse() {
        val currentMessages = messages.value
        val lastUserMessage = currentMessages.lastOrNull { it.role == "user" } ?: return
        val sessionId = _currentSessionId.value ?: return

        val imageUri = lastUserMessage.imageUri?.let { Uri.parse(it) }
        val detectedAction = deviceActionHandler.detectActionFromText(lastUserMessage.content)

        streamResponse(sessionId, lastUserMessage.content, imageUri, detectedAction)
    }

    fun executeAssistantAction(actionType: String, actionData: String?) {
        when (actionType) {
            "OPEN_APP" -> deviceActionHandler.openApp(actionData ?: "")
            "WEB_SEARCH" -> deviceActionHandler.searchWeb(actionData ?: "")
            "OPEN_URL" -> deviceActionHandler.openUrl(actionData ?: "")
            "SET_ALARM" -> {
                val parts = actionData?.split(":")
                val hour = parts?.getOrNull(0)?.toIntOrNull() ?: 8
                val minute = parts?.getOrNull(1)?.toIntOrNull() ?: 0
                deviceActionHandler.setAlarm(hour, minute, "Miya Assistant Alarm")
            }
            "SET_TIMER" -> {
                val mins = actionData?.removeSuffix("m")?.toIntOrNull() ?: 5
                deviceActionHandler.setTimer(mins * 60, "Miya Timer")
            }
            "CREATE_TASK" -> {
                viewModelScope.launch {
                    repository.insertTask(
                        TaskEntity(
                            title = actionData ?: "New Task",
                            notes = "Created via Miya AI Assistant"
                        )
                    )
                }
            }
        }
    }

    fun copyMessage(text: String) {
        deviceActionHandler.copyToClipboard(text)
    }

    fun shareMessage(text: String) {
        deviceActionHandler.shareText(text)
    }

    fun speakMessage(text: String) {
        val settings = repository.getSettings()
        voiceAssistant.speak(text, settings.ttsSpeechRate, settings.ttsPitch)
    }

    fun toggleVoiceListening() {
        if (voiceAssistant.isListening.value) {
            voiceAssistant.stopListening()
        } else {
            val settings = repository.getSettings()
            voiceAssistant.startListening(settings.voiceLanguage)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistant.release()
    }
}
