package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderEditorSheet(
    cv: EuropassCv,
    onDismiss: () -> Unit,
    onOpenPhotoPicker: () -> Unit,
    onSaveHeader: (
        firstName: String,
        lastName: String,
        headline: String,
        email: String,
        phone: String,
        location: String,
        nationality: String,
        showPhoto: Boolean
    ) -> Unit
) {
    var firstName by remember(cv) { mutableStateOf(cv.firstName) }
    var lastName by remember(cv) { mutableStateOf(cv.lastName) }
    var headline by remember(cv) { mutableStateOf(cv.headline) }
    var email by remember(cv) { mutableStateOf(cv.email) }
    var phone by remember(cv) { mutableStateOf(cv.phone) }
    var location by remember(cv) { mutableStateOf(cv.location) }
    var nationality by remember(cv) { mutableStateOf(cv.nationality) }
    var showPhoto by remember(cv) { mutableStateOf(cv.showPhoto) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Edit Profile Header", style = MaterialTheme.typography.titleLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show Photo on CV")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = showPhoto, onCheckedChange = { showPhoto = it })
                }
                FilledTonalButton(onClick = onOpenPhotoPicker) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Replace Photo")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Name") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Surname") },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = headline,
                onValueChange = { headline = it },
                label = { Text("Professional Headline") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("City, Country") },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = nationality,
                onValueChange = { nationality = it },
                label = { Text("Nationality") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSaveHeader(firstName, lastName, headline, email, phone, location, nationality, showPhoto)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_header_sheet_button")
            ) {
                Text("Apply Header Changes")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignStudioSheet(
    cv: EuropassCv,
    onDismiss: () -> Unit,
    onSelectTemplate: (CvTemplateType) -> Unit,
    onSelectColor: (Long) -> Unit,
    onFontScaleChanged: (Float) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Canva Design & Theme", style = MaterialTheme.typography.titleLarge)

            Text("Model", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CvTemplateType.entries) { tpl ->
                    val active = cv.templateId == tpl.id
                    FilterChip(
                        selected = active,
                        onClick = { onSelectTemplate(tpl) },
                        label = { Text(tpl.displayName) }
                    )
                }
            }

            Text("Accent Color", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentPaletteOptions.forEach { (_, hex) ->
                    val selected = cv.accentColorHex == hex
                    Surface(
                        onClick = { onSelectColor(hex) },
                        shape = CircleShape,
                        color = Color(hex),
                        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.White),
                        modifier = Modifier.size(34.dp)
                    ) {}
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Typography Scale", style = MaterialTheme.typography.labelLarge)
                    Text("${(cv.fontScale * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = cv.fontScale,
                    onValueChange = onFontScaleChanged,
                    valueRange = 0.85f..1.2f
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Done")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionOrderSheet(
    cv: EuropassCv,
    onDismiss: () -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Reorder CV Sections", style = MaterialTheme.typography.titleLarge)
            Text(
                "Arrange sections just like a Canva layout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cv.sectionOrder.indices.toList()) { idx ->
                    val key = cv.sectionOrder[idx]
                    val section = CvSectionType.fromKey(key) ?: return@items
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(section.label, fontWeight = FontWeight.Medium)
                            Row {
                                IconButton(
                                    onClick = { onMoveUp(key) },
                                    enabled = idx > 0
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                                }
                                IconButton(
                                    onClick = { onMoveDown(key) },
                                    enabled = idx < cv.sectionOrder.lastIndex
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionContentEditorSheet(
    section: CvSectionType,
    cv: EuropassCv,
    isAiBusy: Boolean,
    onDismiss: () -> Unit,
    onUpdateCv: ((EuropassCv) -> EuropassCv) -> Unit,
    onAiEnhanceSummary: (String) -> Unit,
    onAiEnhanceExperience: (String, String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Edit ${section.label}", style = MaterialTheme.typography.titleLarge)

            when (section) {
                CvSectionType.SUMMARY -> {
                    var summaryText by remember(cv.summary) { mutableStateOf(cv.summary) }
                    OutlinedTextField(
                        value = summaryText,
                        onValueChange = {
                            summaryText = it
                            onUpdateCv { current -> current.copy(summary = it) }
                        },
                        label = { Text("Profile Summary") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { onAiEnhanceSummary("Make it punchy, executive, and ATS-ready in 3 sentences.") },
                            enabled = !isAiBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Polish")
                        }
                        FilledTonalButton(
                            onClick = { onAiEnhanceSummary("Expand slightly with quantifiable achievements.") },
                            enabled = !isAiBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("AI Impact+")
                        }
                    }
                }

                CvSectionType.EXPERIENCE -> {
                    var newRole by remember { mutableStateOf("") }
                    var newCompany by remember { mutableStateOf("") }
                    var newDates by remember { mutableStateOf("") }
                    var newDesc by remember { mutableStateOf("") }

                    cv.experiences.forEach { exp ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${exp.role} @ ${exp.company}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            onUpdateCv { c -> c.copy(experiences = c.experiences.filterNot { it.id == exp.id }) }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete role", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                OutlinedTextField(
                                    value = exp.description,
                                    onValueChange = { updated ->
                                        onUpdateCv { c ->
                                            c.copy(experiences = c.experiences.map {
                                                if (it.id == exp.id) it.copy(description = updated) else it
                                            })
                                        }
                                    },
                                    label = { Text("Responsibilities & Impact") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                TextButton(
                                    onClick = { onAiEnhanceExperience(exp.id, "Rewrite into 3 crisp bullet points starting with strong action verbs.") },
                                    enabled = !isAiBusy
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Rewrite with AI")
                                }
                            }
                        }
                    }

                    HorizontalDivider()
                    Text("Add Work Experience", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = newRole, onValueChange = { newRole = it }, label = { Text("Job Title") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = newCompany, onValueChange = { newCompany = it }, label = { Text("Company") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = newDates, onValueChange = { newDates = it }, label = { Text("Dates (e.g. 2022-2025)") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            if (newRole.isNotBlank()) {
                                val parts = newDates.split("-", "–").map { it.trim() }
                                onUpdateCv { c ->
                                    c.copy(
                                        experiences = listOf(
                                            WorkExperience(
                                                id = UUID.randomUUID().toString(),
                                                role = newRole.trim(),
                                                company = newCompany.trim(),
                                                startDate = parts.getOrNull(0).orEmpty(),
                                                endDate = parts.getOrNull(1) ?: "Present",
                                                description = newDesc.trim()
                                            )
                                        ) + c.experiences
                                    )
                                }
                                newRole = ""; newCompany = ""; newDates = ""; newDesc = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Experience")
                    }
                }

                CvSectionType.EDUCATION -> {
                    var degree by remember { mutableStateOf("") }
                    var school by remember { mutableStateOf("") }
                    var years by remember { mutableStateOf("") }

                    cv.education.forEach { edu ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(edu.degree, fontWeight = FontWeight.Bold)
                                    Text("${edu.institution} (${edu.startDate} - ${edu.endDate})", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(
                                    onClick = {
                                        onUpdateCv { c -> c.copy(education = c.education.filterNot { it.id == edu.id }) }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove education")
                                }
                            }
                        }
                    }

                    OutlinedTextField(value = degree, onValueChange = { degree = it }, label = { Text("Degree / Certificate") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = school, onValueChange = { school = it }, label = { Text("Institution") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = years, onValueChange = { years = it }, label = { Text("Years") }, modifier = Modifier.weight(1f))
                    }
                    Button(
                        onClick = {
                            if (degree.isNotBlank()) {
                                onUpdateCv { c ->
                                    c.copy(
                                        education = c.education + EducationItem(
                                            degree = degree.trim(),
                                            institution = school.trim(),
                                            endDate = years.trim()
                                        )
                                    )
                                }
                                degree = ""; school = ""; years = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Education")
                    }
                }

                CvSectionType.SKILLS -> {
                    var skillName by remember { mutableStateOf("") }
                    var skillLevel by remember { mutableIntStateOf(4) }

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        cv.skills.forEach { s ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    onUpdateCv { c -> c.copy(skills = c.skills.filterNot { it.id == s.id }) }
                                },
                                label = { Text("${s.name} (${s.level}/5)") },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                    OutlinedTextField(value = skillName, onValueChange = { skillName = it }, label = { Text("New Skill") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Level: $skillLevel/5")
                        Spacer(modifier = Modifier.width(12.dp))
                        Slider(
                            value = skillLevel.toFloat(),
                            onValueChange = { skillLevel = it.toInt().coerceIn(1, 5) },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            if (skillName.isNotBlank()) {
                                onUpdateCv { c -> c.copy(skills = c.skills + SkillItem(name = skillName.trim(), level = skillLevel)) }
                                skillName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Skill")
                    }
                }

                CvSectionType.LANGUAGES -> {
                    var langName by remember { mutableStateOf("") }
                    var cefr by remember { mutableStateOf("B2") }

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        cv.languages.forEach { l ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    onUpdateCv { c -> c.copy(languages = c.languages.filterNot { it.id == l.id }) }
                                },
                                label = { Text("${l.language}: ${l.cefrLevel}") },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                    OutlinedTextField(value = langName, onValueChange = { langName = it }, label = { Text("Language") }, modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("A1", "A2", "B1", "B2", "C1", "C2", "Native")) { level ->
                            FilterChip(selected = cefr == level, onClick = { cefr = level }, label = { Text(level) })
                        }
                    }
                    Button(
                        onClick = {
                            if (langName.isNotBlank()) {
                                onUpdateCv { c -> c.copy(languages = c.languages + LanguageItem(language = langName.trim(), cefrLevel = cefr)) }
                                langName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Language")
                    }
                }

                CvSectionType.CERTIFICATIONS -> {
                    var certText by remember { mutableStateOf("") }
                    cv.certifications.forEach { cert ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("• $cert", modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    onUpdateCv { c -> c.copy(certifications = c.certifications.filterNot { it == cert }) }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove certification")
                            }
                        }
                    }
                    OutlinedTextField(value = certText, onValueChange = { certText = it }, label = { Text("New Certification or Award") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            if (certText.isNotBlank()) {
                                onUpdateCv { c -> c.copy(certifications = c.certifications + certText.trim()) }
                                certText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Certification")
                    }
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Done Editing")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
