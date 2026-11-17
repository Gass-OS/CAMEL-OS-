package com.example.camel.ui

import androidx.compose.foundation.layout.*
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
import com.example.camel.core.CamelAppDescriptor
import com.example.camel.core.CamelAppId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamelAppContextSheet(
    app: CamelAppDescriptor,
    isPinnedDesktop: Boolean,
    isPinnedPanel: Boolean,
    onOpenApp: (CamelAppId) -> Unit,
    onToggleDesktopShortcut: (CamelAppId) -> Unit,
    onTogglePanelShortcut: (CamelAppId) -> Unit,
    onOpenInAppManager: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(app.accentHex).copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(app.icon, contentDescription = app.title, tint = Color(app.accentHex))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${app.category.label} • ${app.batch.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

            Button(
                onClick = {
                    onDismiss()
                    onOpenApp(app.id)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("ctx_open_app")
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (app.isIntegrated) "Open ${app.title}" else "Inspect ${app.title} Source Spec")
            }

            OutlinedButton(
                onClick = {
                    onToggleDesktopShortcut(app.id)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("ctx_toggle_desktop")
            ) {
                Icon(
                    if (isPinnedDesktop) Icons.Filled.DeleteOutline else Icons.Filled.AddToHomeScreen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isPinnedDesktop) "Remove Shortcut from Desktop" else "Add Shortcut to Desktop")
            }

            OutlinedButton(
                onClick = {
                    onTogglePanelShortcut(app.id)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("ctx_toggle_panel")
            ) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isPinnedPanel) "Unpin from Top Panel" else "Pin to Top Panel")
            }

            TextButton(
                onClick = {
                    onDismiss()
                    onOpenInAppManager()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("ctx_app_settings")
            ) {
                Icon(Icons.Filled.SettingsApplications, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage App & Permissions in OS Settings")
            }
        }
    }
}
