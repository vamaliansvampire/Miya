package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

sealed class ConnectionTestResult {
    data class Success(val latencyMs: Long, val model: String) : ConnectionTestResult()
    data class Failure(val errorMessage: String, val statusCode: Int? = null) : ConnectionTestResult()
}

class GeminiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    /**
     * Tests the API key and model connectivity with a lightweight ping.
     */
    suspend fun testConnection(apiKey: String, model: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ConnectionTestResult.Failure("API Key is empty. Please enter a valid Gemini API key.")
        }

        val startTime = System.currentTimeMillis()
        val pingRequest = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = "Hello! Please reply with exactly the word 'ONLINE'."))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.0f,
                maxOutputTokens = 10
            )
        )

        try {
            val jsonBody = requestAdapter.toJson(pingRequest)
            val url = "$BASE_URL/v1beta/models/$model:generateContent?key=$apiKey"
            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            val latency = System.currentTimeMillis() - startTime
            val bodyString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorParsed = try {
                    responseAdapter.fromJson(bodyString)?.error
                } catch (e: Exception) {
                    null
                }

                val msg = when (response.code) {
                    400 -> "HTTP 400 Bad Request: ${errorParsed?.message ?: "Check your model or request."}"
                    403 -> "HTTP 403 Forbidden: Invalid API key or permission denied."
                    404 -> "HTTP 404 Not Found: Model '$model' not found or unsupported."
                    429 -> "HTTP 429 Too Many Requests: Rate limit or quota exceeded."
                    500, 503 -> "HTTP ${response.code}: Gemini service temporary issue. Try again."
                    else -> "HTTP ${response.code}: ${errorParsed?.message ?: bodyString.take(150)}"
                }
                return@withContext ConnectionTestResult.Failure(msg, response.code)
            }

            val parsed = responseAdapter.fromJson(bodyString)
            val text = parsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                ConnectionTestResult.Success(latencyMs = latency, model = model)
            } else {
                ConnectionTestResult.Failure("Connected, but received unexpected empty candidate payload.")
            }
        } catch (e: Exception) {
            ConnectionTestResult.Failure("Network error: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Streams content chunk by chunk using Server-Sent Events (SSE).
     */
    fun streamGenerateContent(
        apiKey: String,
        model: String,
        request: GeminiRequest
    ): Flow<String> = flow {
        val jsonBody = requestAdapter.toJson(request)
        val url = "$BASE_URL/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey"

        val httpRequest = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string().orEmpty()
            val errorMsg = try {
                responseAdapter.fromJson(errorBody)?.error?.message ?: "HTTP ${response.code}"
            } catch (e: Exception) {
                "HTTP ${response.code}: $errorBody"
            }
            throw RuntimeException(errorMsg)
        }

        val body = response.body ?: throw RuntimeException("Empty response body from Gemini API")
        val reader = BufferedReader(InputStreamReader(body.byteStream()))

        reader.use { bufferedReader ->
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (!currentLine.startsWith("data:")) continue

                val jsonPayload = currentLine.removePrefix("data:").trim()
                if (jsonPayload.isEmpty() || jsonPayload == "[DONE]") continue

                try {
                    val parsed = responseAdapter.fromJson(jsonPayload)
                    val chunkText = parsed?.candidates?.firstOrNull()
                        ?.content?.parts?.firstOrNull()?.text
                    if (!chunkText.isNullOrEmpty()) {
                        emit(chunkText)
                    }
                } catch (e: Exception) {
                    // Ignore transient chunk parse errors
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Non-streaming direct generation fallback.
     */
    suspend fun generateContent(
        apiKey: String,
        model: String,
        request: GeminiRequest
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = requestAdapter.toJson(request)
            val url = "$BASE_URL/v1beta/models/$model:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            val bodyString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = try {
                    responseAdapter.fromJson(bodyString)?.error?.message ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $bodyString"
                }
                return@withContext Result.failure(RuntimeException(errorMsg))
            }

            val parsed = responseAdapter.fromJson(bodyString)
            val text = parsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(RuntimeException("No response text found"))

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com"
    }
}
