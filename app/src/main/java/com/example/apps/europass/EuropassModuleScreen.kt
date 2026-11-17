package com.example.apps.europass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EuropassScaffold
import com.example.ui.components.EuropassTab
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.CanvasEditorScreen
import com.example.ui.screens.SavedCvsScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.EuropassViewModel

@Composable
fun EuropassModuleScreen(
    viewModel: EuropassViewModel,
    requestedTab: EuropassTab?,
    onConsumedRequestedTab: () -> Unit,
    onPostOsNotification: (title: String, message: String) -> Unit,
    onExitModule: () -> Unit,
    modifier: Modifier = Modifier
) {
    MyApplicationTheme {
        val savedCvs by viewModel.savedCvs.collectAsStateWithLifecycle()
        val builderDraft by viewModel.builderDraft.collectAsStateWithLifecycle()
        val activeCv by viewModel.activeCv.collectAsStateWithLifecycle()
        val aiState by viewModel.aiState.collectAsStateWithLifecycle()
        val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

        var currentTab by remember { mutableStateOf(EuropassTab.Studio) }
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(requestedTab) {
            if (requestedTab != null) {
                currentTab = requestedTab
                onConsumedRequestedTab()
            }
        }

        LaunchedEffect(snackbarMessage) {
            val msg = snackbarMessage
            if (!msg.isNullOrBlank()) {
                onPostOsNotification("Europass", msg)
                snackbarHostState.showSnackbar(msg)
                viewModel.clearSnackbar()
            }
        }

        BackHandler {
            if (currentTab != EuropassTab.Studio) {
                currentTab = EuropassTab.Studio
            } else {
                onExitModule()
            }
        }

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isExpandedWidth = maxWidth >= 600.dp

            EuropassScaffold(
                currentTab = currentTab,
                isExpandedWidth = isExpandedWidth,
                snackbarHostState = snackbarHostState,
                onSelectTab = { currentTab = it }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        EuropassTab.Studio -> AiStudioScreen(
                            draft = builderDraft,
                            aiState = aiState,
                            onUpdateDraft = viewModel::updateBuilderDraft,
                            onAppendSpokenText = viewModel::appendSpokenTextToNarrative,
                            onContinueWithAi = viewModel::continueProfileWithAi,
                            onGenerateFullCv = {
                                viewModel.generateEuropassCvWithAi {
                                    currentTab = EuropassTab.Canvas
                                }
                            },
                            onDismissAiState = viewModel::clearAiState,
                            onOpenManualCanvas = {
                                viewModel.createNewManualCv(builderDraft.selectedTemplate)
                                currentTab = EuropassTab.Canvas
                            }
                        )

                        EuropassTab.Templates -> TemplatesScreen(
                            activeCv = activeCv,
                            onApplyTemplate = viewModel::applyTemplate,
                            onApplyAccentColor = { hex ->
                                viewModel.updateActiveCv { it.copy(accentColorHex = hex) }
                            },
                            onOpenInCanvas = { currentTab = EuropassTab.Canvas }
                        )

                        EuropassTab.Canvas -> CanvasEditorScreen(
                            cv = activeCv,
                            aiState = aiState,
                            onUpdateCv = viewModel::updateActiveCv,
                            onSaveLocal = viewModel::saveActiveCvLocally,
                            onSharePdf = { viewModel.exportAndSharePdf(activeCv) },
                            onGeneratePdfFile = { viewModel.generatePdfFile(activeCv) },
                            onApplyTemplate = viewModel::applyTemplate,
                            onMoveSectionUp = viewModel::moveSectionUp,
                            onMoveSectionDown = viewModel::moveSectionDown,
                            onAiEnhanceSummary = viewModel::enhanceSummaryWithAi,
                            onAiEnhanceExperience = viewModel::enhanceExperienceWithAi
                        )

                        EuropassTab.Saved -> SavedCvsScreen(
                            savedCvs = savedCvs,
                            onOpenCv = { cv ->
                                viewModel.loadCvIntoCanvas(cv)
                                currentTab = EuropassTab.Canvas
                            },
                            onDuplicateCv = viewModel::duplicateCv,
                            onShareCv = { cv -> viewModel.exportAndSharePdf(cv) },
                            onDeleteCv = viewModel::deleteCv,
                            onCreateWithAi = { currentTab = EuropassTab.Studio },
                            onCreateBlankStarter = {
                                viewModel.createNewManualCv()
                                currentTab = EuropassTab.Canvas
                            }
                        )
                    }
                }
            }
        }
    }
}
