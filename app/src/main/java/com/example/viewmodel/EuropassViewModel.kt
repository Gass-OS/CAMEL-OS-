package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CvAiService
import com.example.data.CvRepository
import com.example.data.EuropassDatabase
import com.example.model.*
import com.example.util.CvPdfExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val TAG = "EuropassViewModel"

sealed interface AiGenerationState {
    data object Idle : AiGenerationState
    data class Streaming(val message: String) : AiGenerationState
    data class Loading(val message: String) : AiGenerationState
    data class Success(val message: String) : AiGenerationState
    data class Error(val message: String) : AiGenerationState
}

data class BuilderDraftState(
    val firstName: String = "",
    val lastName: String = "",
    val targetRole: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val nationality: String = "",
    val photoUri: String? = null,
    val narrative: String = "",
    val selectedTemplate: CvTemplateType = CvTemplateType.EUROPASS_CLASSIC
)

class EuropassViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CvRepository by lazy {
        val dao = EuropassDatabase.getInstance(application).cvDao()
        CvRepository(dao)
    }

    private val aiService = CvAiService()

    val savedCvs: StateFlow<List<EuropassCv>> = repository.allCvs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _builderDraft = MutableStateFlow(BuilderDraftState())
    val builderDraft: StateFlow<BuilderDraftState> = _builderDraft.asStateFlow()

    private val _activeCv = MutableStateFlow(createDefaultStarterCv())
    val activeCv: StateFlow<EuropassCv> = _activeCv.asStateFlow()

    private val _aiState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val aiState: StateFlow<AiGenerationState> = _aiState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun clearAiState() {
        _aiState.value = AiGenerationState.Idle
    }

    fun updateBuilderDraft(transform: (BuilderDraftState) -> BuilderDraftState) {
        _builderDraft.update(transform)
    }

    fun appendSpokenTextToNarrative(spoken: String) {
        val clean = spoken.trim()
        if (clean.isEmpty()) return
        _builderDraft.update { current ->
            val separator = if (current.narrative.isBlank()) "" else " "
            current.copy(narrative = current.narrative + separator + clean)
        }
    }

    fun continueProfileWithAi() {
        val draft = _builderDraft.value
        if (draft.firstName.isBlank() && draft.lastName.isBlank() && draft.narrative.isBlank()) {
            _aiState.value = AiGenerationState.Error("Please enter your name or speak/type a short description first.")
            return
        }

        viewModelScope.launch {
            _aiState.value = AiGenerationState.Streaming("AI is continuing your profile...")
            val baseText = draft.narrative.trim()
            val accumulated = StringBuilder()
            try {
                aiService.continueProfileDraftStream(
                    firstName = draft.firstName,
                    lastName = draft.lastName,
                    currentDraft = baseText,
                    targetRole = draft.targetRole
                ).collect { chunk ->
                    accumulated.append(chunk)
                    val combined = if (baseText.isBlank()) {
                        accumulated.toString()
                    } else {
                        "$baseText\n\n${accumulated}"
                    }
                    _builderDraft.update { it.copy(narrative = combined) }
                }
                _aiState.value = AiGenerationState.Success("Profile continued with AI! Review or generate your CV.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI continuation error", e)
                _aiState.value = AiGenerationState.Error(e.localizedMessage ?: "AI continuation failed.")
            }
        }
    }

    fun generateEuropassCvWithAi(onSuccessNavigateToCanvas: () -> Unit) {
        val draft = _builderDraft.value
        if (draft.firstName.isBlank() && draft.lastName.isBlank()) {
            _aiState.value = AiGenerationState.Error("Please enter your Name and Surname first.")
            return
        }
        if (draft.narrative.isBlank() && draft.targetRole.isBlank()) {
            _aiState.value = AiGenerationState.Error("Describe your profile or talk about your experience so AI can build your CV.")
            return
        }

        viewModelScope.launch {
            _aiState.value = AiGenerationState.Loading("Building your Europass Pro CV...")
            try {
                val generated = aiService.generateFullEuropassCv(
                    firstName = draft.firstName,
                    lastName = draft.lastName,
                    email = draft.email,
                    phone = draft.phone,
                    location = draft.location,
                    nationality = draft.nationality,
                    targetRole = draft.targetRole,
                    narrative = draft.narrative.ifBlank { "Target role: ${draft.targetRole}" },
                    template = draft.selectedTemplate,
                    photoUri = draft.photoUri
                )
                val saved = repository.saveCv(generated)
                _activeCv.value = saved
                _aiState.value = AiGenerationState.Success("Europass CV generated and saved locally!")
                onSuccessNavigateToCanvas()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "CV generation error", e)
                _aiState.value = AiGenerationState.Error(e.localizedMessage ?: "Failed to generate CV.")
            }
        }
    }

    fun enhanceSummaryWithAi(instruction: String) {
        val cv = _activeCv.value
        if (cv.summary.isBlank()) {
            _aiState.value = AiGenerationState.Error("Add a short summary first so AI can rewrite it.")
            return
        }
        viewModelScope.launch {
            _aiState.value = AiGenerationState.Loading("Polishing summary with AI...")
            try {
                val improved = aiService.enhanceSectionText("Profile Summary", cv.summary, instruction)
                updateActiveCv { it.copy(summary = improved) }
                _aiState.value = AiGenerationState.Success("Summary updated with AI!")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = AiGenerationState.Error(e.localizedMessage ?: "AI rewrite failed.")
            }
        }
    }

    fun enhanceExperienceWithAi(expId: String, instruction: String) {
        val exp = _activeCv.value.experiences.firstOrNull { it.id == expId } ?: return
        if (exp.description.isBlank() && exp.role.isBlank()) return
        viewModelScope.launch {
            _aiState.value = AiGenerationState.Loading("Polishing experience bullet points...")
            try {
                val input = "${exp.role} at ${exp.company}:\n${exp.description}"
                val improved = aiService.enhanceSectionText("Work Experience", input, instruction)
                updateActiveCv { cv ->
                    cv.copy(
                        experiences = cv.experiences.map {
                            if (it.id == expId) it.copy(description = improved) else it
                        }
                    )
                }
                _aiState.value = AiGenerationState.Success("Experience updated with AI!")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = AiGenerationState.Error(e.localizedMessage ?: "AI rewrite failed.")
            }
        }
    }

    fun updateActiveCv(transform: (EuropassCv) -> EuropassCv) {
        _activeCv.update { transform(it).copy(updatedAt = System.currentTimeMillis()) }
    }

    fun saveActiveCvLocally() {
        viewModelScope.launch {
            val saved = repository.saveCv(_activeCv.value)
            _activeCv.value = saved
            _snackbarMessage.value = "CV saved locally on device"
        }
    }

    fun loadCvIntoCanvas(cv: EuropassCv) {
        _activeCv.value = cv
    }

    fun createNewManualCv(template: CvTemplateType = CvTemplateType.EUROPASS_CLASSIC) {
        val draft = _builderDraft.value
        _activeCv.value = createDefaultStarterCv(template).copy(
            id = UUID.randomUUID().toString(),
            firstName = draft.firstName.ifBlank { "Elena" },
            lastName = draft.lastName.ifBlank { "Rossi" },
            email = draft.email.ifBlank { "elena.rossi@example.eu" },
            phone = draft.phone.ifBlank { "+32 2 555 0192" },
            location = draft.location.ifBlank { "Brussels, Belgium" },
            photoUri = draft.photoUri,
            showPhoto = true
        )
    }

    fun applyTemplate(template: CvTemplateType) {
        updateActiveCv {
            it.copy(
                templateId = template.id,
                accentColorHex = template.defaultAccentHex
            )
        }
        _builderDraft.update { it.copy(selectedTemplate = template) }
    }

    fun duplicateCv(cv: EuropassCv) {
        viewModelScope.launch {
            val copy = cv.copy(
                id = UUID.randomUUID().toString(),
                title = "${cv.title} (Copy)",
                updatedAt = System.currentTimeMillis()
            )
            repository.saveCv(copy)
            _snackbarMessage.value = "Duplicated '${cv.fullName}' CV"
        }
    }

    fun deleteCv(id: String) {
        viewModelScope.launch {
            repository.deleteCv(id)
            _snackbarMessage.value = "CV removed from local storage"
        }
    }

    fun moveSectionUp(sectionKey: String) {
        updateActiveCv { cv ->
            val list = cv.sectionOrder.toMutableList()
            val idx = list.indexOf(sectionKey)
            if (idx > 0) {
                list.removeAt(idx)
                list.add(idx - 1, sectionKey)
            }
            cv.copy(sectionOrder = list)
        }
    }

    fun moveSectionDown(sectionKey: String) {
        updateActiveCv { cv ->
            val list = cv.sectionOrder.toMutableList()
            val idx = list.indexOf(sectionKey)
            if (idx >= 0 && idx < list.size - 1) {
                list.removeAt(idx)
                list.add(idx + 1, sectionKey)
            }
            cv.copy(sectionOrder = list)
        }
    }

    fun exportAndSharePdf(cv: EuropassCv = _activeCv.value) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val pdfFile = withContext(Dispatchers.IO) {
                    repository.saveCv(cv)
                    CvPdfExporter.generatePdf(context, cv)
                }
                CvPdfExporter.sharePdf(context, pdfFile)
            } catch (e: Exception) {
                Log.e(TAG, "PDF share failed", e)
                _snackbarMessage.value = "Could not export PDF: ${e.localizedMessage}"
            }
        }
    }

    suspend fun generatePdfFile(cv: EuropassCv = _activeCv.value): File {
        val context = getApplication<Application>()
        return withContext(Dispatchers.IO) {
            repository.saveCv(cv)
            CvPdfExporter.generatePdf(context, cv)
        }
    }

    private fun createDefaultStarterCv(template: CvTemplateType = CvTemplateType.EUROPASS_CLASSIC): EuropassCv {
        return EuropassCv(
            id = UUID.randomUUID().toString(),
            title = "Europass Pro CV",
            firstName = "Elena",
            lastName = "Rossi",
            headline = "Product & Digital Systems Specialist",
            email = "elena.rossi@europass-mail.eu",
            phone = "+32 2 555 0148",
            location = "Brussels, Belgium",
            nationality = "Italian",
            showPhoto = true,
            summary = "Results-driven professional with 5+ years leading cross-functional European digital initiatives, streamlining operations, and delivering accessible user-centric products.",
            experiences = listOf(
                WorkExperience(
                    role = "Senior Product Coordinator",
                    company = "EuroTech Solutions",
                    location = "Brussels",
                    startDate = "03/2023",
                    endDate = "Present",
                    description = "• Directed cross-border product roadmaps across 4 EU teams, boosting delivery velocity by 28%.\n• Standardized documentation and onboarding workflows aligned with EU accessibility directives."
                )
            ),
            education = listOf(
                EducationItem(
                    degree = "M.Sc. in Digital Innovation & Management",
                    institution = "Politecnico di Milano",
                    location = "Milan, Italy",
                    startDate = "2018",
                    endDate = "2020",
                    description = "Graduated cum laude; thesis on European interoperable digital credentials."
                )
            ),
            skills = listOf(
                SkillItem(name = "Project Management", level = 5, category = "Leadership"),
                SkillItem(name = "Product Strategy", level = 5, category = "Core"),
                SkillItem(name = "Data Analytics", level = 4, category = "Technical"),
                SkillItem(name = "UX Research", level = 4, category = "Design")
            ),
            languages = listOf(
                LanguageItem(language = "Italian", cefrLevel = "Native"),
                LanguageItem(language = "English", cefrLevel = "C2"),
                LanguageItem(language = "French", cefrLevel = "B2")
            ),
            certifications = listOf("PRINCE2 Practitioner", "European Digital Competence (DigComp 2.2)"),
            templateId = template.id,
            accentColorHex = template.defaultAccentHex
        )
    }
}
