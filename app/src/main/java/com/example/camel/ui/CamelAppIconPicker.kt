package com.example.camel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.camel.core.CamelAppDescriptor
import com.example.camel.core.CamelAppIconOverrides

/**
 * Per-app icon picker (Feature 1). Two override kinds, both stored as a
 * per-app preference and reversible:
 * - a built-in Material icon from the curated grid, or
 * - a gallery image, picked with the system photo picker and copied into
 *   the app's private filesDir by the ViewModel (never a raw content URI,
 *   so the icon still loads after the picker permission is gone).
 * No icon-pack support in v1 — see CUSTOMISATION-PLAN.md.
 */
@Composable
fun CamelAppIconPicker(
    app: CamelAppDescriptor,
    currentRef: String?,
    onSelectBuiltIn: (String) -> Unit,
    onPickFromGallery: () -> Unit,
    onResetIcon: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CamelAppIcon(
                        app = app,
                        customIconRef = currentRef,
                        modifier = Modifier.size(40.dp),
                        tint = Color(app.accentHex)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Change icon",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = app.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Built-in icons",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    items(CamelAppIconOverrides.BUILT_IN, key = { (name, _) -> name }) { (name, vector) ->
                        val isSelected = currentRef == "icon:$name"
                        val isOriginal = app.icon == vector && currentRef == null
                        Surface(
                            onClick = { onSelectBuiltIn(name) },
                            color = if (isSelected) {
                                Color(app.accentHex).copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSelected || isOriginal) 2.dp else 1.dp,
                                color = if (isSelected) {
                                    Color(app.accentHex)
                                } else if (isOriginal) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                }
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("icon_pick_$name")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    vector,
                                    contentDescription = name,
                                    tint = Color(app.accentHex),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onPickFromGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("icon_pick_gallery")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose image from gallery")
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (currentRef != null) {
                        OutlinedButton(
                            onClick = onResetIcon,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("icon_reset")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset icon")
                        }
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("icon_picker_cancel")
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
