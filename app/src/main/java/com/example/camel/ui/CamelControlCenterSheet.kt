package com.example.camel.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.camel.core.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamelControlCenterSheet(
    preferences: CamelOsPreferences,
    telemetry: com.example.camel.services.CamelSystemTelemetry,
    notifications: List<CamelNotification>,
    onSelectTheme: (CamelThemePreset) -> Unit,
    onSelectWallpaper: (CamelWallpaperPreset) -> Unit,
    onSelectFont: (CamelFontStyle) -> Unit,
    onToggleDoNotDisturb: (Boolean) -> Unit,
    onToggleShowTelemetry: (Boolean) -> Unit,
    onToggleShowClock: (Boolean) -> Unit,
    onOpenFullSettings: () -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Control Center",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${telemetry.deviceModel} • ${telemetry.androidVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                            onOpenFullSettings()
                        },
                        modifier = Modifier.testTag("open_full_settings_button")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("All Settings")
                    }
                }
            }

            // Control Tiles Grid (2 rows of 4)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickSettingActionTile(
                            icon = if (telemetry.isNetworkOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                            label = telemetry.networkLabel,
                            active = telemetry.isNetworkOnline,
                            onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) } },
                            modifier = Modifier.weight(1f)
                        )
                        QuickSettingActionTile(
                            icon = Icons.Filled.Bluetooth,
                            label = "Bluetooth",
                            active = false,
                            onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) } },
                            modifier = Modifier.weight(1f)
                        )
                        QuickSettingActionTile(
                            icon = Icons.Filled.Brightness6,
                            label = "Display",
                            active = false,
                            onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) } },
                            modifier = Modifier.weight(1f)
                        )
                        QuickSettingActionTile(
                            icon = Icons.Filled.VolumeUp,
                            label = "Sound",
                            active = false,
                            onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickSettingActionTile(
                            icon = Icons.Filled.DoNotDisturbOn,
                            label = if (preferences.doNotDisturb) "DND On" else "DND Off",
                            active = preferences.doNotDisturb,
                            onClick = { onToggleDoNotDisturb(!preferences.doNotDisturb) },
                            modifier = Modifier.weight(1f).testTag("toggle_dnd_tile")
                        )
                        QuickSettingActionTile(
                            icon = Icons.Filled.Speed,
                            label = "Telemetry",
                            active = preferences.showTelemetryOnDesktop,
                            onClick = { onToggleShowTelemetry(!preferences.showTelemetryOnDesktop) },
                            modifier = Modifier.weight(1f).testTag("toggle_telemetry_tile")
                        )
                        QuickSettingActionTile(
                            icon = Icons.Filled.Schedule,
                            label = "Clock",
                            active = preferences.showClockOnDesktop,
                            onClick = { onToggleShowClock(!preferences.showClockOnDesktop) },
                            modifier = Modifier.weight(1f).testTag("toggle_clock_tile")
                        )
                        QuickSettingActionTile(
                            icon = Icons.Filled.LocationOn,
                            label = "Location",
                            active = false,
                            onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Theme Swatches
            item {
                Text("Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CamelThemePreset.entries) { preset ->
                        val selected = preferences.themePreset == preset
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectTheme(preset) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(preset.primaryHex))
                                )
                            },
                            label = { Text(preset.displayName) }
                        )
                    }
                }
            }

            // Quick Font Type Switcher
            item {
                Text("Font Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CamelFontStyle.entries) { font ->
                        FilterChip(
                            selected = preferences.fontStyle == font,
                            onClick = { onSelectFont(font) },
                            label = { Text(font.displayName) }
                        )
                    }
                }
            }

            // Quick Background Wallpaper Switcher
            item {
                Text("Desktop Background", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CamelWallpaperPreset.entries) { wp ->
                        FilterChip(
                            selected = preferences.wallpaperPreset == wp,
                            onClick = {
                                if (wp == CamelWallpaperPreset.CUSTOM_IMAGE && preferences.customWallpaperPath == null) {
                                    onDismiss()
                                    onOpenFullSettings()
                                } else {
                                    onSelectWallpaper(wp)
                                }
                            },
                            label = { Text(wp.displayName) }
                        )
                    }
                }
            }

            // Notification Center Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications (${notifications.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (notifications.isNotEmpty()) {
                        TextButton(
                            onClick = onClearAllNotifications,
                            modifier = Modifier.testTag("clear_notifications_button")
                        ) {
                            Text("Clear All")
                        }
                    }
                }
            }

            if (notifications.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No active Camel OS notifications.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(notifications, key = { it.id }) { item ->
                    val badgeColor = when (item.severity) {
                        CamelNotificationSeverity.SUCCESS -> MaterialTheme.colorScheme.tertiary
                        CamelNotificationSeverity.WARNING -> MaterialTheme.colorScheme.error
                        CamelNotificationSeverity.INFO -> MaterialTheme.colorScheme.primary
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.sourceLabel} • ${item.formattedTime()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor
                                )
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = item.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDismissNotification(item.id) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Dismiss notification",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSettingActionTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (active) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.heightIn(min = 66.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
