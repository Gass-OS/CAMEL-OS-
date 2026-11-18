package com.example.camel.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.camel.core.*
import com.example.ui.theme.JetBrainsMonoFamily

private enum class CamelSettingsTab(val label: String) {
    APPEARANCE("Appearance"),
    DESKTOP_PANEL("Desktop & Panel"),
    APPS_MANAGER("Apps (${CamelAppRegistry.modules.size})")
}

@Composable
fun CamelSettingsScreen(
    preferences: CamelOsPreferences,
    onSelectTheme: (CamelThemePreset) -> Unit,
    onSelectWallpaper: (CamelWallpaperPreset) -> Unit,
    onPickCustomWallpaperUri: (Uri) -> Unit,
    onChangeWallpaperDim: (Float) -> Unit,
    onSelectFont: (CamelFontStyle) -> Unit,
    onToggleShowClock: (Boolean) -> Unit,
    onToggleShowTelemetry: (Boolean) -> Unit,
    onClearDesktopShortcuts: () -> Unit,
    onToggleDesktopShortcut: (CamelAppId) -> Unit,
    onTogglePanelShortcut: (CamelAppId) -> Unit,
    onToggleAppEnabled: (CamelAppId) -> Unit,
    onOpenApp: (CamelAppId) -> Unit,
    onCloseSettings: () -> Unit,
    onSetCustomAppIcon: (CamelAppId, String) -> Unit = { _, _ -> },
    onResetCustomAppIcon: (CamelAppId) -> Unit = {},
    onPickCustomAppIconUri: (CamelAppId, Uri) -> Unit = { _, _ -> },
    startAtPanel: Boolean = false,
    onSetPanelHeightDp: (Float) -> Unit = {},
    onSetPanelWidthMode: (CamelPanelWidth) -> Unit = {},
    onSetPanelColourMode: (CamelPanelColour) -> Unit = {},
    onSetPanelCustomColourHex: (Long) -> Unit = {},
    onSetPanelAlpha: (Float) -> Unit = {},
    onSetPanelCornerRadiusDp: (Float) -> Unit = {},
    onResetPanelSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember {
        mutableStateOf(
            if (startAtPanel) CamelSettingsTab.DESKTOP_PANEL else CamelSettingsTab.APPEARANCE
        )
    }
    var appFilterQuery by remember { mutableStateOf("") }
    var iconPickerTarget by remember { mutableStateOf<CamelAppId?>(null) }

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickCustomWallpaperUri(uri)
        }
    }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val target = iconPickerTarget
        if (uri != null && target != null) {
            onPickCustomAppIconUri(target, uri)
        }
        iconPickerTarget = null
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 960.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Camel OS Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Themes, wallpapers, fonts, panel & 21-app manager",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onCloseSettings,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("close_settings_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close OS Settings")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                CamelSettingsTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = CamelSettingsTab.entries.size
                        ),
                        modifier = Modifier.testTag("settings_tab_${tab.name.lowercase()}")
                    ) {
                        Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (activeTab) {
                CamelSettingsTab.APPEARANCE -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 28.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Wallpaper Picker Section
                        item {
                            SettingsSectionCard(title = "Desktop Background Wallpaper") {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(CamelWallpaperPreset.entries) { wp ->
                                        val isSelected = preferences.wallpaperPreset == wp
                                        Surface(
                                            onClick = {
                                                if (wp == CamelWallpaperPreset.CUSTOM_IMAGE && preferences.customWallpaperPath == null) {
                                                    wallpaperPicker.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                } else {
                                                    onSelectWallpaper(wp)
                                                }
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            ),
                                            modifier = Modifier
                                                .width(124.dp)
                                                .height(78.dp)
                                                .testTag("wallpaper_${wp.id}")
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color(wp.topHex), Color(wp.midHex), Color(wp.bottomHex))
                                                        )
                                                    )
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.BottomStart
                                            ) {
                                                Text(
                                                    text = wp.displayName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        wallpaperPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("pick_custom_wallpaper_button")
                                ) {
                                    Icon(Icons.Filled.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Choose Custom Background from Gallery")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Wallpaper Dim Overlay (${(preferences.wallpaperDimAlpha * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = preferences.wallpaperDimAlpha,
                                    onValueChange = onChangeWallpaperDim,
                                    valueRange = 0.1f..0.85f
                                )
                            }
                        }

                        // Theme Presets Section
                        item {
                            SettingsSectionCard(title = "Camel OS Color Themes") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CamelThemePreset.entries.forEach { preset ->
                                        val selected = preferences.themePreset == preset
                                        Surface(
                                            onClick = { onSelectTheme(preset) },
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(
                                                if (selected) 1.5.dp else 1.dp,
                                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("settings_theme_${preset.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(preset.primaryHex))
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(preset.displayName, style = MaterialTheme.typography.titleSmall)
                                                    Text(
                                                        preset.subtitle,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (selected) {
                                                    Icon(
                                                        Icons.Filled.CheckCircle,
                                                        contentDescription = "Active",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Font Type Section
                        item {
                            SettingsSectionCard(title = "System Typography / Font Type") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CamelFontStyle.entries.forEach { font ->
                                        val selected = preferences.fontStyle == font
                                        Surface(
                                            onClick = { onSelectFont(font) },
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(
                                                if (selected) 1.5.dp else 1.dp,
                                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("font_style_${font.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Filled.FontDownload,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(font.displayName, style = MaterialTheme.typography.titleSmall)
                                                    Text(
                                                        font.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (selected) {
                                                    Icon(
                                                        Icons.Filled.CheckCircle,
                                                        contentDescription = "Active font",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                CamelSettingsTab.DESKTOP_PANEL -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 28.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            SettingsSectionCard(title = "Top Panel") {
                                // Live preview — same style math as the real panel.
                                val previewColour = resolvePanelColour(
                                    preferences.panelColourMode,
                                    preferences.panelCustomColourHex
                                )
                                val previewRadius = effectivePanelCornerRadiusDp(
                                    preferences.panelWidthMode,
                                    preferences.panelCornerRadiusDp
                                )
                                Surface(
                                    color = previewColour.copy(alpha = preferences.panelAlpha),
                                    shape = RoundedCornerShape(previewRadius.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("panel_preview")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Calculator",
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "12:00",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Panel height (${preferences.panelHeightDp.toInt()} dp)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = preferences.panelHeightDp,
                                    onValueChange = onSetPanelHeightDp,
                                    valueRange = 60f..84f,
                                    steps = 23,
                                    modifier = Modifier.testTag("panel_height_slider")
                                )
                                Text(
                                    text = "Panel width",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = preferences.panelWidthMode == CamelPanelWidth.FULL,
                                        onClick = { onSetPanelWidthMode(CamelPanelWidth.FULL) },
                                        label = { Text("Full width") },
                                        modifier = Modifier.testTag("panel_width_full")
                                    )
                                    FilterChip(
                                        selected = preferences.panelWidthMode == CamelPanelWidth.FLOATING,
                                        onClick = { onSetPanelWidthMode(CamelPanelWidth.FLOATING) },
                                        label = { Text("Floating") },
                                        modifier = Modifier.testTag("panel_width_floating")
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Panel colour",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = preferences.panelColourMode == CamelPanelColour.THEME_SURFACE,
                                        onClick = { onSetPanelColourMode(CamelPanelColour.THEME_SURFACE) },
                                        label = { Text("Theme surface") },
                                        modifier = Modifier.testTag("panel_colour_theme_surface")
                                    )
                                    FilterChip(
                                        selected = preferences.panelColourMode == CamelPanelColour.ACCENT,
                                        onClick = { onSetPanelColourMode(CamelPanelColour.ACCENT) },
                                        label = { Text("Accent") },
                                        modifier = Modifier.testTag("panel_colour_accent")
                                    )
                                    FilterChip(
                                        selected = preferences.panelColourMode == CamelPanelColour.CUSTOM,
                                        onClick = { onSetPanelColourMode(CamelPanelColour.CUSTOM) },
                                        label = { Text("Custom") },
                                        modifier = Modifier.testTag("panel_colour_custom")
                                    )
                                }
                                if (preferences.panelColourMode == CamelPanelColour.CUSTOM) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(panelCustomColours) { (name, hex) ->
                                            val isSelected = preferences.panelCustomColourHex == hex
                                            Surface(
                                                onClick = {
                                                    onSetPanelCustomColourHex(hex)
                                                    onSetPanelColourMode(CamelPanelColour.CUSTOM)
                                                },
                                                color = Color(hex),
                                                shape = CircleShape,
                                                border = BorderStroke(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                ),
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("panel_custom_colour_$name")
                                            ) {
                                                if (isSelected) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Filled.Check,
                                                            contentDescription = "Selected",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Panel opacity (${(preferences.panelAlpha * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = preferences.panelAlpha,
                                    onValueChange = onSetPanelAlpha,
                                    valueRange = 0.5f..1f,
                                    steps = 49,
                                    modifier = Modifier.testTag("panel_opacity_slider")
                                )
                                Text(
                                    text = "Corner radius (${previewRadius.toInt()} dp)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = preferences.panelCornerRadiusDp,
                                    onValueChange = onSetPanelCornerRadiusDp,
                                    valueRange = 0f..24f,
                                    steps = 23,
                                    modifier = Modifier.testTag("panel_radius_slider")
                                )
                                if (preferences.panelWidthMode == CamelPanelWidth.FLOATING &&
                                    preferences.panelCornerRadiusDp <= 0f
                                ) {
                                    Text(
                                        text = "Floating panels use 12 dp corners when the radius is 0.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                OutlinedButton(
                                    onClick = onResetPanelSettings,
                                    enabled = isPanelCustomised(preferences),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("reset_panel_button")
                                ) {
                                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reset panel to defaults")
                                }
                            }
                        }

                        item {
                            SettingsSectionCard(title = "Clean KDE Desktop Widgets") {
                                SettingsSwitchRow(
                                    title = "Show Desktop Clock & Date",
                                    subtitle = "Floating digital clock on empty desktop",
                                    checked = preferences.showClockOnDesktop,
                                    onCheckedChange = onToggleShowClock
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                SettingsSwitchRow(
                                    title = "Show Live Hardware Telemetry",
                                    subtitle = "Battery, RAM & Storage pills in desktop header",
                                    checked = preferences.showTelemetryOnDesktop,
                                    onCheckedChange = onToggleShowTelemetry
                                )
                            }
                        }

                        item {
                            SettingsSectionCard(title = "Desktop & Top Panel Shortcuts") {
                                Text(
                                    text = "Desktop Shortcuts: ${preferences.pinnedDesktopApps.size} • Top Panel Pins: ${preferences.pinnedPanelApps.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onClearDesktopShortcuts,
                                    enabled = preferences.pinnedDesktopApps.isNotEmpty(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("clear_desktop_shortcuts_button")
                                ) {
                                    Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reset Desktop to Clean Empty Workspace")
                                }
                            }
                        }

                        item {
                            SettingsSectionCard(title = "Android System Bridges") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            runCatching { context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Display")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) }
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Sound")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", context.packageName, null)
                                            )
                                            runCatching { context.startActivity(intent) }
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Permissions")
                                    }
                                }
                            }
                        }
                    }
                }

                CamelSettingsTab.APPS_MANAGER -> {
                    val filtered = remember(appFilterQuery) { CamelAppRegistry.search(appFilterQuery) }
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = appFilterQuery,
                            onValueChange = { appFilterQuery = it },
                            singleLine = true,
                            placeholder = { Text("Manage 21 apps, permissions & shortcuts...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 28.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filtered, key = { it.id.name }) { app ->
                                val isDisabled = app.id.routeKey in preferences.disabledApps
                                val isDesktopPinned = app.id.routeKey in preferences.pinnedDesktopApps
                                val isPanelPinned = app.id.routeKey in preferences.pinnedPanelApps

                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Surface(
                                                color = Color(app.accentHex).copy(alpha = 0.16f),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.size(42.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                CamelAppIcon(
                                                    app = app,
                                                    customIconRef = preferences.customAppIcons[app.id.routeKey],
                                                    modifier = Modifier.size(24.dp),
                                                    tint = Color(app.accentHex)
                                                )
                                            }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = app.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = app.termuxPath,
                                                    fontFamily = JetBrainsMonoFamily,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Switch(
                                                checked = !isDisabled,
                                                onCheckedChange = { onToggleAppEnabled(app.id) }
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            FilterChip(
                                                selected = isDesktopPinned,
                                                onClick = { onToggleDesktopShortcut(app.id) },
                                                label = { Text(if (isDesktopPinned) "On Desktop" else "Add Desktop") }
                                            )
                                            FilterChip(
                                                selected = isPanelPinned,
                                                onClick = { onTogglePanelShortcut(app.id) },
                                                label = { Text(if (isPanelPinned) "In Panel" else "Pin Panel") }
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            TextButton(onClick = { iconPickerTarget = app.id }) {
                                                Text("Change icon")
                                            }
                                            if (app.id.routeKey in preferences.customAppIcons) {
                                                TextButton(onClick = { onResetCustomAppIcon(app.id) }) {
                                                    Text("Reset icon")
                                                }
                                            }
                                            TextButton(onClick = { onOpenApp(app.id) }) {
                                                Text(if (app.isIntegrated) "Open" else "Specs")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val iconPickerApp = iconPickerTarget?.let { CamelAppRegistry.get(it) }
            if (iconPickerApp != null) {
                CamelAppIconPicker(
                    app = iconPickerApp,
                    currentRef = preferences.customAppIcons[iconPickerApp.id.routeKey],
                    onSelectBuiltIn = { name ->
                        onSetCustomAppIcon(iconPickerApp.id, name)
                        iconPickerTarget = null
                    },
                    onPickFromGallery = {
                        iconPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onResetIcon = {
                        onResetCustomAppIcon(iconPickerApp.id)
                        iconPickerTarget = null
                    },
                    onDismiss = { iconPickerTarget = null }
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Curated custom panel colours (name -> ARGB hex). */
private val panelCustomColours: List<Pair<String, Long>> = listOf(
    "gold" to 0xFFD4AF37,
    "navy" to 0xFF0E2252,
    "emerald" to 0xFF10B981,
    "amethyst" to 0xFFC084FC,
    "crimson" to 0xFFDC2626,
    "orange" to 0xFFF97316,
    "teal" to 0xFF14B8A6,
    "graphite" to 0xFF3A3F4A
)
