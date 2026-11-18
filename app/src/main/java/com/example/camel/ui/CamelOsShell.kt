package com.example.camel.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.apps.europass.EuropassModuleScreen
import com.example.camel.core.CamelAppId
import com.example.camel.core.CamelAppRegistry
import com.example.camel.core.webFolder
import com.example.camel.core.folderToAppId
import com.example.camel.core.CamelNotificationSeverity
import com.example.camel.core.CamelOsPreferences
import com.example.camel.core.CamelOsViewModel
import com.example.camel.services.CamelSystemTelemetry
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.viewmodel.EuropassViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class CamelViewportMode {
    DESKTOP,
    DRAWER,
    SETTINGS,
    MODULE
}

private fun Modifier.consumingOverlay(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            event.changes.forEach { it.consume() }
        }
    }
}

@Composable
fun CamelOsShell(
    osViewModel: CamelOsViewModel,
    europassViewModel: EuropassViewModel
) {
    val prefs by osViewModel.preferences.collectAsStateWithLifecycle()
    val telemetry by osViewModel.telemetry.collectAsStateWithLifecycle()
    val activeApp by osViewModel.activeApp.collectAsStateWithLifecycle()
    val runningApps by osViewModel.runningApps.collectAsStateWithLifecycle()
    val isDrawerOpen by osViewModel.isDrawerOpen.collectAsStateWithLifecycle()
    val isSettingsOpen by osViewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val isControlCenterOpen by osViewModel.isControlCenterOpen.collectAsStateWithLifecycle()
    val searchQuery by osViewModel.searchQuery.collectAsStateWithLifecycle()
    val notifications by osViewModel.notifications.collectAsStateWithLifecycle()
    val pendingApp by osViewModel.pendingIntegrationApp.collectAsStateWithLifecycle()
    val contextMenuTarget by osViewModel.appContextMenuTarget.collectAsStateWithLifecycle()
    val unreadCount = remember(notifications) { notifications.count { it.isUnread } }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showDesktopContextMenu by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    val closeRunningApp = { appId: CamelAppId ->
         appId.webFolder()?.let(::releaseCamelWebView)
         osViewModel.closeApp(appId)
     }

    BackHandler(enabled = true) {
        when {
            showExitConfirmation -> showExitConfirmation = false
            isControlCenterOpen -> osViewModel.toggleControlCenter(false)
            contextMenuTarget != null -> osViewModel.dismissAppContextMenu()
            pendingApp != null -> osViewModel.dismissPendingIntegrationSheet()
            isDrawerOpen -> osViewModel.toggleDrawer(false)
            isSettingsOpen -> osViewModel.openSettings(false)
            activeApp != null -> osViewModel.goHome()
            else -> showExitConfirmation = true
        }
    }

    CamelOsTheme(
        preset = prefs.themePreset,
        fontStyle = prefs.fontStyle
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top KDE Plasma-Style Panel (Launcher, Tools, Pinned/Running Apps, System Tray)
                CamelTopPlasmaPanel(
                    activeApp = activeApp,
                    isDrawerOpen = isDrawerOpen,
                    isSettingsOpen = isSettingsOpen,
                    runningApps = runningApps,
                    pinnedPanelApps = prefs.pinnedPanelApps,
                    telemetry = telemetry,
                    unreadNotifications = unreadCount,
                    onToggleDrawer = { osViewModel.toggleDrawer() },
                    onOpenSettings = { osViewModel.openSettings(true) },
                    onSelectApp = { osViewModel.openApp(it) },
                    onCloseRunningApp = closeRunningApp,
                    onOpenControlCenter = { osViewModel.toggleControlCenter(true) }
                )

                // Media pill: in the layout flow directly under the top
                // panel, never as an overlay on top of it, so it can never
                // cover the launcher/menu buttons or intercept their taps.
                // When no media session is active it takes no space.
                CamelDynamicIsland(
                    onOpenApp = { folder ->
                        folderToAppId(folder)?.let { osViewModel.openApp(it) }
                    }
                )

                val viewportInsetsModifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()

                Box(modifier = viewportInsetsModifier) {
                    CamelBaseViewport(
                        activeApp = activeApp,
                        prefs = prefs,
                        osViewModel = osViewModel,
                        europassViewModel = europassViewModel,
                        telemetry = telemetry,
                        onLongPressEmptySpace = { showDesktopContextMenu = true }
                    )

                    if (isDrawerOpen) {
                        Box(Modifier.fillMaxSize().consumingOverlay()) {
                            CamelAppDrawer(
                                searchQuery = searchQuery,
                                disabledApps = prefs.disabledApps,
                                onSearchQueryChange = osViewModel::updateSearchQuery,
                                onSelectApp = { osViewModel.openApp(it) },
                                onLongClickApp = osViewModel::openAppContextMenu,
                                onOpenSettings = { osViewModel.openSettings(true) },
                                onCloseDrawer = { osViewModel.toggleDrawer(false) }
                            )
                        }
                    }

                    if (isSettingsOpen) {
                        Box(Modifier.fillMaxSize().consumingOverlay()) {
                            CamelSettingsScreen(
                                preferences = prefs,
                                onSelectTheme = osViewModel::setThemePreset,
                                onSelectWallpaper = osViewModel::setWallpaperPreset,
                                onPickCustomWallpaperUri = osViewModel::setCustomWallpaperFromUri,
                                onChangeWallpaperDim = osViewModel::setWallpaperDimAlpha,
                                onSelectFont = osViewModel::setFontStyle,
                                onToggleShowClock = osViewModel::setShowClockOnDesktop,
                                onToggleShowTelemetry = osViewModel::setShowTelemetry,
                                onClearDesktopShortcuts = osViewModel::clearDesktopShortcuts,
                                onToggleDesktopShortcut = osViewModel::toggleDesktopShortcut,
                                onTogglePanelShortcut = osViewModel::togglePanelShortcut,
                                onToggleAppEnabled = osViewModel::toggleAppEnabled,
                                onOpenApp = { osViewModel.openApp(it) },
                                onCloseSettings = { osViewModel.openSettings(false) }
                            )
                        }
                    }
                }
            }

            if (isControlCenterOpen) {
                CamelControlCenterSheet(
                    preferences = prefs,
                    telemetry = telemetry,
                    notifications = notifications,
                    onSelectTheme = osViewModel::setThemePreset,
                    onSelectWallpaper = osViewModel::setWallpaperPreset,
                    onSelectFont = osViewModel::setFontStyle,
                    onToggleDoNotDisturb = osViewModel::setDoNotDisturb,
                    onToggleShowTelemetry = osViewModel::setShowTelemetry,
                    onToggleShowClock = osViewModel::setShowClockOnDesktop,
                    onOpenFullSettings = { osViewModel.openSettings(true) },
                    onDismissNotification = osViewModel::dismissNotification,
                    onClearAllNotifications = osViewModel::clearAllNotifications,
                    onDismiss = { osViewModel.toggleControlCenter(false) }
                )
            }

            if (showDesktopContextMenu) {
                CamelDesktopContextSheet(
                    onOpenApplications = {
                        showDesktopContextMenu = false
                        osViewModel.toggleDrawer(true)
                    },
                    onPersonalize = {
                        showDesktopContextMenu = false
                        osViewModel.openSettings(true)
                    },
                    onQuickSettings = {
                        showDesktopContextMenu = false
                        osViewModel.toggleControlCenter(true)
                    },
                    onDismiss = { showDesktopContextMenu = false }
                )
            }

            val ctxDescriptor = contextMenuTarget?.let { CamelAppRegistry.get(it) }
            if (ctxDescriptor != null) {
                CamelAppContextSheet(
                    app = ctxDescriptor,
                    isPinnedDesktop = ctxDescriptor.id.routeKey in prefs.pinnedDesktopApps,
                    isPinnedPanel = ctxDescriptor.id.routeKey in prefs.pinnedPanelApps,
                    onOpenApp = { osViewModel.openApp(it) },
                    onToggleDesktopShortcut = osViewModel::toggleDesktopShortcut,
                    onTogglePanelShortcut = osViewModel::togglePanelShortcut,
                    onOpenInAppManager = { osViewModel.openSettings(true) },
                    onDismiss = osViewModel::dismissAppContextMenu
                )
            }

            val pendingDescriptor = pendingApp?.let { CamelAppRegistry.get(it) }
            if (pendingDescriptor != null) {
                CamelPendingModuleSheet(
                    app = pendingDescriptor,
                    onDismiss = osViewModel::dismissPendingIntegrationSheet
                )
            }

            if (showExitConfirmation) {
                AlertDialog(
                    onDismissRequest = { showExitConfirmation = false },
                    title = { Text("Exit Camel OS?") },
                    text = { Text("Are you sure you want to close Camel OS?") },
                    confirmButton = {
                        TextButton(onClick = { activity?.finish() }) { Text("Exit") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitConfirmation = false }) { Text("Cancel") }
                    }
                )
            }
            }
        }
    }
}

