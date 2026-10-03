package com.example.data.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.PhotoAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

sealed class GeminiResult<out T> {
    data class Success<T>(val data: T, val message: String? = null) : GeminiResult<T>()
    data class Error(val errorMessage: String, val throwable: Throwable? = null) : GeminiResult<Nothing>()
}

data class EditedPhotoResult(
    val bitmap: Bitmap,
    val textExplanation: String?
)

class GeminiApiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Retrieves API key from BuildConfig or custom user-provided override
     */
    fun resolveApiKey(userKey: String?): String {
        if (!userKey.isNullOrBlank()) return userKey.trim()
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    /**
     * Resizes a bitmap so that the max dimension is within maxDimension (e.g. 1024),
     * ensuring fast transmission and compliance with API payloads.
     */
    private fun scaleBitmapForApi(src: Bitmap, maxDimension: Int = 1024): Bitmap {
        val width = src.width
        val height = src.height
        if (width <= maxDimension && height <= maxDimension) return src

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (ratio > 1) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val scaled = scaleBitmapForApi(bitmap, 1024)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Edits a photo using Gemini image model (gemini-2.5-flash-image)
     */
    suspend fun editPhotoWithGemini(
        sourceBitmap: Bitmap,
        prompt: String,
        apiKey: String,
        modelName: String = "gemini-2.5-flash-image"
    ): GeminiResult<EditedPhotoResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error(
                "Kunci API Gemini belum dikonfigurasi. Masukkan kunci API di Pengaturan atau Secrets Panel."
            )
        }

        try {
            val base64Data = bitmapToBase64(sourceBitmap)

            // Construct JSON request for Gemini
            val root = JSONObject()
            val contents = JSONArray()
            val contentObj = JSONObject()
            val parts = JSONArray()

            // Text instruction part
            val textPart = JSONObject().apply {
                put("text", "Perform this precise photo edit on the provided image: $prompt. Maintain the highest visual quality.")
            }
            parts.put(textPart)

            // Image part
            val inlineData = JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", base64Data)
            }
            val imagePart = JSONObject().apply {
                put("inlineData", inlineData)
            }
            parts.put(imagePart)

            contentObj.put("parts", parts)
            contents.put(contentObj)
            root.put("contents", contents)

            // Generation config specifying both IMAGE and TEXT
            val generationConfig = JSONObject().apply {
                val modalities = JSONArray().apply {
                    put("IMAGE")
                    put("TEXT")
                }
                put("responseModalities", modalities)
            }
            root.put("generationConfig", generationConfig)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val requestBody = root.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseString)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}: $responseString"
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                return@withContext GeminiResult.Error(errorMsg)
            }

            val jsonResponse = JSONObject(responseString)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiResult.Error("Gemini tidak mengembalikan hasil. Coba lagi atau ubah instruksi prompt.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val responseContent = firstCandidate.optJSONObject("content")
            val responseParts = responseContent?.optJSONArray("parts")

            if (responseParts == null || responseParts.length() == 0) {
                return@withContext GeminiResult.Error("Format konten hasil Gemini kosong.")
            }

            var outputBitmap: Bitmap? = null
            var outputText: String? = null

            for (i in 0 until responseParts.length()) {
                val part = responseParts.getJSONObject(i)
                if (part.has("inlineData")) {
                    val imgData = part.getJSONObject("inlineData").optString("data", "")
                    if (imgData.isNotEmpty()) {
                        val decodedBytes = Base64.decode(imgData, Base64.DEFAULT)
                        outputBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    }
                } else if (part.has("text")) {
                    outputText = part.optString("text")
                }
            }

            if (outputBitmap != null) {
                GeminiResult.Success(EditedPhotoResult(outputBitmap, outputText))
            } else if (!outputText.isNullOrBlank()) {
                // If model only returned text advice without image
                GeminiResult.Error("Gemini memberikan catatan teks namun gambar belum dapat digenerasi: $outputText")
            } else {
                GeminiResult.Error("Tidak ada data gambar dalam respons Gemini.")
            }

        } catch (e: Exception) {
            GeminiResult.Error("Gagal menghubungi Gemini AI: ${e.localizedMessage ?: e.message}", e)
        }
    }

    /**
     * Visual Photo Analysis with Gemini 3.5 Flash
     */
    suspend fun analyzePhoto(
        sourceBitmap: Bitmap,
        apiKey: String
    ): GeminiResult<PhotoAnalysis> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("Kunci API Gemini diperlukan untuk analisis foto.")
        }

        try {
            val base64Data = bitmapToBase64(sourceBitmap)
            val promptText = """
                Analisis foto ini secara profesional dan visual. 
                Jawab HANYA dalam format JSON mentah tanpa markdown (tanpa ```json atau ```), dengan skema berikut:
                {
                  "score": 8.7,
                  "scoreSummary": "Pencahayaan natural yang seimbang dengan fokus tajam",
                  "lighting": "Pencahayaan alami lembut dengan kontras halus",
                  "composition": "Komposisi proporsional dengan latar belakang yang rapi",
                  "mood": "Hangat, elegan, dan estetik",
                  "description": "Deskripsi singkat 1 kalimat tentang foto ini",
                  "tags": ["Potret", "Alami", "Bokeh", "Estetik"],
                  "suggestedPrompts": [
                    "Ubah foto menjadi gaya anime modern dengan pencahayaan neon",
                    "Ganti latar belakang dengan pemandangan pantai sunset tropis",
                    "Tambahkan efek vintage analog film 35mm tahun 1980",
                    "Ubah menjadi lukisan cat minyak klasik bertekstur tebal"
                  ]
                }
            """.trimIndent()

            val root = JSONObject()
            val contents = JSONArray()
            val contentObj = JSONObject()
            val parts = JSONArray()

            parts.put(JSONObject().apply { put("text", promptText) })
            parts.put(JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Data)
                })
            })

            contentObj.put("parts", parts)
            contents.put(contentObj)
            root.put("contents", contents)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestBody = root.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GeminiResult.Error("Analisis gagal: HTTP ${response.code}")
            }

            val jsonResponse = JSONObject(responseString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val textPart = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")

            if (textPart.isNullOrBlank()) {
                return@withContext GeminiResult.Error("Respons analisis kosong dari Gemini.")
            }

            // Clean markdown code blocks if any
            var cleanedJson = textPart.trim()
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.removePrefix("```json")
            } else if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson.removePrefix("```")
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.removeSuffix("```")
            }
            cleanedJson = cleanedJson.trim()

            val analysisObj = JSONObject(cleanedJson)
            val score = analysisObj.optDouble("score", 8.0).toFloat()
            val scoreSummary = analysisObj.optString("scoreSummary", "Foto berkualitas tinggi")
            val lighting = analysisObj.optString("lighting", "Pencahayaan baik")
            val composition = analysisObj.optString("composition", "Komposisi seimbang")
            val mood = analysisObj.optString("mood", "Menyenangkan")
            val description = analysisObj.optString("description", "Foto indah siap diedit.")

            val tagsList = mutableListOf<String>()
            val tagsArr = analysisObj.optJSONArray("tags")
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    tagsList.add(tagsArr.getString(i))
                }
            }

            val promptsList = mutableListOf<String>()
            val promptsArr = analysisObj.optJSONArray("suggestedPrompts")
            if (promptsArr != null) {
                for (i in 0 until promptsArr.length()) {
                    promptsList.add(promptsArr.getString(i))
                }
            }

            GeminiResult.Success(
                PhotoAnalysis(
                    score = score,
                    scoreSummary = scoreSummary,
                    lighting = lighting,
                    composition = composition,
                    mood = mood,
                    description = description,
                    tags = tagsList,
                    suggestedPrompts = promptsList
                )
            )

        } catch (e: Exception) {
            GeminiResult.Error("Gagal menganalisis foto: ${e.localizedMessage ?: e.message}", e)
        }
    }
}
