package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CvSectionType
import com.example.model.CvTemplateType
import com.example.model.EuropassCv
import com.example.ui.components.*
import com.example.util.CvPdfExporter
import com.example.viewmodel.AiGenerationState
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CanvasEditorScreen(
    cv: EuropassCv,
    aiState: AiGenerationState,
    onUpdateCv: ((EuropassCv) -> EuropassCv) -> Unit,
    onSaveLocal: () -> Unit,
    onSharePdf: () -> Unit,
    onGeneratePdfFile: suspend () -> File,
    onApplyTemplate: (CvTemplateType) -> Unit,
    onMoveSectionUp: (String) -> Unit,
    onMoveSectionDown: (String) -> Unit,
    onAiEnhanceSummary: (String) -> Unit,
    onAiEnhanceExperience: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var zoomFactor by remember { mutableFloatStateOf(1.0f) }
    var editingHeader by remember { mutableStateOf(false) }
    var editingDesign by remember { mutableStateOf(false) }
    var editingOrder by remember { mutableStateOf(false) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf<CvSectionType?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }
    var pendingExportPdf by remember { mutableStateOf<File?>(null) }

    val savePdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        val file = pendingExportPdf
        if (destUri != null && file != null) {
            val ok = CvPdfExporter.copyPdfToUri(context, file, destUri)
            Toast.makeText(
                context,
                if (ok) "PDF saved to your chosen folder!" else "Failed to write PDF file.",
                Toast.LENGTH_SHORT
            ).show()
        }
        pendingExportPdf = null
    }

    val isAiBusy = aiState is AiGenerationState.Loading || aiState is AiGenerationState.Streaming

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
    ) {
        // Top Canva-style Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { zoomFactor = (zoomFactor - 0.1f).coerceAtLeast(0.75f) },
                        modifier = Modifier.testTag("canvas_zoom_out")
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out")
                    }
                    Text(
                        text = "${(zoomFactor * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { zoomFactor = (zoomFactor + 0.1f).coerceAtMost(1.35f) },
                        modifier = Modifier.testTag("canvas_zoom_in")
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSaveLocal,
                        modifier = Modifier.testTag("canvas_save_local")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save CV locally", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                val file = onGeneratePdfFile()
                                pendingExportPdf = file
                                val safeTitle = cv.fullName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Europass_CV" }
                                savePdfDocumentLauncher.launch("$safeTitle.pdf")
                            }
                        },
                        modifier = Modifier.testTag("canvas_download_pdf")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                    FilledTonalButton(
                        onClick = onSharePdf,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("canvas_share_pdf")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        }

        if (isAiBusy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Interactive A4 Sheet Workspace
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val baseWidth = 360.dp * zoomFactor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tap any section on the CV to edit, add, or AI-rewrite",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                CvPreviewCanvas(
                    cv = cv,
                    interactive = true,
                    selectedSection = selectedSection,
                    onHeaderClick = { editingHeader = true },
                    onSectionClick = { sec -> selectedSection = sec },
                    modifier = Modifier
                        .width(baseWidth)
                        .testTag("cv_interactive_canvas")
                )
            }
        }

        // Bottom Canva Tool Dock
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CanvaDockTool(Icons.Default.Person, "Header", "dock_header") { editingHeader = true }
                CanvaDockTool(Icons.Default.CameraAlt, "Photo", "dock_photo") { showPhotoSheet = true }
                CanvaDockTool(Icons.Default.Palette, "Design", "dock_design") { editingDesign = true }
                CanvaDockTool(Icons.Default.AddCircleOutline, "Add / Edit", "dock_add") { showAddMenu = true }
                CanvaDockTool(Icons.Default.SwapVert, "Layout", "dock_layout") { editingOrder = true }
                CanvaDockTool(Icons.Default.AutoAwesome, "AI Polish", "dock_ai") {
                    selectedSection = CvSectionType.SUMMARY
                }
            }
        }
    }

    if (showAddMenu) {
        AlertDialog(
            onDismissRequest = { showAddMenu = false },
            title = { Text("Add or Edit Section") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CvSectionType.entries.forEach { sec ->
                        FilledTonalButton(
                            onClick = {
                                showAddMenu = false
                                selectedSection = sec
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(sec.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMenu = false }) { Text("Close") }
            }
        )
    }

    if (editingHeader) {
        HeaderEditorSheet(
            cv = cv,
            onDismiss = { editingHeader = false },
            onOpenPhotoPicker = { showPhotoSheet = true },
            onSaveHeader = { first, last, headline, email, phone, loc, nat, showPic ->
                onUpdateCv {
                    it.copy(
                        firstName = first,
                        lastName = last,
                        headline = headline,
                        email = email,
                        phone = phone,
                        location = loc,
                        nationality = nat,
                        showPhoto = showPic
                    )
                }
            }
        )
    }

    if (editingDesign) {
        DesignStudioSheet(
            cv = cv,
            onDismiss = { editingDesign = false },
            onSelectTemplate = onApplyTemplate,
            onSelectColor = { hex -> onUpdateCv { it.copy(accentColorHex = hex) } },
            onFontScaleChanged = { scale -> onUpdateCv { it.copy(fontScale = scale) } }
        )
    }

    if (editingOrder) {
        SectionOrderSheet(
            cv = cv,
            onDismiss = { editingOrder = false },
            onMoveUp = onMoveSectionUp,
            onMoveDown = onMoveSectionDown
        )
    }

    val activeSection = selectedSection
    if (activeSection != null) {
        SectionContentEditorSheet(
            section = activeSection,
            cv = cv,
            isAiBusy = isAiBusy,
            onDismiss = { selectedSection = null },
            onUpdateCv = onUpdateCv,
            onAiEnhanceSummary = onAiEnhanceSummary,
            onAiEnhanceExperience = onAiEnhanceExperience
        )
    }

    if (showPhotoSheet) {
        PhotoSourceBottomSheet(
            hasExistingPhoto = !cv.photoUri.isNullOrBlank(),
            onDismiss = { showPhotoSheet = false },
            onPhotoSelected = { uri ->
                onUpdateCv { it.copy(photoUri = uri, showPhoto = uri != null) }
            }
        )
    }
}

@Composable
private fun CanvaDockTool(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(3.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}
