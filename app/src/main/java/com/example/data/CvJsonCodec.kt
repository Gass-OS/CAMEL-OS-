package com.example.data

import com.example.model.CvSectionType
import com.example.model.EducationItem
import com.example.model.EuropassCv
import com.example.model.LanguageItem
import com.example.model.SkillItem
import com.example.model.WorkExperience
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object CvJsonCodec {

    fun toJson(cv: EuropassCv): String {
        val root = JSONObject().apply {
            put("id", cv.id)
            put("title", cv.title)
            put("firstName", cv.firstName)
            put("lastName", cv.lastName)
            put("headline", cv.headline)
            put("email", cv.email)
            put("phone", cv.phone)
            put("location", cv.location)
            put("website", cv.website)
            put("nationality", cv.nationality)
            put("photoUri", cv.photoUri ?: JSONObject.NULL)
            put("showPhoto", cv.showPhoto)
            put("summary", cv.summary)
            put("templateId", cv.templateId)
            put("accentColorHex", cv.accentColorHex)
            put("fontScale", cv.fontScale.toDouble())
            put("updatedAt", cv.updatedAt)

            put("experiences", JSONArray().apply {
                cv.experiences.forEach { exp ->
                    put(JSONObject().apply {
                        put("id", exp.id)
                        put("role", exp.role)
                        put("company", exp.company)
                        put("location", exp.location)
                        put("startDate", exp.startDate)
                        put("endDate", exp.endDate)
                        put("description", exp.description)
                    })
                }
            })

            put("education", JSONArray().apply {
                cv.education.forEach { edu ->
                    put(JSONObject().apply {
                        put("id", edu.id)
                        put("degree", edu.degree)
                        put("institution", edu.institution)
                        put("location", edu.location)
                        put("startDate", edu.startDate)
                        put("endDate", edu.endDate)
                        put("description", edu.description)
                    })
                }
            })

            put("skills", JSONArray().apply {
                cv.skills.forEach { skill ->
                    put(JSONObject().apply {
                        put("id", skill.id)
                        put("name", skill.name)
                        put("level", skill.level)
                        put("category", skill.category)
                    })
                }
            })

            put("languages", JSONArray().apply {
                cv.languages.forEach { lang ->
                    put(JSONObject().apply {
                        put("id", lang.id)
                        put("language", lang.language)
                        put("cefrLevel", lang.cefrLevel)
                    })
                }
            })

            put("certifications", JSONArray().apply {
                cv.certifications.forEach { put(it) }
            })

            put("sectionOrder", JSONArray().apply {
                cv.sectionOrder.forEach { put(it) }
            })
        }
        return root.toString()
    }

    fun fromJson(jsonString: String): EuropassCv {
        val obj = JSONObject(jsonString)

        val expArray = obj.optJSONArray("experiences") ?: JSONArray()
        val experiences = (0 until expArray.length()).map { i ->
            val e = expArray.getJSONObject(i)
            WorkExperience(
                id = e.optString("id").ifBlank { UUID.randomUUID().toString() },
                role = e.optString("role"),
                company = e.optString("company"),
                location = e.optString("location"),
                startDate = e.optString("startDate"),
                endDate = e.optString("endDate", "Present"),
                description = e.optString("description")
            )
        }

        val eduArray = obj.optJSONArray("education") ?: JSONArray()
        val education = (0 until eduArray.length()).map { i ->
            val e = eduArray.getJSONObject(i)
            EducationItem(
                id = e.optString("id").ifBlank { UUID.randomUUID().toString() },
                degree = e.optString("degree"),
                institution = e.optString("institution"),
                location = e.optString("location"),
                startDate = e.optString("startDate"),
                endDate = e.optString("endDate"),
                description = e.optString("description")
            )
        }

        val skillArray = obj.optJSONArray("skills") ?: JSONArray()
        val skills = (0 until skillArray.length()).map { i ->
            val s = skillArray.getJSONObject(i)
            SkillItem(
                id = s.optString("id").ifBlank { UUID.randomUUID().toString() },
                name = s.optString("name"),
                level = s.optInt("level", 4).coerceIn(1, 5),
                category = s.optString("category", "Core")
            )
        }

        val langArray = obj.optJSONArray("languages") ?: JSONArray()
        val languages = (0 until langArray.length()).map { i ->
            val l = langArray.getJSONObject(i)
            LanguageItem(
                id = l.optString("id").ifBlank { UUID.randomUUID().toString() },
                language = l.optString("language"),
                cefrLevel = l.optString("cefrLevel", "B2")
            )
        }

        val certArray = obj.optJSONArray("certifications") ?: JSONArray()
        val certifications = (0 until certArray.length()).map { certArray.getString(it) }

        val orderArray = obj.optJSONArray("sectionOrder")
        val sectionOrder = if (orderArray != null && orderArray.length() > 0) {
            (0 until orderArray.length()).map { orderArray.getString(it) }
        } else {
            CvSectionType.defaultOrder
        }

        val photoRaw = if (obj.isNull("photoUri")) null else obj.optString("photoUri").takeIf { it.isNotBlank() }

        return EuropassCv(
            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
            title = obj.optString("title", "Europass Curriculum Vitae"),
            firstName = obj.optString("firstName"),
            lastName = obj.optString("lastName"),
            headline = obj.optString("headline"),
            email = obj.optString("email"),
            phone = obj.optString("phone"),
            location = obj.optString("location"),
            website = obj.optString("website"),
            nationality = obj.optString("nationality"),
            photoUri = photoRaw,
            showPhoto = obj.optBoolean("showPhoto", true),
            summary = obj.optString("summary"),
            experiences = experiences,
            education = education,
            skills = skills,
            languages = languages,
            certifications = certifications,
            templateId = obj.optString("templateId", "europass_classic"),
            accentColorHex = obj.optLong("accentColorHex", 0xFF0D2B6B),
            fontScale = obj.optDouble("fontScale", 1.0).toFloat(),
            sectionOrder = sectionOrder,
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}
