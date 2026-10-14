package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.example.core.security.KeyStoreManager
import com.example.core.settings.AppSettings
import com.example.core.settings.UserSettings
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.MiyaDao
import com.example.data.local.NoteEntity
import com.example.data.local.TaskEntity
import com.example.data.remote.ConnectionTestResult
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiInlineData
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

class MiyaRepository(
    private val context: Context,
    private val miyaDao: MiyaDao,
    private val keyStoreManager: KeyStoreManager,
    private val appSettings: AppSettings,
    private val geminiClient: GeminiClient
) {

    // --- Key Management ---
    fun getEffectiveApiKey(): String {
        val userSavedKey = keyStoreManager.getApiKey()?.trim().orEmpty()
        if (userSavedKey.isNotEmpty()) {
            return userSavedKey
        }
        // Fallback to BuildConfig if configured in AI Studio Secrets panel
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Exception) {
            ""
        }
        return if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    fun hasCustomKey(): Boolean {
        return !keyStoreManager.getApiKey().isNullOrBlank()
    }

    fun saveApiKey(key: String) {
        keyStoreManager.saveApiKey(key.trim())
    }

    fun clearApiKey() {
        keyStoreManager.clearApiKey()
    }

    suspend fun testConnection(apiKey: String, model: String): ConnectionTestResult {
        return geminiClient.testConnection(apiKey, model)
    }

    // --- Settings ---
    fun getSettings(): UserSettings = appSettings.getSettings()

    fun updateSettings(settings: UserSettings) {
        appSettings.updateModel(settings.selectedModel)
        appSettings.updateTemperature(settings.temperature)
        appSettings.updatePersona(settings.persona)
        appSettings.updateCustomSystemInstruction(settings.customSystemInstruction)
        appSettings.updateTtsEnabled(settings.ttsEnabled)
        appSettings.updateTtsRate(settings.ttsSpeechRate)
        appSettings.updateTtsPitch(settings.ttsPitch)
        appSettings.updateVoiceLanguage(settings.voiceLanguage)
        appSettings.updateThemeMode(settings.themeMode)
        appSettings.updateAssistantName(settings.assistantName)
        appSettings.updateAutoScroll(settings.autoScrollChat)
    }

    // --- Sessions & Chat ---
    fun getAllSessions(): Flow<List<ChatSessionEntity>> = miyaDao.getAllSessions()

    suspend fun createNewSession(title: String = "New Conversation"): Long {
        val session = ChatSessionEntity(title = title)
        return miyaDao.insertSession(session)
    }

    suspend fun updateSessionTitle(sessionId: Long, newTitle: String) {
        val session = miyaDao.getSessionById(sessionId) ?: return
        miyaDao.updateSession(session.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteSession(sessionId: Long) {
        miyaDao.deleteSession(sessionId)
    }

    suspend fun clearAllConversations() {
        miyaDao.deleteAllSessions()
    }

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> =
        miyaDao.getMessagesForSession(sessionId)

    suspend fun insertMessage(message: ChatMessageEntity): Long {
        val id = miyaDao.insertMessage(message)
        // Update session timestamp
        val session = miyaDao.getSessionById(message.sessionId)
        if (session != null) {
            miyaDao.updateSession(session.copy(updatedAt = System.currentTimeMillis()))
        }
        return id
    }

    suspend fun deleteMessage(messageId: Long) {
        miyaDao.deleteMessageById(messageId)
    }

    // --- Gemini Call Builder ---
    suspend fun prepareGeminiRequest(
        sessionId: Long,
        currentPrompt: String,
        imageUri: Uri? = null
    ): GeminiRequest = withContext(Dispatchers.IO) {
        val settings = getSettings()
        val history = miyaDao.getMessagesListForSession(sessionId)

        val contents = mutableListOf<GeminiContent>()

        // Take the last 12 messages for conversation context memory
        val recentHistory = history.takeLast(12)
        for (msg in recentHistory) {
            val role = if (msg.role == "user") "user" else "model"
            contents.add(
                GeminiContent(
                    role = role,
                    parts = listOf(GeminiPart(text = msg.content))
                )
            )
        }

        // Add current turn
        val currentParts = mutableListOf<GeminiPart>()
        currentParts.add(GeminiPart(text = currentPrompt))

        if (imageUri != null) {
            val inlineData = uriToGeminiInlineData(imageUri)
            if (inlineData != null) {
                currentParts.add(GeminiPart(inlineData = inlineData))
            }
        }

        contents.add(GeminiContent(role = "user", parts = currentParts))

        // System Instruction
        val systemText = if (settings.customSystemInstruction.isNotBlank()) {
            "${settings.persona.systemInstruction}\n\nAdditional instructions: ${settings.customSystemInstruction}"
        } else {
            settings.persona.systemInstruction
        }

        GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(
                role = "system",
                parts = listOf(GeminiPart(text = systemText))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = settings.temperature,
                topP = 0.95f
            )
        )
    }

    private fun uriToGeminiInlineData(uri: Uri): GeminiInlineData? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // Scale down if too large to conserve memory and bandwidth
                val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
                    val scale = 1024f / maxOf(bitmap.width, bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt(),
                        true
                    )
                } else bitmap

                val outputStream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                GeminiInlineData(mimeType = "image/jpeg", data = base64)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun streamGenerateContent(apiKey: String, model: String, request: GeminiRequest): Flow<String> {
        return geminiClient.streamGenerateContent(apiKey, model, request)
    }

    // --- Tasks & Notes ---
    fun getAllTasks(): Flow<List<TaskEntity>> = miyaDao.getAllTasks()

    suspend fun insertTask(task: TaskEntity): Long = miyaDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = miyaDao.updateTask(task)

    suspend fun deleteTask(taskId: Long) = miyaDao.deleteTask(taskId)

    suspend fun clearAllTasks() = miyaDao.deleteAllTasks()

    fun getAllNotes(): Flow<List<NoteEntity>> = miyaDao.getAllNotes()

    suspend fun insertNote(note: NoteEntity): Long = miyaDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) = miyaDao.updateNote(note)

    suspend fun deleteNote(noteId: Long) = miyaDao.deleteNote(noteId)

    suspend fun clearAllNotes() = miyaDao.deleteAllNotes()
}
