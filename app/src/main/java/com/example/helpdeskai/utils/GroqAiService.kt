package com.example.helpdeskai.utils

import android.os.Handler
import android.os.Looper
import com.example.helpdeskai.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * HelpDesk AI — Powered by Groq LLM (openai/gpt-oss-20b)
 *
 * ALL intelligence comes from the Groq Large Language Model.
 * No if-else keyword matching. No hardcoded rules.
 *
 * Features:
 * 1. processStudentComplaint() — LLM auto-categorizes + writes professional structured description
 * 2. generateAdminReply()     — LLM writes formal official administrative response
 *
 * Groq returns structured JSON (response_format: json_object) every time.
 */
object GroqAiService {

    private val GROQ_API_KEY  = BuildConfig.GROQ_API_KEY
    private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    private const val GROQ_MODEL    = "openai/gpt-oss-20b"

    private const val CONNECT_TIMEOUT = 15_000   // 15 sec
    private const val READ_TIMEOUT    = 30_000   // 30 sec — LLM needs time

    private val executor    = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Result from Groq for a student complaint */
    data class GroqComplaintResult(
        val category:            String,  // LLM-chosen category
        val urgency:             String,  // High / Medium / Low — decided by LLM
        val polishedDescription: String,  // LLM-written professional description
        val resolutionSuggestion:String,  // LLM-suggested action
        val confidence:          Int      // LLM-reported confidence %
    )

    // ─────────────────────────────────────────────────────────────────
    //  STUDENT SIDE — Auto-Categorize + Structured Description via LLM
    // ─────────────────────────────────────────────────────────────────

    /**
     * Sends student's raw complaint text to Groq LLM.
     *
     * The LLM:
     *   • Understands the complaint semantically (not keyword matching)
     *   • Picks the correct department category from the provided list
     *   • Rewrites the complaint into a professional structured format
     *   • Assesses urgency based on context
     *   • Suggests a resolution step for administration
     *
     * Returns structured JSON → parsed into GroqComplaintResult.
     * On network/API failure → calls onError with a plain error message.
     */
    fun processStudentComplaint(
        title:          String,
        rawDescription: String,
        onSuccess:      (GroqComplaintResult) -> Unit,
        onError:        (errorMessage: String) -> Unit
    ) {
        executor.execute {
            try {
                val systemPrompt = """
You are HelpDesk AI, the intelligent complaint assistant for a college campus.

A student has submitted a complaint. Your job is to:
1. Understand the complaint semantically and decide the correct department category.
2. Rewrite the description into a structured, professional format in the FIRST PERSON from the student's perspective (e.g., "I am writing to report...", "I noticed that..."). NEVER write "The student reports" or in the third person.
3. Assess urgency based on the context (health/safety = High, daily inconvenience = Medium, minor = Low).
4. Suggest what action the support team should take.

You MUST respond ONLY with a valid JSON object — no extra text, no markdown, no explanation.

The JSON must have exactly these keys:
{
  "category": "<choose ONE: Hostel & Mess | Scholarship & Fees | Academics & Exams | Campus Infrastructure | Placement & Training | Administration | Other>",
  "urgency": "<choose ONE: High | Medium | Low>",
  "polished_description": "<2-3 clear, professional sentences written strictly in the FIRST PERSON from the student's own voice (\"I am facing...\", \"I noticed...\").>",
  "resolution_suggestion": "<One specific action the support team should take to resolve this complaint.>",
  "confidence": <integer between 85 and 99>
}
                """.trimIndent()

                val userMessage = "Title: $title\n\nStudent's description: $rawDescription"

                val llmJson = callGroqLlm(systemPrompt, userMessage)
                val parsed  = JSONObject(llmJson)

                val result = GroqComplaintResult(
                    category             = sanitizeCategory(parsed.optString("category", "Other")),
                    urgency              = sanitizeUrgency(parsed.optString("urgency", "Medium")),
                    polishedDescription  = parsed.optString("polished_description", rawDescription),
                    resolutionSuggestion = parsed.optString("resolution_suggestion", ""),
                    confidence           = parsed.optInt("confidence", 92).coerceIn(50, 99)
                )

                mainHandler.post { onSuccess(result) }

            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post {
                    onError("Could not reach Groq AI. Please check your internet connection and try again.\n\nError: ${e.message?.take(80)}")
                }
            }
        }
    }

    /**
     * Backward-compatible alias for enhanceDescription
     */
    fun enhanceDescription(
        rawDescription: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        processStudentComplaint(
            title = "Complaint",
            rawDescription = rawDescription,
            onSuccess = { result ->
                onSuccess(result.polishedDescription)
            },
            onError = onError
        )
    }

