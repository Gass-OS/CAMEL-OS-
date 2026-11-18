package com.example.camel.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
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
import com.example.camel.core.CamelNotificationSeverity
import com.example.camel.core.CamelOsPreferences
import com.example.camel.core.CamelOsViewModel
import com.example.camel.core.CamelPanelColour
import com.example.camel.core.CamelPanelWidth
import com.example.camel.core.CamelWindow
import com.example.camel.core.CamelWindowState
import com.example.camel.services.CamelSystemTelemetry
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.components.EuropassTab
import com.example.viewmodel.EuropassViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val windows by osViewModel.windows.collectAsStateWithLifecycle()
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
    var iconPickerTarget by remember { mutableStateOf<CamelAppId?>(null) }
    // Desktop long-press -> Personalize opens Settings straight at the Panel section.
    var settingsStartAtPanel by remember { mutableStateOf(false) }
    LaunchedEffect(isSettingsOpen) {
        if (!isSettingsOpen) settingsStartAtPanel = false
    }
    val activity = LocalContext.current as? Activity

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val target = iconPickerTarget
        if (uri != null && target != null) {
            osViewModel.setCustomAppIconFromUri(target, uri)
        }
        iconPickerTarget = null
    }

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
            // Back with a focused window minimises that window — its
            // WebView stays acquired, so state and media are preserved.
            // Back with no window focused falls through to the exit
            // dialog below, exactly as today.
            activeApp != null -> osViewModel.minimiseWindow(activeApp)
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
                    customAppIcons = prefs.customAppIcons,
                    panelHeightDp = prefs.panelHeightDp,
                    panelWidthMode = prefs.panelWidthMode,
                    panelColourMode = prefs.panelColourMode,
                    panelCustomColourHex = prefs.panelCustomColourHex,
                    panelAlpha = prefs.panelAlpha,
                    panelCornerRadiusDp = prefs.panelCornerRadiusDp,
                    telemetry = telemetry,
                    unreadNotifications = unreadCount,
                    onToggleDrawer = { osViewModel.toggleDrawer() },
                    onOpenSettings = { osViewModel.openSettings(true) },
                    onSelectApp = { osViewModel.openApp(it) },
                    onCloseRunningApp = closeRunningApp,
                    onOpenControlCenter = { osViewModel.toggleControlCenter(true) }
                )

                val viewportInsetsModifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()

                Box(modifier = viewportInsetsModifier) {
                    CamelDesktopArea(
                        windows = windows,
                        prefs = prefs,
                        osViewModel = osViewModel,
                        europassViewModel = europassViewModel,
                        telemetry = telemetry,
                        onLongPressEmptySpace = { showDesktopContextMenu = true },
                        onCloseRunningApp = closeRunningApp
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
                                onCloseDrawer = { osViewModel.toggleDrawer(false) },
                                customAppIcons = prefs.customAppIcons
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
                                onSetCustomAppIcon = osViewModel::setCustomAppIcon,
                                onResetCustomAppIcon = osViewModel::resetCustomAppIcon,
                                onPickCustomAppIconUri = osViewModel::setCustomAppIconFromUri,
                                onOpenApp = { osViewModel.openApp(it) },
                                onCloseSettings = { osViewModel.openSettings(false) },
                                startAtPanel = settingsStartAtPanel,
                                onSetPanelHeightDp = osViewModel::setPanelHeightDp,
                                onSetPanelWidthMode = osViewModel::setPanelWidthMode,
                                onSetPanelColourMode = osViewModel::setPanelColourMode,
                                onSetPanelCustomColourHex = osViewModel::setPanelCustomColourHex,
                                onSetPanelAlpha = osViewModel::setPanelAlpha,
                                onSetPanelCornerRadiusDp = osViewModel::setPanelCornerRadiusDp,
                                onResetPanelSettings = osViewModel::resetPanelSettings
                            )
                        }
                    }
                }

                // Bottom taskbar: one item per open window. It appears when
                // the first window opens and disappears with the last one.
                if (windows.isNotEmpty()) {
                    CamelTaskbar(
                        windows = windows,
                        focusedAppId = activeApp,
                        customAppIcons = prefs.customAppIcons,
                        onSelect = { osViewModel.focusWindow(it) },
                        onClose = closeRunningApp,
                        onShowDesktop = { osViewModel.goHome() }
                    )
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
                        settingsStartAtPanel = true
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
                    onDismiss = osViewModel::dismissAppContextMenu,
                    customIconRef = prefs.customAppIcons[ctxDescriptor.id.routeKey],
                    onChangeIcon = { iconPickerTarget = it },
                    onResetIcon = { osViewModel.resetCustomAppIcon(it) }
                )
            }

            val iconPickerApp = iconPickerTarget?.let { CamelAppRegistry.get(it) }
            if (iconPickerApp != null) {
                CamelAppIconPicker(
                    app = iconPickerApp,
                    currentRef = prefs.customAppIcons[iconPickerApp.id.routeKey],
                    onSelectBuiltIn = { name ->
                        osViewModel.setCustomAppIcon(iconPickerApp.id, name)
                        iconPickerTarget = null
                    },
                    onPickFromGallery = {
                        iconPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onResetIcon = {
                        osViewModel.resetCustomAppIcon(iconPickerApp.id)
                        iconPickerTarget = null
                    },
                    onDismiss = { iconPickerTarget = null }
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

/**
 * Multi-window desktop area: the dashboard is always the background layer
 * and open windows float on top of it, like an XFCE desktop. Minimised
 * windows are not composed at all — their WebView is detached (paused
 * unless it is playing audio or recording) while the pool keeps it
 * acquired, so focusing the window restores it with its state intact.
 * A window's content is the existing module rendering, unchanged.
 */
@Composable
private fun CamelDesktopArea(
    windows: List<CamelWindow>,
    prefs: CamelOsPreferences,
    osViewModel: CamelOsViewModel,
    europassViewModel: EuropassViewModel,
    telemetry: CamelSystemTelemetry,
    onLongPressEmptySpace: () -> Unit,
    onCloseRunningApp: (CamelAppId) -> Unit
) {
    val activeApp by osViewModel.activeApp.collectAsStateWithLifecycle()
    val windowNotice by osViewModel.windowNotice.collectAsStateWithLifecycle()
    val requestedEuropassTab by osViewModel.requestedEuropassTab.collectAsStateWithLifecycle()

    // The window-cap message dismisses itself after 3 seconds.
    LaunchedEffect(windowNotice) {
        if (windowNotice != null) {
            delay(3_000)
            osViewModel.dismissWindowNotice()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        CamelDesktopDashboard(
            preferences = prefs,
            telemetry = telemetry,
            onOpenApp = osViewModel::openApp,
            onLongClickApp = osViewModel::openAppContextMenu,
            onOpenControlCenter = { osViewModel.toggleControlCenter(true) },
            onLongPressEmptySpace = onLongPressEmptySpace
        )

        // Render order: non-pinned windows by z, then pinned windows by z,
        // so pinned windows always stack above the rest.
        val visibleWindows = windows
            .filter { it.state != CamelWindowState.MINIMISED }
            .sortedWith(compareBy({ it.pinned }, { it.z }))
        visibleWindows.forEach { window ->
            key(window.appId) {
                val descriptor = CamelAppRegistry.get(window.appId)
                val frameModifier = if (window.state == CamelWindowState.MAXIMISED) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .offset {
                            IntOffset(
                                (window.bounds.x * widthPx).roundToInt(),
                                (window.bounds.y * heightPx).roundToInt()
                            )
                        }
                        .size(
                            width = with(density) { (window.bounds.width * widthPx).toDp() },
                            height = with(density) { (window.bounds.height * heightPx).toDp() }
                        )
                }
                CamelWindowFrame(
                    window = window,
                    descriptor = descriptor,
                    isFocused = window.appId == activeApp,
                    desktopWidthPx = widthPx,
                    desktopHeightPx = heightPx,
                    customIconRef = prefs.customAppIcons[window.appId.routeKey],
                    onFocus = { osViewModel.focusWindow(window.appId) },
                    onMove = { dx, dy -> osViewModel.moveWindow(window.appId, dx, dy) },
                    onResize = { dw, dh -> osViewModel.resizeWindow(window.appId, dw, dh) },
                    onMinimise = { osViewModel.minimiseWindow(window.appId) },
                    onToggleMaximise = { osViewModel.toggleMaximiseWindow(window.appId) },
                    onClose = { onCloseRunningApp(window.appId) },
                    onSnap = { snap -> osViewModel.snapWindow(window.appId, snap) },
                    onTogglePin = { osViewModel.togglePinWindow(window.appId) },
                    modifier = frameModifier
                ) {
                    CamelWindowContent(
                        appId = window.appId,
                        isFocused = window.appId == activeApp,
                        osViewModel = osViewModel,
                        europassViewModel = europassViewModel,
                        requestedEuropassTab = requestedEuropassTab
                    )
                }
            }
        }

        val notice = windowNotice
        if (notice != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .testTag("window_notice")
            ) {
                Text(
                    text = notice,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/** One window's content: the existing module rendering, unchanged. The
 * Back button inside a window is handled by the focused window only
 * (backEnabled): a WebView goes back in its history, otherwise the window
 * is minimised; Europass returns to its Studio tab, otherwise minimises. */
@Composable
private fun CamelWindowContent(
    appId: CamelAppId,
    isFocused: Boolean,
    osViewModel: CamelOsViewModel,
    europassViewModel: EuropassViewModel,
    requestedEuropassTab: EuropassTab?
) {
    when (appId) {
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
            onExitModule = { osViewModel.minimiseWindow(CamelAppId.EUROPASS) },
            backEnabled = isFocused
        )

        else -> {
            val folder = appId.webFolder()
            if (folder != null) {
                key(folder) {
                    CamelWebApp(
                        folder = folder,
                        onClose = { osViewModel.minimiseWindow(appId) },
                        backEnabled = isFocused
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
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
    onOpenControlCenter: () -> Unit,
    customAppIcons: Map<String, String> = emptyMap(),
    panelHeightDp: Float = 60f,
    panelWidthMode: CamelPanelWidth = CamelPanelWidth.FULL,
    panelColourMode: CamelPanelColour = CamelPanelColour.THEME_SURFACE,
    panelCustomColourHex: Long = 0xFFD4AF37,
    panelAlpha: Float = 0.96f,
    panelCornerRadiusDp: Float = 0f
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

    // Panel height is the 48 dp button row plus vertical padding, so the
    // 48–72 dp height setting is realised as 0–12 dp of vertical padding.
    // At the 60 dp default that is exactly today's 6 dp.
    val verticalPadding = ((panelHeightDp - 48f) / 2f).coerceIn(0f, 12f).dp
    val floating = panelWidthMode == CamelPanelWidth.FLOATING
    val cornerRadius = effectivePanelCornerRadiusDp(panelWidthMode, panelCornerRadiusDp).dp
    val panelColour = resolvePanelColour(panelColourMode, panelCustomColourHex)

    Surface(
        color = panelColour.copy(alpha = panelAlpha),
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(cornerRadius),
        // Contrast guard (plan §5): the 1 dp outline stays at all opacities.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = if (floating) {
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp)
        } else {
            Modifier.fillMaxWidth()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = verticalPadding),
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
                                            CamelAppIcon(
                                                app = descriptor,
                                                customIconRef = customAppIcons[descriptor.id.routeKey],
                                                modifier = Modifier.size(18.dp),
                                                tint = Color(descriptor.accentHex)
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
                            CamelAppIcon(
                                app = descriptor,
                                customIconRef = customAppIcons[descriptor.id.routeKey],
                                modifier = Modifier.size(17.dp),
                                tint = Color(descriptor.accentHex)
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
                            CamelAppIcon(
                                app = app,
                                customIconRef = customAppIcons[app.id.routeKey],
                                modifier = Modifier.size(20.dp),
                                tint = Color(app.accentHex)
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
