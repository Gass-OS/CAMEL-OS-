package com.example.ai

import android.util.Log
import com.example.model.CvTemplateType
import com.example.model.EducationItem
import com.example.model.EuropassCv
import com.example.model.LanguageItem
import com.example.model.SkillItem
import com.example.model.WorkExperience
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val TAG = "CvAiService"

class CvAiService {

    private val continuationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    private val cvJsonSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Document title such as Senior Software Engineer CV"),
            "headline" to Schema.string(description = "Professional job title / headline for under the candidate name"),
            "summary" to Schema.string(description = "Compelling 3-4 sentence Europass professional summary"),
            "experiences" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "role" to Schema.string(),
                        "company" to Schema.string(),
                        "location" to Schema.string(),
                        "startDate" to Schema.string(),
                        "endDate" to Schema.string(),
                        "description" to Schema.string(description = "Concise impact-oriented bullet points separated by newlines")
                    )
                ),
                description = "Work experience entries in reverse chronological order"
            ),
            "education" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "degree" to Schema.string(),
                        "institution" to Schema.string(),
                        "location" to Schema.string(),
                        "startDate" to Schema.string(),
                        "endDate" to Schema.string(),
                        "description" to Schema.string()
                    )
                ),
                description = "Education and academic qualifications"
            ),
            "skills" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(),
                        "level" to Schema.integer(description = "Proficiency from 1 to 5"),
                        "category" to Schema.string(description = "Category like Technical, Leadership, Tools")
                    )
                ),
                description = "Top 6 to 10 relevant skills"
            ),
            "languages" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "language" to Schema.string(),
                        "cefrLevel" to Schema.string(description = "CEFR level: A1, A2, B1, B2, C1, C2, or Native")
                    )
                ),
                description = "Spoken languages with Europass CEFR rating"
            ),
            "certifications" to Schema.array(
                items = Schema.string(),
                description = "Relevant certifications, awards, or licenses"
            )
        )
    )

    private val structuredCvModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = cvJsonSchema
            }
        )
    }

    fun continueProfileDraftStream(
        firstName: String,
        lastName: String,
        currentDraft: String,
        targetRole: String
    ): Flow<String> = flow {
        val fullName = listOf(firstName.trim(), lastName.trim()).filter { it.isNotEmpty() }.joinToString(" ")
            .ifEmpty { "the candidate" }
        val prompt = buildString {
            appendLine("You are an expert European HR & Europass CV writer.")
            appendLine("The user ($fullName) started describing their profile and experience by talking or typing:")
            if (targetRole.isNotBlank()) {
                appendLine("Target Role: $targetRole")
            }
            appendLine("--- USER NOTES ---")
            appendLine(currentDraft.ifBlank { "I am looking for a professional position and want to build a strong Europass CV." })
            appendLine("--- END NOTES ---")
            appendLine()
            appendLine("Continue and expand their draft naturally into a polished, complete career story ready for a Europass CV.")
            appendLine("Preserve all real facts they mentioned, smoothly fill in professional framing, suggest realistic action-verb responsibilities, highlight skills, education, and languages if implied, and keep the tone clear, concise, and authentic. Output plain text paragraphs/bullets only (no markdown fences).")
        }

        try {
            withTimeout(60_000L) {
                continuationModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        emit(piece)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Profile continuation timed out", e)
            throw IllegalStateException("AI continuation timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Profile continuation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to continue profile with AI.")
        }
    }

    suspend fun generateFullEuropassCv(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        location: String,
        nationality: String,
        targetRole: String,
        narrative: String,
        template: CvTemplateType,
        photoUri: String?
    ): EuropassCv {
        val prompt = buildString {
            appendLine("Generate a complete, ATS-optimized, professional Europass CV in JSON format based strictly on the candidate's details and narrative.")
            appendLine("Candidate Name: $firstName $lastName")
            if (targetRole.isNotBlank()) appendLine("Target Role: $targetRole")
            if (location.isNotBlank()) appendLine("Location: $location")
            if (nationality.isNotBlank()) appendLine("Nationality: $nationality")
            appendLine()
            appendLine("Candidate Spoken/Typed Profile & Experience:")
            appendLine(narrative)
            appendLine()
            appendLine("Instructions:")
            appendLine("- Write in the same language as the candidate's narrative (default English if unclear).")
            appendLine("- Convert rough experience into structured work history with crisp bullet points using strong action verbs.")
            appendLine("- Include Europass CEFR language ratings (A1-C2 or Native) for any languages mentioned or implied.")
            appendLine("- Include 6-8 categorized skills rated 1-5.")
            appendLine("- If education or certifications aren't explicitly detailed, infer only reasonable baseline items clearly aligned with their stated field, or keep minimal.")
        }

        try {
            val response = withTimeout(60_000L) {
                structuredCvModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("AI returned an empty CV response.")
            val obj = JSONObject(rawJson)

            val expArr = obj.optJSONArray("experiences") ?: JSONArray()
            val experiences = (0 until expArr.length()).map { i ->
                val e = expArr.getJSONObject(i)
                WorkExperience(
                    id = UUID.randomUUID().toString(),
                    role = e.optString("role"),
                    company = e.optString("company"),
                    location = e.optString("location"),
                    startDate = e.optString("startDate"),
                    endDate = e.optString("endDate", "Present"),
                    description = e.optString("description")
                )
            }

            val eduArr = obj.optJSONArray("education") ?: JSONArray()
            val education = (0 until eduArr.length()).map { i ->
                val e = eduArr.getJSONObject(i)
                EducationItem(
                    id = UUID.randomUUID().toString(),
                    degree = e.optString("degree"),
                    institution = e.optString("institution"),
                    location = e.optString("location"),
                    startDate = e.optString("startDate"),
                    endDate = e.optString("endDate"),
                    description = e.optString("description")
                )
            }

            val skillArr = obj.optJSONArray("skills") ?: JSONArray()
            val skills = (0 until skillArr.length()).map { i ->
                val s = skillArr.getJSONObject(i)
                SkillItem(
                    id = UUID.randomUUID().toString(),
                    name = s.optString("name"),
                    level = s.optInt("level", 4).coerceIn(1, 5),
                    category = s.optString("category", "Core")
                )
            }

            val langArr = obj.optJSONArray("languages") ?: JSONArray()
            val languages = (0 until langArr.length()).map { i ->
                val l = langArr.getJSONObject(i)
                LanguageItem(
                    id = UUID.randomUUID().toString(),
                    language = l.optString("language"),
                    cefrLevel = l.optString("cefrLevel", "B2")
                )
            }

            val certArr = obj.optJSONArray("certifications") ?: JSONArray()
            val certifications = (0 until certArr.length()).mapNotNull { i ->
                certArr.optString(i).takeIf { it.isNotBlank() }
            }

            return EuropassCv(
                id = UUID.randomUUID().toString(),
                title = obj.optString("title").ifBlank { "$firstName $lastName - Europass CV" },
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                headline = obj.optString("headline").ifBlank { targetRole },
                email = email.trim(),
                phone = phone.trim(),
                location = location.trim(),
                nationality = nationality.trim(),
                photoUri = photoUri,
                showPhoto = photoUri != null,
                summary = obj.optString("summary"),
                experiences = experiences,
                education = education,
                skills = skills,
                languages = languages,
                certifications = certifications,
                templateId = template.id,
                accentColorHex = template.defaultAccentHex
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Full CV generation timed out", e)
            throw IllegalStateException("CV generation timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Full CV generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to generate Europass CV.")
        }
    }

    suspend fun enhanceSectionText(
        sectionLabel: String,
        currentText: String,
        instruction: String
    ): String {
        val prompt = buildString {
            appendLine("Rewrite and improve the following Europass CV '$sectionLabel' section.")
            appendLine("Instruction: $instruction")
            appendLine("Keep it professional, concise, impact-oriented, and ready to paste directly into a CV. Return plain text only without quotes or markdown wrappers.")
            appendLine()
            appendLine("Current text:")
            appendLine(currentText)
        }

        try {
            val response = withTimeout(60_000L) {
                continuationModel.generateContent(prompt)
            }
            val result = response.text?.trim()
            if (result.isNullOrBlank()) {
                throw IllegalStateException("AI returned empty text.")
            }
            return result
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Section enhancement timed out", e)
            throw IllegalStateException("AI rewrite timed out. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Section enhancement failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to enhance section.")
        }
    }
}