    // ─────────────────────────────────────────────────────────────────
    //  ADMIN SIDE — Auto-Generate Formal Official Reply via LLM
    // ─────────────────────────────────────────────────────────────────

    /**
     * Admin taps "✨ Groq AI Draft" on a complaint.
     * Groq LLM writes a formal, structured administrative resolution reply.
     *
     * The reply:
     *   • Acknowledges the specific complaint
     *   • Names the department being notified
     *   • Gives an estimated resolution timeline
     *   • Is formal, empathetic, professional
     *
     * Returns structured JSON → parsed to extract official_reply string.
     * On failure → calls onError with a plain error message.
     */
    fun generateAdminReply(
        complaintTitle:       String,
        complaintDescription: String,
        complaintCategory:    String,
        onSuccess:            (officialReply: String) -> Unit,
        onError:              (errorMessage: String) -> Unit
    ) {
        executor.execute {
            try {
                val systemPrompt = """
You are the Support Team of a college campus.

A student has submitted a complaint and you must write a formal administrative reply.

Your reply must:
1. Acknowledge the specific issue raised by the student.
2. State that the relevant department has been notified.
3. Give a realistic estimated resolution timeline (e.g. 2-3 working days, 48 hours, etc.).
4. Be formal, polite, and reassuring.

You MUST respond ONLY with a valid JSON object — no extra text, no markdown, no explanation.

The JSON must have exactly this key:
{
  "official_reply": "<2-3 formal sentences. Mention the category/department. Give a timeline. Empathetic but professional tone.>"
}
                """.trimIndent()

                val userMessage = """
Complaint Title: $complaintTitle
Department/Category: $complaintCategory
Student's Description: $complaintDescription
                """.trimIndent()

                val llmJson = callGroqLlm(systemPrompt, userMessage)
                val parsed  = JSONObject(llmJson)

                val reply = parsed.optString(
                    "official_reply",
                    "Your complaint regarding \"$complaintTitle\" has been received and registered. The $complaintCategory department has been notified and will address the issue within 2–3 working days."
                )

                mainHandler.post { onSuccess(reply) }

            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post {
                    onError("Could not reach Groq AI. Please check internet and try again.\n\nError: ${e.message?.take(80)}")
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  CORE: HTTP call to Groq API
    // ─────────────────────────────────────────────────────────────────

    /**
     * Makes an HTTPS POST to Groq's chat completions endpoint.
     * Uses response_format: json_object to force structured JSON output from the LLM.
     * Returns the raw content string from the LLM (the JSON it generated).
     */
    private fun callGroqLlm(systemPrompt: String, userMessage: String): String {
        val requestBody = JSONObject().apply {
            put("model", GROQ_MODEL)
            put("response_format", JSONObject().apply { put("type", "json_object") })
            put("temperature", 0.2)       // Low = consistent, structured output
            put("max_tokens", 600)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
            })
        }

        val url  = URL(GROQ_ENDPOINT)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $GROQ_API_KEY")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            connectTimeout = CONNECT_TIMEOUT
            readTimeout    = READ_TIMEOUT
            doOutput       = true
            doInput        = true
        }

        try {
            val bodyBytes = requestBody.toString().toByteArray(Charsets.UTF_8)
            conn.outputStream.use { it.write(bodyBytes) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val groqWrapper  = JSONObject(responseText)
                // Extract the LLM's actual content (the JSON the model wrote)
                return groqWrapper
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } else {
                val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
                throw RuntimeException("Groq returned HTTP $responseCode: $errorText")
            }
        } finally {
            conn.disconnect()
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  SAFETY HELPERS — just validate LLM output, NOT keyword matching
    // ─────────────────────────────────────────────────────────────────

    private val VALID_CATEGORIES = listOf(
        "Hostel & Mess",
        "Scholarship & Fees",
        "Academics & Exams",
        "Campus Infrastructure",
        "Placement & Training",
        "Administration",
        "Other"
    )

    /** Ensures the LLM-returned category is one of our valid options */
    private fun sanitizeCategory(llmValue: String): String {
        val trimmed = llmValue.trim()
        // Exact match first
        VALID_CATEGORIES.firstOrNull { it.equals(trimmed, ignoreCase = true) }?.let { return it }
        // Partial match as safety net (in case LLM added extra words)
        VALID_CATEGORIES.firstOrNull { trimmed.contains(it, ignoreCase = true) }?.let { return it }
        // If nothing matches, trust "Other"
        return "Other"
    }

    /** Ensures urgency is exactly one of the three valid values */
    private fun sanitizeUrgency(llmValue: String): String {
        return when (llmValue.trim().lowercase()) {
            "high"   -> "High"
            "low"    -> "Low"
            else     -> "Medium"
        }
    }
}
