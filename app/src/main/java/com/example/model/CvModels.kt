package com.example.model

import java.util.UUID

enum class CvTemplateType(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val defaultAccentHex: Long,
    val badgeLabel: String
) {
    EUROPASS_CLASSIC(
        id = "europass_classic",
        displayName = "Europass Official",
        subtitle = "EU royal navy header, structured grid & CEFR badges",
        defaultAccentHex = 0xFF0D2B6B,
        badgeLabel = "EU Standard"
    ),
    NORDIC_MINIMAL(
        id = "nordic_minimal",
        displayName = "Nordic Editorial",
        subtitle = "Playfair serif typography, airy margins & clean rules",
        defaultAccentHex = 0xFF1E293B,
        badgeLabel = "Minimal"
    ),
    EXECUTIVE_SLATE(
        id = "executive_slate",
        displayName = "Executive Gold",
        subtitle = "High-contrast dark header with champagne gold accents",
        defaultAccentHex = 0xFFB45309,
        badgeLabel = "Leadership"
    ),
    TECH_MONO(
        id = "tech_mono",
        displayName = "Tech Engineer",
        subtitle = "Monospace metadata tags, crisp stack & project focus",
        defaultAccentHex = 0xFF0F766E,
        badgeLabel = "Developer"
    ),
    CREATIVE_STUDIO(
        id = "creative_studio",
        displayName = "Creative Studio",
        subtitle = "Asymmetric left sidebar with modern color pills",
        defaultAccentHex = 0xFF4338CA,
        badgeLabel = "Modern"
    );

    companion object {
        fun fromId(id: String): CvTemplateType =
            entries.firstOrNull { it.id == id } ?: EUROPASS_CLASSIC
    }
}

enum class CvSectionType(val key: String, val label: String) {
    SUMMARY("summary", "About Me & Profile"),
    EXPERIENCE("experience", "Work Experience"),
    EDUCATION("education", "Education & Training"),
    SKILLS("skills", "Digital & Core Skills"),
    LANGUAGES("languages", "Language Skills (CEFR)"),
    CERTIFICATIONS("certifications", "Certifications & Honors");

    companion object {
        val defaultOrder: List<String> = entries.map { it.key }
        fun fromKey(key: String): CvSectionType? = entries.firstOrNull { it.key == key }
    }
}

data class WorkExperience(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val company: String,
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "Present",
    val description: String = ""
)

data class EducationItem(
    val id: String = UUID.randomUUID().toString(),
    val degree: String,
    val institution: String,
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = ""
)

data class SkillItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val level: Int = 4, // 1..5
    val category: String = "Core"
)

data class LanguageItem(
    val id: String = UUID.randomUUID().toString(),
    val language: String,
    val cefrLevel: String = "B2" // A1, A2, B1, B2, C1, C2, Native
)

data class EuropassCv(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Europass Curriculum Vitae",
    val firstName: String = "",
    val lastName: String = "",
    val headline: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val website: String = "",
    val nationality: String = "",
    val photoUri: String? = null,
    val showPhoto: Boolean = true,
    val summary: String = "",
    val experiences: List<WorkExperience> = emptyList(),
    val education: List<EducationItem> = emptyList(),
    val skills: List<SkillItem> = emptyList(),
    val languages: List<LanguageItem> = emptyList(),
    val certifications: List<String> = emptyList(),
    val templateId: String = CvTemplateType.EUROPASS_CLASSIC.id,
    val accentColorHex: Long = CvTemplateType.EUROPASS_CLASSIC.defaultAccentHex,
    val fontScale: Float = 1.0f,
    val sectionOrder: List<String> = CvSectionType.defaultOrder,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val fullName: String
        get() = listOf(firstName.trim(), lastName.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifEmpty { "Your Name" }
}

data class CareerResource(
    val title: String,
    val provider: String,
    val description: String,
    val url: String,
    val category: String
)

val FreeCareerResources = listOf(
    CareerResource(
        title = "Official EU Europass Standards",
        provider = "European Union",
        description = "Official European Commission guidelines on CV structure, digital credentials, and mobility across 30+ European countries.",
        url = "https://europa.eu/europass",
        category = "EU Standard"
    ),
    CareerResource(
        title = "CEFR Language Self-Assessment Grid",
        provider = "Council of Europe",
        description = "Standardized A1 to C2 proficiency descriptors for Listening, Reading, Speaking, and Writing on Europass CVs.",
        url = "https://europa.eu/europass/en/common-european-framework-reference-language-skills",
        category = "Languages"
    ),
    CareerResource(
        title = "ESCO Skills & Occupations Taxonomy",
        provider = "European Commission",
        description = "Free multilingual classification of 13,000+ European skills and job titles to boost ATS keyword matching.",
        url = "https://esco.ec.europa.eu",
        category = "ATS Keywords"
    ),
    CareerResource(
        title = "EURES European Job Mobility Portal",
        provider = "EURES Network",
        description = "Search verified free job openings across Europe and match directly with Europass-compatible profiles.",
        url = "https://eures.europa.eu",
        category = "Jobs"
    )
)

val AccentPaletteOptions: List<Pair<String, Long>> = listOf(
    "EU Navy" to 0xFF0D2B6B,
    "Cobalt" to 0xFF1D4ED8,
    "Emerald" to 0xFF0F766E,
    "Champagne Gold" to 0xFFB45309,
    "Royal Indigo" to 0xFF4338CA,
    "Burgundy" to 0xFF9F1239,
    "Obsidian" to 0xFF1E293B
)
