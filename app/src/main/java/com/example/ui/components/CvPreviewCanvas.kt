package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CvSectionType
import com.example.model.CvTemplateType
import com.example.model.EuropassCv
import com.example.ui.theme.InterFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.PlusJakartaSansFamily

@Composable
fun CvPreviewCanvas(
    cv: EuropassCv,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    compactPreview: Boolean = false,
    selectedSection: CvSectionType? = null,
    onHeaderClick: () -> Unit = {},
    onSectionClick: (CvSectionType) -> Unit = {}
) {
    val template = CvTemplateType.fromId(cv.templateId)
    val accent = Color(cv.accentColorHex)
    val scale = if (compactPreview) 0.72f else cv.fontScale.coerceIn(0.85f, 1.2f)

    val headingFont: FontFamily = when (template) {
        CvTemplateType.NORDIC_MINIMAL -> PlayfairDisplayFamily
        CvTemplateType.TECH_MONO -> JetBrainsMonoFamily
        else -> PlusJakartaSansFamily
    }
    val bodyFont: FontFamily = when (template) {
        CvTemplateType.TECH_MONO -> JetBrainsMonoFamily
        else -> InterFamily
    }

    val isDarkBanner = template == CvTemplateType.EUROPASS_CLASSIC || template == CvTemplateType.EXECUTIVE_SLATE
    val isSidebarLayout = template == CvTemplateType.CREATIVE_STUDIO

    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(if (compactPreview) 10.dp else 4.dp),
        shadowElevation = if (compactPreview) 4.dp else 10.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (isSidebarLayout) {
                Box(
                    modifier = Modifier
                        .width(if (compactPreview) 10.dp else 18.dp)
                        .fillMaxHeight()
                        .background(accent)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Header Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDarkBanner) accent else Color.White)
                        .then(
                            if (interactive) Modifier.clickable { onHeaderClick() } else Modifier
                        )
                        .padding(
                            horizontal = if (compactPreview) 12.dp else 20.dp,
                            vertical = if (compactPreview) 12.dp else 18.dp
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (compactPreview) 10.dp else 14.dp)
                    ) {
                        if (cv.showPhoto && !cv.photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(cv.photoUri),
                                contentDescription = "Candidate photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(if (compactPreview) 42.dp else 68.dp)
                                    .clip(if (template == CvTemplateType.TECH_MONO) RoundedCornerShape(10.dp) else CircleShape)
                                    .border(
                                        2.dp,
                                        if (isDarkBanner) Color.White else accent,
                                        if (template == CvTemplateType.TECH_MONO) RoundedCornerShape(10.dp) else CircleShape
                                    )
                            )
                        } else if (cv.showPhoto) {
                            Box(
                                modifier = Modifier
                                    .size(if (compactPreview) 42.dp else 64.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkBanner) Color.White.copy(alpha = 0.18f) else accent.copy(alpha = 0.1f))
                                    .border(1.5.dp, if (isDarkBanner) Color.White.copy(alpha = 0.6f) else accent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Avatar placeholder",
                                    tint = if (isDarkBanner) Color.White else accent,
                                    modifier = Modifier.size(if (compactPreview) 22.dp else 32.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = cv.fullName,
                                    color = if (isDarkBanner) Color.White else Color(0xFF0F172A),
                                    fontFamily = headingFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (20 * scale).sp,
                                    lineHeight = (24 * scale).sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (interactive) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit header",
                                        tint = if (isDarkBanner) Color.White.copy(alpha = 0.75f) else accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (cv.headline.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = cv.headline,
                                    color = if (isDarkBanner) Color(0xFFE2E8F0) else accent,
                                    fontFamily = bodyFont,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = (12.5 * scale).sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val contactLine = listOfNotNull(
                                cv.email.takeIf { it.isNotBlank() },
                                cv.phone.takeIf { it.isNotBlank() },
                                cv.location.takeIf { it.isNotBlank() }
                            ).joinToString(" • ")
                            if (contactLine.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = contactLine,
                                    color = if (isDarkBanner) Color(0xFFCBD5E1) else Color(0xFF475569),
                                    fontFamily = bodyFont,
                                    fontSize = (10 * scale).sp,
                                    lineHeight = (13 * scale).sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (!isDarkBanner) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (compactPreview) 12.dp else 20.dp)
                            .height(2.5.dp)
                            .background(accent)
                    )
                }

                // Body Sections rendered in Canva sectionOrder
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (compactPreview) 12.dp else 20.dp,
                            vertical = if (compactPreview) 10.dp else 14.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(if (compactPreview) 8.dp else 12.dp)
                ) {
                    val orderedSections = cv.sectionOrder.mapNotNull { CvSectionType.fromKey(it) }
                    val displaySections = if (compactPreview) orderedSections.take(3) else orderedSections

                    displaySections.forEach { section ->
                        val isSelected = selectedSection == section
                        val sectionModifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (isSelected) Modifier
                                    .background(accent.copy(alpha = 0.06f))
                                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                else Modifier
                            )
                            .then(
                                if (interactive) Modifier.clickable { onSectionClick(section) } else Modifier
                            )
                            .padding(horizontal = if (interactive) 6.dp else 0.dp, vertical = 4.dp)

                        when (section) {
                            CvSectionType.SUMMARY -> {
                                if (cv.summary.isNotBlank() || interactive) {
                                    Column(modifier = sectionModifier) {
                                        CvSectionHeader("PROFILE SUMMARY", accent, headingFont, scale, interactive)
                                        Text(
                                            text = cv.summary.ifBlank { "Tap to add your Europass profile summary..." },
                                            color = if (cv.summary.isBlank()) Color(0xFF94A3B8) else Color(0xFF334155),
                                            fontFamily = bodyFont,
                                            fontSize = (10.5 * scale).sp,
                                            lineHeight = (14.5 * scale).sp,
                                            maxLines = if (compactPreview) 3 else Int.MAX_VALUE,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            CvSectionType.EXPERIENCE -> {
                                if (cv.experiences.isNotEmpty() || interactive) {
                                    Column(modifier = sectionModifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CvSectionHeader("WORK EXPERIENCE", accent, headingFont, scale, interactive)
                                        if (cv.experiences.isEmpty()) {
                                            Text("Tap to add work experience", color = Color(0xFF94A3B8), fontSize = (10.5 * scale).sp)
                                        } else {
                                            val items = if (compactPreview) cv.experiences.take(1) else cv.experiences
                                            items.forEach { exp ->
                                                Column {
                                                    Text(
                                                        text = exp.role,
                                                        color = Color(0xFF0F172A),
                                                        fontFamily = headingFont,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = (11.5 * scale).sp
                                                    )
                                                    val meta = listOfNotNull(
                                                        exp.company.takeIf { it.isNotBlank() },
                                                        exp.location.takeIf { it.isNotBlank() },
                                                        listOf(exp.startDate, exp.endDate).filter { it.isNotBlank() }.joinToString(" – ").takeIf { it.isNotBlank() }
                                                    ).joinToString(" • ")
                                                    if (meta.isNotBlank()) {
                                                        Text(
                                                            text = meta,
                                                            color = accent,
                                                            fontFamily = bodyFont,
                                                            fontWeight = FontWeight.Medium,
                                                            fontSize = (9.5 * scale).sp
                                                        )
                                                    }
                                                    if (exp.description.isNotBlank()) {
                                                        Text(
                                                            text = exp.description,
                                                            color = Color(0xFF334155),
                                                            fontFamily = bodyFont,
                                                            fontSize = (10 * scale).sp,
                                                            lineHeight = (14 * scale).sp,
                                                            maxLines = if (compactPreview) 2 else Int.MAX_VALUE,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            CvSectionType.EDUCATION -> {
                                if (cv.education.isNotEmpty() || interactive) {
                                    Column(modifier = sectionModifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CvSectionHeader("EDUCATION & TRAINING", accent, headingFont, scale, interactive)
                                        if (cv.education.isEmpty()) {
                                            Text("Tap to add education", color = Color(0xFF94A3B8), fontSize = (10.5 * scale).sp)
                                        } else {
                                            val items = if (compactPreview) cv.education.take(1) else cv.education
                                            items.forEach { edu ->
                                                Column {
                                                    Text(
                                                        text = edu.degree,
                                                        color = Color(0xFF0F172A),
                                                        fontFamily = headingFont,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = (11 * scale).sp
                                                    )
                                                    val meta = listOfNotNull(
                                                        edu.institution.takeIf { it.isNotBlank() },
                                                        listOf(edu.startDate, edu.endDate).filter { it.isNotBlank() }.joinToString(" – ").takeIf { it.isNotBlank() }
                                                    ).joinToString(" • ")
                                                    if (meta.isNotBlank()) {
                                                        Text(
                                                            text = meta,
                                                            color = accent,
                                                            fontFamily = bodyFont,
                                                            fontSize = (9.5 * scale).sp
                                                        )
                                                    }
                                                    if (edu.description.isNotBlank() && !compactPreview) {
                                                        Text(
                                                            text = edu.description,
                                                            color = Color(0xFF475569),
                                                            fontFamily = bodyFont,
                                                            fontSize = (9.5 * scale).sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            CvSectionType.SKILLS -> {
                                if (cv.skills.isNotEmpty() || interactive) {
                                    Column(modifier = sectionModifier) {
                                        CvSectionHeader("DIGITAL & CORE SKILLS", accent, headingFont, scale, interactive)
                                        if (cv.skills.isEmpty()) {
                                            Text("Tap to add skills", color = Color(0xFF94A3B8), fontSize = (10.5 * scale).sp)
                                        } else {
                                            @OptIn(ExperimentalLayoutApi::class)
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                cv.skills.forEach { skill ->
                                                    Surface(
                                                        color = accent.copy(alpha = 0.09f),
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.25f))
                                                    ) {
                                                        Text(
                                                            text = "${skill.name} • ${"●".repeat(skill.level)}${"○".repeat(5 - skill.level)}",
                                                            color = Color(0xFF0F172A),
                                                            fontFamily = bodyFont,
                                                            fontSize = (9.5 * scale).sp,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            CvSectionType.LANGUAGES -> {
                                if (cv.languages.isNotEmpty() || interactive) {
                                    Column(modifier = sectionModifier) {
                                        CvSectionHeader("LANGUAGE SKILLS (CEFR)", accent, headingFont, scale, interactive)
                                        if (cv.languages.isEmpty()) {
                                            Text("Tap to add languages", color = Color(0xFF94A3B8), fontSize = (10.5 * scale).sp)
                                        } else {
                                            @OptIn(ExperimentalLayoutApi::class)
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                cv.languages.forEach { lang ->
                                                    Surface(
                                                        color = Color(0xFFF1F5F9),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = lang.language,
                                                                color = Color(0xFF0F172A),
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = (9.5 * scale).sp
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = lang.cefrLevel,
                                                                color = accent,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = (9.5 * scale).sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            CvSectionType.CERTIFICATIONS -> {
                                if (cv.certifications.isNotEmpty() || interactive) {
                                    Column(modifier = sectionModifier) {
                                        CvSectionHeader("CERTIFICATIONS & HONORS", accent, headingFont, scale, interactive)
                                        if (cv.certifications.isEmpty()) {
                                            Text("Tap to add certifications", color = Color(0xFF94A3B8), fontSize = (10.5 * scale).sp)
                                        } else {
                                            cv.certifications.forEach { cert ->
                                                Text(
                                                    text = "• $cert",
                                                    color = Color(0xFF334155),
                                                    fontFamily = bodyFont,
                                                    fontSize = (10 * scale).sp,
                                                    lineHeight = (14 * scale).sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!compactPreview) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Europass Curriculum Vitae • ${template.displayName}",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp,
                            fontFamily = bodyFont
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CvSectionHeader(
    title: String,
    accent: Color,
    headingFont: FontFamily,
    scale: Float,
    interactive: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = accent,
                fontFamily = headingFont,
                fontWeight = FontWeight.Bold,
                fontSize = (10.5 * scale).sp,
                letterSpacing = 0.8.sp
            )
            if (interactive) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit $title",
                    tint = accent.copy(alpha = 0.55f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        HorizontalDivider(thickness = 1.dp, color = accent.copy(alpha = 0.28f))
        Spacer(modifier = Modifier.height(5.dp))
    }
}
