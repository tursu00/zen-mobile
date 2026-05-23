package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiSearchHelper {
    private const val TAG = "GeminiSearchHelper"
    private const val MODEL_NAME = "gemini-3.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Recommends the best search engine (google, duckduckgo, bing) based on query semantics.
     */
    suspend fun recommendSearchEngine(query: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Safe fallback
            return@withContext fallbackRecommendation(query)
        }

        val prompt = """
            Determine the single best search engine to answer this query: "$query".
            Options are exactly: "google", "duckduckgo", "bing".
            
            Return ONLY a raw JSON object with this format, do not add markdown wrapping or anything else:
            {
               "engine": "google" or "duckduckgo" or "bing",
               "reason": "short explanation in Turkish"
            }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Request failed code: ${response.code}")
                    return@withContext fallbackRecommendation(query)
                }
                val bodyString = response.body?.string() ?: ""
                Log.d(TAG, "Response: $bodyString")
                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                val parts = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text") ?: ""
                
                // Extract clean JSON block if model returns with backticks
                val cleanJson = if (text.contains("```")) {
                    text.substringAfter("```json").substringBefore("```").trim()
                } else if (text.contains("```")) {
                    text.substringAfter("```").substringBefore("```").trim()
                } else {
                    text.trim()
                }

                val resultObj = JSONObject(cleanJson)
                val engine = resultObj.optString("engine", "google").lowercase()
                if (engine == "google" || engine == "duckduckgo" || engine == "bing") {
                    engine
                } else {
                    "google"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in recommendSearchEngine", e)
            fallbackRecommendation(query)
        }
    }

    private fun fallbackRecommendation(query: String): String {
        // Safe keywords-based classification if API is unavailable or rate-limited
        val lower = query.lowercase().trim()
        return when {
            // Privacy focused or sensitive terms
            lower.contains("secret") || lower.contains("privacy") || lower.contains("gizli") || lower.contains("korsan") || lower.contains("vpn") -> "duckduckgo"
            // Tech products or Microsoft specifics
            lower.contains("windows") || lower.contains("microsoft") || lower.contains("xbox") || lower.contains("office") || lower.contains("azure") -> "bing"
            // General query -> google
            else -> "google"
        }
    }

    /**
     * Generates a structured AI search assistant response (Turkish summary, bullet points, recommendations)
     */
    suspend fun getSearchAssistantSummary(query: String): AISummaryResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AISummaryResult(
                summary = "Yapay Zeka Arama Desteği aktif, ancak API Anahtarı eksik. Lütfen AI Studio Secrets panelinden geçerli bir API anahtarı ekleyin.",
                quickFacts = listOf("API anahtarı eksik", "Çevrimdışı simülasyon modu devrede"),
                suggestedTips = listOf("Soru sormayı deneyin", "Ayarlardan API anahtarınızı tanımlayın")
            )
        }

        val prompt = """
            Soru/Arama Terimi: "$query"
            Kullanıcı aramasına destek olacak şekilde, bu konu hakkında bilgilendirici, pratik ve hızlı bir Türkçe yapay zeka arama özeti hazırla.
            
            Return ONLY a raw JSON with this format, do not add markdown code blocks or wrapping:
            {
               "summary": "Konunun detaylı ama derli toplu Türkçe açıklaması (maksimum 4-5 cümle).",
               "quickFacts": [
                  "1. önemli hap bilgi",
                  "2. önemli hap bilgi",
                  "3. önemli hap bilgi"
               ],
               "suggestedTips": [
                  "Arama önerisi tip 1",
                  "Arama önerisi tip 2"
               ]
            }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext AISummaryResult("Arama destek özeti alınamadı. (Hata kodu: ${response.code})")
                }
                val bodyString = response.body?.string() ?: ""
                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                val parts = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text") ?: ""

                val cleanJson = if (text.contains("```")) {
                    text.substringAfter("```json").substringBefore("```").trim()
                } else if (text.contains("```")) {
                    text.substringAfter("```").substringBefore("```").trim()
                } else {
                    text.trim()
                }

                val resultObj = JSONObject(cleanJson)
                val summary = resultObj.optString("summary")
                val quickFacts = mutableListOf<String>()
                val factsArray = resultObj.optJSONArray("quickFacts")
                if (factsArray != null) {
                    for (i in 0 until factsArray.length()) {
                        quickFacts.add(factsArray.getString(i))
                    }
                }
                val suggestedTips = mutableListOf<String>()
                val tipsArray = resultObj.optJSONArray("suggestedTips")
                if (tipsArray != null) {
                    for (i in 0 until tipsArray.length()) {
                        suggestedTips.add(tipsArray.getString(i))
                    }
                }

                AISummaryResult(summary, quickFacts, suggestedTips)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getSearchAssistantSummary", e)
            AISummaryResult("Yapay zeka asistanı şu anda müsait değil: ${e.localizedMessage}")
        }
    }
}

data class AISummaryResult(
    val summary: String,
    val quickFacts: List<String> = emptyList(),
    val suggestedTips: List<String> = emptyList()
)
