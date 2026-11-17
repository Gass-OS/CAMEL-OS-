package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.model.CvTemplateType
import com.example.ui.components.PhotoSourceBottomSheet
import com.example.viewmodel.AiGenerationState
import com.example.viewmodel.BuilderDraftState
import java.util.Locale

@Composable
fun AiStudioScreen(
    draft: BuilderDraftState,
    aiState: AiGenerationState,
    onUpdateDraft: ((BuilderDraftState) -> BuilderDraftState) -> Unit,
    onAppendSpokenText: (String) -> Unit,
    onContinueWithAi: () -> Unit,
    onGenerateFullCv: () -> Unit,
    onDismissAiState: () -> Unit,
    onOpenManualCanvas: () -> Unit
) {
    val context = LocalContext.current
    var showPhotoSheet by remember { mutableStateOf(false) }
    var showContactFields by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(speechRecognizer) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun startVoiceRecognition() {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            Toast.makeText(context, "Speech recognition service is unavailable on this device.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                Toast.makeText(context, "Could not capture speech (code $error). Try typing or speaking closer.", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    onAppendSpokenText(spoken)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceRecognition()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice dictation.", Toast.LENGTH_SHORT).show()
        }
    }

    val isBusy = aiState is AiGenerationState.Loading || aiState is AiGenerationState.Streaming

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FREE EUROPASS PRO GENERATOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Craft your European CV in seconds",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "Add your photo, enter your name, then speak or write your experience. AI completes and formats your CV.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }

        // Photo & Identity Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { showPhotoSheet = true }
                            .testTag("studio_photo_picker"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!draft.photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(draft.photoUri),
                                contentDescription = "Your CV portrait",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = "Upload or take photo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Profile Picture",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Upload from gallery or snap a camera portrait",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FilledTonalButton(
                            onClick = { showPhotoSheet = true },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("studio_photo_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (draft.photoUri == null) "Add Photo" else "Change Photo")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = draft.firstName,
                        onValueChange = { v -> onUpdateDraft { it.copy(firstName = v) } },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_first_name")
                    )
                    OutlinedTextField(
                        value = draft.lastName,
                        onValueChange = { v -> onUpdateDraft { it.copy(lastName = v) } },
                        label = { Text("Surname") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_last_name")
                    )
                }

                OutlinedTextField(
                    value = draft.targetRole,
                    onValueChange = { v -> onUpdateDraft { it.copy(targetRole = v) } },
                    label = { Text("Target Job Title (e.g. Android Developer, Nurse)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_target_role")
                )

                TextButton(
                    onClick = { showContactFields = !showContactFields },
                    modifier = Modifier.testTag("toggle_contact_details")
                ) {
                    Icon(
                        if (showContactFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showContactFields) "Hide Contact & Nationality" else "Add Email, Phone, City & Nationality")
                }

                AnimatedVisibility(visible = showContactFields) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = draft.email,
                            onValueChange = { v -> onUpdateDraft { it.copy(email = v) } },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = draft.phone,
                                onValueChange = { v -> onUpdateDraft { it.copy(phone = v) } },
                                label = { Text("Phone") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = draft.location,
                                onValueChange = { v -> onUpdateDraft { it.copy(location = v) } },
                                label = { Text("City, Country") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = draft.nationality,
                            onValueChange = { v -> onUpdateDraft { it.copy(nationality = v) } },
                            label = { Text("Nationality") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Talk or Write Narrative + AI Continue
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Profile & Experience",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Speak with mic or type a few lines — AI continues it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledIconButton(
                        onClick = {
                            if (isListening) {
                                speechRecognizer?.stopListening()
                                isListening = false
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    startVoiceRecognition()
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isListening) Color.White else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("voice_dictate_button")
                    ) {
                        Icon(
                            if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop listening" else "Talk to describe experience"
                        )
                    }
                }

                if (isListening) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Listening... speak naturally about your jobs, skills, and languages.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = draft.narrative,
                    onValueChange = { v -> onUpdateDraft { it.copy(narrative = v) } },
                    placeholder = {
                        Text("Example: I worked 3 years in Milan as a full-stack developer using Kotlin and React. I speak Italian native and English C1...")
                    },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_narrative")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onContinueWithAi,
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("ai_continue_button")
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Continue")
                    }

                    TextButton(
                        onClick = { onUpdateDraft { it.copy(narrative = "") } },
                        enabled = draft.narrative.isNotBlank() && !isBusy,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Clear")
                    }
                }
            }
        }

        // Template Quick Selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Modern Europass Model",
                style = MaterialTheme.typography.titleMedium
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(CvTemplateType.entries) { tpl ->
                    val selected = draft.selectedTemplate == tpl
                    Surface(
                        onClick = { onUpdateDraft { it.copy(selectedTemplate = tpl) } },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.testTag("studio_template_${tpl.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(tpl.defaultAccentHex))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tpl.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // AI Status / Error Banner
        when (aiState) {
            is AiGenerationState.Loading, is AiGenerationState.Streaming -> {
                val msg = if (aiState is AiGenerationState.Loading) aiState.message else (aiState as AiGenerationState.Streaming).message
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = msg, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            is AiGenerationState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissAiState) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }
            is AiGenerationState.Success -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF14532D),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissAiState) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF14532D))
                        }
                    }
                }
            }
            AiGenerationState.Idle -> {}
        }

        // Primary Action Buttons
        Button(
            onClick = onGenerateFullCv,
            enabled = !isBusy,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("generate_cv_button")
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Generate Free Europass CV", style = MaterialTheme.typography.titleMedium)
        }

        OutlinedButton(
            onClick = onOpenManualCanvas,
            enabled = !isBusy,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("open_canvas_manual_button")
        ) {
            Icon(Icons.Default.EditNote, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Interactive Canvas Editor")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showPhotoSheet) {
        PhotoSourceBottomSheet(
            hasExistingPhoto = !draft.photoUri.isNullOrBlank(),
            onDismiss = { showPhotoSheet = false },
            onPhotoSelected = { uri -> onUpdateDraft { it.copy(photoUri = uri) } }
        )
    }
}
