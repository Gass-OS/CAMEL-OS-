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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.camel.core.CamelAppDescriptor
import com.example.camel.core.CamelAppId
import com.example.camel.core.webFolder

/**
 * Long-press app menu, upgraded to the frosted transparent style used across
 * the plasma desktop. The gesture and the four actions are unchanged; this
 * sheet adds an app-info block (version, folder, size, status, services).
 *
 * Frosted, not blurred: true background blur needs RenderEffect (API 31+)
 * and minSdk is 24, so below API 31 the sheet is translucent without blur.
 */
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
    onDismiss: () -> Unit,
    customIconRef: String? = null,
    onChangeIcon: ((CamelAppId) -> Unit)? = null,
    onResetIcon: ((CamelAppId) -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.35f)
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
                        CamelAppIcon(
                            app = app,
                            customIconRef = customIconRef,
                            modifier = Modifier.size(24.dp),
                            tint = Color(app.accentHex)
                        )
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

            // App info — version comes from the OS build itself (BuildConfig),
            // because the registry carries no per-app semantic version.
            // Sizes are the bundled asset-folder sizes measured at plan time.
            AppInfoRow(label = "Version", value = BuildConfig.VERSION_NAME)
            AppInfoRow(
                label = "Status",
                value = if (app.isIntegrated) "Integrated" else "Awaiting upload"
            )
            AppInfoRow(
                label = "Folder",
                value = app.id.webFolder() ?: if (app.id == CamelAppId.EUROPASS) {
                    "Native app"
                } else {
                    "—"
                }
            )
            AppInfoRow(
                label = "Size",
                value = if (app.id.webFolder() != null) "Assets ${app.sizeLabel}" else "—"
            )
            if (app.sharedServices.isNotEmpty()) {
                AppInfoRow(
                    label = "Services",
                    value = app.sharedServices.joinToString(", ")
                )
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

            if (onChangeIcon != null) {
                OutlinedButton(
                    onClick = { onChangeIcon(app.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ctx_change_icon")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change icon")
                }
            }

            if (customIconRef != null && onResetIcon != null) {
                TextButton(
                    onClick = { onResetIcon(app.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ctx_reset_icon")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset icon to original")
                }
            }
        }
    }
}

@Composable
private fun AppInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