@Composable
private fun CamelBaseViewport(
    activeApp: CamelAppId?,
    prefs: CamelOsPreferences,
    osViewModel: CamelOsViewModel,
    europassViewModel: EuropassViewModel,
    telemetry: CamelSystemTelemetry,
    onLongPressEmptySpace: () -> Unit
) {
    val requestedEuropassTab by osViewModel.requestedEuropassTab.collectAsStateWithLifecycle()
    val baseMode = if (activeApp != null) CamelViewportMode.MODULE else CamelViewportMode.DESKTOP

    AnimatedContent(
        targetState = baseMode,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "camel_base_viewport_transition"
    ) { currentMode ->
        when (currentMode) {
            CamelViewportMode.DESKTOP -> CamelDesktopDashboard(
                preferences = prefs,
                telemetry = telemetry,
                onOpenApp = osViewModel::openApp,
                onLongClickApp = osViewModel::openAppContextMenu,
                onOpenControlCenter = { osViewModel.toggleControlCenter(true) },
                onLongPressEmptySpace = onLongPressEmptySpace
            )

            CamelViewportMode.MODULE -> {
                val app = activeApp
                when (app) {
                    CamelAppId.EUROPASS -> EuropassModuleScreen(
                        viewModel = europassViewModel,
                        requestedTab = requestedEuropassTab,
                        onConsumedRequestedTab = osViewModel::consumeRequestedEuropassTab,
                        onPostOsNotification = { title, message ->
                            osViewModel.postNotification(
                                sourceApp = CamelAppId.EUROPASS,
                                sourceLabel = "Europass",
                                title = title,
                                message = message,
                                severity = CamelNotificationSeverity.SUCCESS
                            )
                        },
                        onExitModule = osViewModel::goHome
                    )

                    else -> {
                        val folder = app?.webFolder()
                        if (folder != null) {
                            key(folder) {
                                CamelWebApp(folder = folder, onClose = osViewModel::goHome)
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }

            else -> Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun CamelTopPlasmaPanel(
    activeApp: CamelAppId?,
    isDrawerOpen: Boolean,
    isSettingsOpen: Boolean,
    runningApps: List<CamelAppId>,
    pinnedPanelApps: Set<String>,
    telemetry: CamelSystemTelemetry,
    unreadNotifications: Int,
    onToggleDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectApp: (CamelAppId) -> Unit,
    onCloseRunningApp: (CamelAppId) -> Unit,
    onOpenControlCenter: () -> Unit
) {
    var clockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var switcherExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            clockMillis = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val clockText = remember(clockMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(clockMillis))
    }

    val panelShortcutModules = remember(pinnedPanelApps, runningApps) {
        CamelAppRegistry.modules.filter {
            it.id.routeKey in pinnedPanelApps && it.id !in runningApps
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Camel Logo -> Opens Application Menu
            Surface(
                onClick = onToggleDrawer,
                color = if (isDrawerOpen) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isDrawerOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("top_panel_camel_menu")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_camel_foreground),
                        contentDescription = "Open Application Menu",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                    )
                }
            }

            // OS Settings Tool
            PanelToolIconButton(
                icon = Icons.Filled.Settings,
                description = "Camel OS Settings",
                selected = isSettingsOpen,
                onClick = onOpenSettings,
                modifier = Modifier.testTag("top_panel_settings")
            )

            // Running Windows + Pinned Panel App Shortcuts
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (runningApps.size > 1) {
                    item(key = "app_switcher") {
                        Box {
                            Surface(
                                onClick = { switcherExpanded = true },
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp).testTag("top_panel_app_switcher")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Apps,
                                        contentDescription = "Open apps",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        "${runningApps.size}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = switcherExpanded,
                                onDismissRequest = { switcherExpanded = false },
                                modifier = Modifier.widthIn(max = 260.dp)
                            ) {
                                runningApps.forEach { appId ->
                                    val descriptor = CamelAppRegistry.get(appId)
                                    DropdownMenuItem(
                                        text = { Text(descriptor.title, maxLines = 1) },
                                        leadingIcon = {
                                            Icon(
                                                descriptor.icon,
                                                contentDescription = null,
                                                tint = Color(descriptor.accentHex),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { onCloseRunningApp(appId) }) {
                                                Icon(
                                                    Icons.Filled.Close,
                                                    contentDescription = "Close ${descriptor.title}",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            switcherExpanded = false
                                            onSelectApp(appId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                items(runningApps, key = { "run_${it.name}" }) { appId ->
                    val descriptor = CamelAppRegistry.get(appId)
                    val isCurrent = activeApp == appId && !isDrawerOpen && !isSettingsOpen
                    Surface(
                        onClick = { onSelectApp(appId) },
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("top_panel_running_${appId.routeKey}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                descriptor.icon,
                                contentDescription = descriptor.title,
                                tint = Color(descriptor.accentHex),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = descriptor.title,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            IconButton(
                                onClick = { onCloseRunningApp(appId) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Close ${descriptor.title}",
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                items(panelShortcutModules, key = { "pin_${it.id.name}" }) { app ->
                    Surface(
                        onClick = { onSelectApp(app.id) },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("top_panel_pin_${app.id.routeKey}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                app.icon,
                                contentDescription = app.title,
                                tint = Color(app.accentHex),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // System Tray (Battery + Network + Notifications + Clock -> Control Center)
            Surface(
                onClick = onOpenControlCenter,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("top_panel_control_center")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        if (telemetry.isNetworkOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                        contentDescription = telemetry.networkLabel,
                        tint = if (telemetry.isNetworkOnline) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${telemetry.batteryPercent}%",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Control Center",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = clockText,
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
