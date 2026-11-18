package com.example.camel.core

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camel.services.CamelSystemTelemetry
import com.example.camel.services.CamelTelemetryService
import com.example.ui.components.EuropassTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CamelOsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsStore = CamelPreferencesStore(application)
    private val telemetryService = CamelTelemetryService(application)

    val preferences: StateFlow<CamelOsPreferences> = prefsStore.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CamelOsPreferences()
        )

    private val _telemetry = MutableStateFlow(telemetryService.snapshot())
    val telemetry: StateFlow<CamelSystemTelemetry> = _telemetry.asStateFlow()

    private val _activeApp = MutableStateFlow<CamelAppId?>(null)
    val activeApp: StateFlow<CamelAppId?> = _activeApp.asStateFlow()

    private val _runningApps = MutableStateFlow<List<CamelAppId>>(emptyList())
    val runningApps: StateFlow<List<CamelAppId>> = _runningApps.asStateFlow()

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isControlCenterOpen = MutableStateFlow(false)
    val isControlCenterOpen: StateFlow<Boolean> = _isControlCenterOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _pendingIntegrationApp = MutableStateFlow<CamelAppId?>(null)
    val pendingIntegrationApp: StateFlow<CamelAppId?> = _pendingIntegrationApp.asStateFlow()

    private val _appContextMenuTarget = MutableStateFlow<CamelAppId?>(null)
    val appContextMenuTarget: StateFlow<CamelAppId?> = _appContextMenuTarget.asStateFlow()

    private val _requestedEuropassTab = MutableStateFlow<EuropassTab?>(null)
    val requestedEuropassTab: StateFlow<EuropassTab?> = _requestedEuropassTab.asStateFlow()

    private val _notifications = MutableStateFlow(
        listOf(
            CamelNotification(
                sourceApp = null,
                sourceLabel = "Camel Core",
                title = "Camel OS Ready",
                message = "Tap the Camel emblem in the top panel to open Applications, or long-press any app to pin desktop & panel shortcuts.",
                severity = CamelNotificationSeverity.SUCCESS
            )
        )
    )
    val notifications: StateFlow<List<CamelNotification>> = _notifications.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(2_500)
                _telemetry.value = telemetryService.snapshot()
            }
        }
    }

    fun openApp(appId: CamelAppId, europassTab: EuropassTab? = null) {
        val descriptor = CamelAppRegistry.get(appId)
        if (descriptor.id.routeKey in preferences.value.disabledApps) {
            postNotification(
                sourceApp = appId,
                sourceLabel = "App Manager",
                title = "${descriptor.title} is Disabled",
                message = "Re-enable ${descriptor.title} in OS Settings > Applications to launch it.",
                severity = CamelNotificationSeverity.WARNING
            )
            return
        }
        if (!descriptor.isIntegrated && appId.webFolder() == null) {
            _pendingIntegrationApp.value = appId
            return
        }

        if (appId == CamelAppId.EUROPASS && europassTab != null) {
            _requestedEuropassTab.value = europassTab
        }

        _isDrawerOpen.value = false
        _isSettingsOpen.value = false
        _isControlCenterOpen.value = false
        _appContextMenuTarget.value = null
        _activeApp.value = appId

        _runningApps.update { current ->
            (listOf(appId) + current.filterNot { it == appId }).distinct()
        }
    }

    fun consumeRequestedEuropassTab() {
        _requestedEuropassTab.value = null
    }

    fun closeApp(appId: CamelAppId) {
        _runningApps.update { list -> list.filterNot { it == appId } }
        if (_activeApp.value == appId) {
            _activeApp.value = _runningApps.value.firstOrNull()
        }
    }

    fun goHome() {
        _activeApp.value = null
        _isDrawerOpen.value = false
        _isSettingsOpen.value = false
        _isControlCenterOpen.value = false
        _appContextMenuTarget.value = null
    }

    fun toggleDrawer(open: Boolean? = null) {
        val target = open ?: !_isDrawerOpen.value
        _isDrawerOpen.value = target
        if (target) {
            _isSettingsOpen.value = false
            _isControlCenterOpen.value = false
        }
    }

    fun openSettings(open: Boolean = true) {
        _isSettingsOpen.value = open
        if (open) {
            _isDrawerOpen.value = false
            _isControlCenterOpen.value = false
        }
    }

    fun toggleControlCenter(open: Boolean? = null) {
        val target = open ?: !_isControlCenterOpen.value
        _isControlCenterOpen.value = target
        if (target) {
            markAllNotificationsRead()
        }
    }

    fun openAppContextMenu(appId: CamelAppId) {
        _appContextMenuTarget.value = appId
    }

    fun dismissAppContextMenu() {
        _appContextMenuTarget.value = null
    }

    fun dismissPendingIntegrationSheet() {
        _pendingIntegrationApp.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setThemePreset(preset: CamelThemePreset) {
        viewModelScope.launch {
            prefsStore.setThemePreset(preset)
        }
    }

    fun setWallpaperPreset(preset: CamelWallpaperPreset) {
        viewModelScope.launch {
            prefsStore.setWallpaperPreset(preset)
        }
    }

    fun setWallpaperDimAlpha(alpha: Float) {
        viewModelScope.launch {
            prefsStore.setWallpaperDimAlpha(alpha)
        }
    }

    fun setCustomWallpaperFromUri(uri: Uri) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(getApplication<Application>().filesDir, "wallpapers").apply { mkdirs() }
                    val target = File(dir, "custom_desktop_bg.jpg")
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    target.absolutePath
                }.getOrNull()
            }
            if (!savedPath.isNullOrBlank()) {
                prefsStore.setCustomWallpaperPath(savedPath)
                postNotification(
                    sourceApp = null,
                    sourceLabel = "Appearance",
                    title = "Wallpaper Updated",
                    message = "Applied custom desktop background.",
                    severity = CamelNotificationSeverity.SUCCESS
                )
            }
        }
    }

    fun setFontStyle(fontStyle: CamelFontStyle) {
        viewModelScope.launch {
            prefsStore.setFontStyle(fontStyle)
        }
    }

    fun setDoNotDisturb(enabled: Boolean) {
        viewModelScope.launch { prefsStore.setDoNotDisturb(enabled) }
    }

    fun setShowTelemetry(enabled: Boolean) {
        viewModelScope.launch { prefsStore.setShowTelemetry(enabled) }
    }

    fun setShowClockOnDesktop(enabled: Boolean) {
        viewModelScope.launch { prefsStore.setShowClock(enabled) }
    }

    fun toggleDesktopShortcut(appId: CamelAppId) {
        viewModelScope.launch {
            val wasPinned = appId.routeKey in preferences.value.pinnedDesktopApps
            prefsStore.toggleDesktopShortcut(appId.routeKey)
            val title = CamelAppRegistry.get(appId).title
            postNotification(
                sourceApp = appId,
                sourceLabel = "Desktop",
                title = if (wasPinned) "Removed from Desktop" else "Added to Desktop",
                message = if (wasPinned) "$title shortcut removed from desktop." else "$title shortcut pinned to desktop.",
                severity = CamelNotificationSeverity.INFO
            )
        }
    }

    fun clearDesktopShortcuts() {
        viewModelScope.launch { prefsStore.clearDesktopShortcuts() }
    }

    fun togglePanelShortcut(appId: CamelAppId) {
        viewModelScope.launch {
            prefsStore.togglePanelShortcut(appId.routeKey)
        }
    }

    fun toggleAppEnabled(appId: CamelAppId) {
        viewModelScope.launch {
            if (appId.routeKey !in preferences.value.disabledApps) {
                closeApp(appId)
            }
            prefsStore.toggleAppEnabled(appId.routeKey)
        }
    }

    fun setCustomAppIcon(appId: CamelAppId, iconName: String) {
        viewModelScope.launch {
            prefsStore.setCustomAppIcon(appId.routeKey, "icon:$iconName")
            postNotification(
                sourceApp = appId,
                sourceLabel = "Appearance",
                title = "Icon Updated",
                message = "${CamelAppRegistry.get(appId).title} now uses the $iconName icon.",
                severity = CamelNotificationSeverity.INFO
            )
        }
    }

    fun resetCustomAppIcon(appId: CamelAppId) {
        viewModelScope.launch {
            val hadOverride = appId.routeKey in preferences.value.customAppIcons
            prefsStore.setCustomAppIcon(appId.routeKey, null)
            if (hadOverride) {
                runCatching {
                    File(getApplication<Application>().filesDir, "app_icons/${appId.routeKey}.jpg").delete()
                }
                postNotification(
                    sourceApp = appId,
                    sourceLabel = "Appearance",
                    title = "Icon Reset",
                    message = "${CamelAppRegistry.get(appId).title} is back to its original icon.",
                    severity = CamelNotificationSeverity.INFO
                )
            }
        }
    }

    /**
     * Gallery icon: decode the picked image, downscale it to at most 512 px
     * (same pattern as PhotoStorageHelper) and store it as a private JPEG
     * per app, so the icon survives after the picker URI permission ends.
     */
    fun setCustomAppIconFromUri(appId: CamelAppId, uri: Uri) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(getApplication<Application>().filesDir, "app_icons").apply { mkdirs() }
                    val target = File(dir, "${appId.routeKey}.jpg")
                    var saved = false
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        val decoded = BitmapFactory.decodeStream(input)
                        if (decoded != null) {
                            val scaled = scaleBitmapDown(decoded, 512)
                            java.io.FileOutputStream(target).use { out ->
                                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            }
                            if (scaled !== decoded) decoded.recycle()
                            scaled.recycle()
                            saved = true
                        }
                    }
                    if (saved) target.absolutePath else null
                }.getOrNull()
            }
            if (!savedPath.isNullOrBlank()) {
                prefsStore.setCustomAppIcon(appId.routeKey, "file:$savedPath")
                postNotification(
                    sourceApp = appId,
                    sourceLabel = "Appearance",
                    title = "Icon Updated",
                    message = "${CamelAppRegistry.get(appId).title} now uses your chosen image.",
                    severity = CamelNotificationSeverity.INFO
                )
            }
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    fun postNotification(
        sourceApp: CamelAppId?,
        sourceLabel: String,
        title: String,
        message: String,
        severity: CamelNotificationSeverity = CamelNotificationSeverity.INFO
    ) {
        if (preferences.value.doNotDisturb && severity != CamelNotificationSeverity.WARNING) {
            return
        }
        val item = CamelNotification(
            sourceApp = sourceApp,
            sourceLabel = sourceLabel,
            title = title,
            message = message,
            severity = severity,
            isUnread = !_isControlCenterOpen.value
        )
        _notifications.update { current -> (listOf(item) + current).take(20) }
    }

    fun dismissNotification(id: String) {
        _notifications.update { list -> list.filterNot { it.id == id } }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }

    private fun markAllNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(isUnread = false) } }
    }
}
