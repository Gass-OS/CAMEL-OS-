package com.example.camel.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class CamelAppCategory(val label: String) {
    PRODUCTIVITY_AI("Productivity"),
    UTILITIES("Utilities"),
    MEDIA_STREAMS("Media & TV"),
    HARDWARE_SYSTEM("System"),
    ENVIRONMENTS("VMs & Linux")
}

enum class CamelIntegrationBatch(val label: String, val order: Int) {
    INTEGRATED("Ready", 0),
    BATCH_1("Batch 1 • Utilities", 1),
    BATCH_2("Batch 2 • Productivity", 2),
    BATCH_3("Batch 3 • Media & TV", 3),
    BATCH_4("Batch 4 • System & Sensors", 4),
    BATCH_5("Batch 5 • Linux & Windows", 5)
}

enum class CamelAppId(val routeKey: String) {
    EUROPASS("europass"),
    HORLOGE("horloge"),
    CALCULATOR("calculator"),
    NOTES("notes"),
    RECORDER("recorder"),
    CALENDRIER("calendrier"),
    WEATHER("weather"),
    MAPS("maps"),
    MUSIC_PLAYER("music_player"),
    VIDEO_PLAYER("video_player"),
    RADIO_GOLD("radio_gold"),
    TV_GOLD("tv_gold"),
    CAMERA("camera"),
    GALERIE("galerie"),
    BOUSSOLE("boussole"),
    SYSTEM_INFO("system_info"),
    MANAGER_GOLD("manager_gold"),
    LINUX_LOADER("linux_loader"),
    LINUX_RUNNER("linux_runner"),
    WINDOWS_LOADER("windows_loader"),
    WINDOWS_RUNNER("windows_runner")
}

data class CamelAppDescriptor(
    val id: CamelAppId,
    val title: String,
    val termuxPath: String,
    val category: CamelAppCategory,
    val batch: CamelIntegrationBatch,
    val isIntegrated: Boolean,
    val icon: ImageVector,
    val accentHex: Long,
    val sharedServices: List<String>,
    val keywords: List<String> = emptyList()
)

object CamelAppRegistry {
    val modules: List<CamelAppDescriptor> = listOf(
        CamelAppDescriptor(
            id = CamelAppId.EUROPASS,
            title = "Europass",
            termuxPath = "/data/data/com.termux/files/home/europass",
            category = CamelAppCategory.PRODUCTIVITY_AI,
            batch = CamelIntegrationBatch.INTEGRATED,
            isIntegrated = true,
            icon = Icons.Filled.Badge,
            accentHex = 0xFFD4AF37,
            sharedServices = listOf("Firebase AI", "App Check", "Room DB", "PDF Export"),
            keywords = listOf("cv", "resume", "ai", "pdf", "canva")
        ),
        CamelAppDescriptor(
            id = CamelAppId.HORLOGE,
            title = "Horloge",
            termuxPath = "/data/data/com.termux/files/home/horloge",
            category = CamelAppCategory.UTILITIES,
            batch = CamelIntegrationBatch.BATCH_1,
            isIntegrated = true,
            icon = Icons.Filled.AccessAlarms,
            accentHex = 0xFFF59E0B,
            sharedServices = listOf("AlarmScheduler", "NotificationBus"),
            keywords = listOf("clock", "alarm", "timer", "stopwatch")
        ),
        CamelAppDescriptor(
            id = CamelAppId.CALCULATOR,
            title = "Calculator",
            termuxPath = "/data/data/com.termux/files/home/Gass-os-Calculator",
            category = CamelAppCategory.UTILITIES,
            batch = CamelIntegrationBatch.BATCH_1,
            isIntegrated = true,
            icon = Icons.Filled.Calculate,
            accentHex = 0xFF38BDF8,
            sharedServices = listOf("Camel Core"),
            keywords = listOf("math", "scientific", "calc", "gass")
        ),
        CamelAppDescriptor(
            id = CamelAppId.NOTES,
            title = "Notes",
            termuxPath = "/data/data/com.termux/files/home/note",
            category = CamelAppCategory.UTILITIES,
            batch = CamelIntegrationBatch.BATCH_1,
            isIntegrated = true,
            icon = Icons.Filled.EditNote,
            accentHex = 0xFFFBBF24,
            sharedServices = listOf("Room DB", "SharedStorage"),
            keywords = listOf("memo", "document", "text", "note")
        ),
        CamelAppDescriptor(
            id = CamelAppId.RECORDER,
            title = "Recorder",
            termuxPath = "/data/data/com.termux/files/home/recorder",
            category = CamelAppCategory.UTILITIES,
            batch = CamelIntegrationBatch.BATCH_1,
            isIntegrated = true,
            icon = Icons.Filled.Mic,
            accentHex = 0xFFF43F5E,
            sharedServices = listOf("RecorderService", "NotificationBus"),
            keywords = listOf("audio", "voice", "mic", "recording")
        ),
        CamelAppDescriptor(
            id = CamelAppId.CALENDRIER,
            title = "Calendrier",
            termuxPath = "/data/data/com.termux/files/home/calendrier",
            category = CamelAppCategory.PRODUCTIVITY_AI,
            batch = CamelIntegrationBatch.BATCH_2,
            isIntegrated = true,
            icon = Icons.Filled.CalendarMonth,
            accentHex = 0xFF60A5FA,
            sharedServices = listOf("CalendarScheduler", "NotificationBus"),
            keywords = listOf("calendar", "agenda", "events")
        ),
        CamelAppDescriptor(
            id = CamelAppId.WEATHER,
            title = "Weather",
            termuxPath = "/data/data/com.termux/files/home/weather",
            category = CamelAppCategory.PRODUCTIVITY_AI,
            batch = CamelIntegrationBatch.BATCH_2,
            isIntegrated = true,
            icon = Icons.Filled.WbSunny,
            accentHex = 0xFFFDE047,
            sharedServices = listOf("WeatherService", "LocationBridge"),
            keywords = listOf("meteo", "forecast", "temperature")
        ),
        CamelAppDescriptor(
            id = CamelAppId.MAPS,
            title = "Maps",
            termuxPath = "/data/data/com.termux/files/home/map",
            category = CamelAppCategory.PRODUCTIVITY_AI,
            batch = CamelIntegrationBatch.BATCH_2,
            isIntegrated = true,
            icon = Icons.Filled.Map,
            accentHex = 0xFF34D399,
            sharedServices = listOf("LocationBridge", "NetworkService"),
            keywords = listOf("gps", "navigation", "map", "route")
        ),
        CamelAppDescriptor(
            id = CamelAppId.MUSIC_PLAYER,
            title = "Music",
            termuxPath = "/data/data/com.termux/files/home/music",
            category = CamelAppCategory.MEDIA_STREAMS,
            batch = CamelIntegrationBatch.BATCH_3,
            isIntegrated = true,
            icon = Icons.Filled.MusicNote,
            accentHex = 0xFFA78BFA,
            sharedServices = listOf("MediaPlaybackService", "NotificationBus"),
            keywords = listOf("audio", "songs", "playlist", "music")
        ),
        CamelAppDescriptor(
            id = CamelAppId.VIDEO_PLAYER,
            title = "Video",
            termuxPath = "/data/data/com.termux/files/home/video-player",
            category = CamelAppCategory.MEDIA_STREAMS,
            batch = CamelIntegrationBatch.BATCH_3,
            isIntegrated = true,
            icon = Icons.Filled.OndemandVideo,
            accentHex = 0xFFFB7185,
            sharedServices = listOf("MediaPlaybackService", "SharedStorage"),
            keywords = listOf("movies", "mp4", "mkv", "video")
        ),
        CamelAppDescriptor(
            id = CamelAppId.RADIO_GOLD,
            title = "Radio Gold",
            termuxPath = "/data/data/com.termux/files/home/radio",
            category = CamelAppCategory.MEDIA_STREAMS,
            batch = CamelIntegrationBatch.BATCH_3,
            isIntegrated = true,
            icon = Icons.Filled.Radio,
            accentHex = 0xFFD4AF37,
            sharedServices = listOf("MediaPlaybackService", "NotificationBus"),
            keywords = listOf("fm", "stations", "radio", "stream")
        ),
        CamelAppDescriptor(
            id = CamelAppId.TV_GOLD,
            title = "TV Gold",
            termuxPath = "/data/data/com.termux/files/home/tv",
            category = CamelAppCategory.MEDIA_STREAMS,
            batch = CamelIntegrationBatch.BATCH_3,
            isIntegrated = true,
            icon = Icons.Filled.LiveTv,
            accentHex = 0xFFEAB308,
            sharedServices = listOf("MediaPlaybackService", "NetworkService"),
            keywords = listOf("iptv", "channels", "tv", "live")
        ),
        CamelAppDescriptor(
            id = CamelAppId.CAMERA,
            title = "Camera",
            termuxPath = "/data/data/com.termux/files/home/camera",
            category = CamelAppCategory.HARDWARE_SYSTEM,
            batch = CamelIntegrationBatch.BATCH_4,
            isIntegrated = true,
            icon = Icons.Filled.PhotoCamera,
            accentHex = 0xFF2DD4BF,
            sharedServices = listOf("CameraBridge", "SharedStorage"),
            keywords = listOf("photo", "lens", "camera", "video")
        ),
        CamelAppDescriptor(
            id = CamelAppId.GALERIE,
            title = "Galerie",
            termuxPath = "/data/data/com.termux/files/home/galerie",
            category = CamelAppCategory.HARDWARE_SYSTEM,
            batch = CamelIntegrationBatch.BATCH_4,
            isIntegrated = true,
            icon = Icons.Filled.PhotoLibrary,
            accentHex = 0xFF818CF8,
            sharedServices = listOf("SharedStorage", "PhotoPicker"),
            keywords = listOf("photos", "images", "albums", "galerie")
        ),
        CamelAppDescriptor(
            id = CamelAppId.BOUSSOLE,
            title = "Boussole",
            termuxPath = "/data/data/com.termux/files/home/boussole",
            category = CamelAppCategory.HARDWARE_SYSTEM,
            batch = CamelIntegrationBatch.BATCH_4,
            isIntegrated = true,
            icon = Icons.Filled.Explore,
            accentHex = 0xFFF97316,
            sharedServices = listOf("SensorBridge", "LocationBridge"),
            keywords = listOf("compass", "boussole", "north", "sensors")
        ),
        CamelAppDescriptor(
            id = CamelAppId.SYSTEM_INFO,
            title = "System Info",
            termuxPath = "/data/data/com.termux/files/home/info",
            category = CamelAppCategory.HARDWARE_SYSTEM,
            batch = CamelIntegrationBatch.BATCH_4,
            isIntegrated = true,
            icon = Icons.Filled.Memory,
            accentHex = 0xFF10B981,
            sharedServices = listOf("CamelTelemetryService"),
            keywords = listOf("cpu", "ram", "battery", "info", "hardware")
        ),
        CamelAppDescriptor(
            id = CamelAppId.MANAGER_GOLD,
            title = "Manager",
            termuxPath = "/data/data/com.termux/files/home/manager",
            category = CamelAppCategory.HARDWARE_SYSTEM,
            batch = CamelIntegrationBatch.BATCH_4,
            isIntegrated = true,
            icon = Icons.Filled.FolderZip,
            accentHex = 0xFFD4AF37,
            sharedServices = listOf("SharedStorage", "AppRegistry"),
            keywords = listOf("files", "explorer", "manager", "storage")
        ),
        CamelAppDescriptor(
            id = CamelAppId.LINUX_LOADER,
            title = "Linux Loader",
            termuxPath = "/data/data/com.termux/files/home/linux-loader",
            category = CamelAppCategory.ENVIRONMENTS,
            batch = CamelIntegrationBatch.BATCH_5,
            isIntegrated = true,
            icon = Icons.Filled.DownloadForOffline,
            accentHex = 0xFF4ADE80,
            sharedServices = listOf("SharedStorage", "NetworkService"),
            keywords = listOf("linux", "rootfs", "distro", "loader")
        ),
        CamelAppDescriptor(
            id = CamelAppId.LINUX_RUNNER,
            title = "Linux Runner",
            termuxPath = "/data/data/com.termux/files/home/linux-runner",
            category = CamelAppCategory.ENVIRONMENTS,
            batch = CamelIntegrationBatch.BATCH_5,
            isIntegrated = false,
            icon = Icons.Filled.Terminal,
            accentHex = 0xFF22C55E,
            sharedServices = listOf("TaskManager", "SharedStorage"),
            keywords = listOf("linux", "runner", "proot", "terminal")
        ),
        CamelAppDescriptor(
            id = CamelAppId.WINDOWS_LOADER,
            title = "Win Loader",
            termuxPath = "/data/data/com.termux/files/home/windows-loader",
            category = CamelAppCategory.ENVIRONMENTS,
            batch = CamelIntegrationBatch.BATCH_5,
            isIntegrated = true,
            icon = Icons.Filled.CloudDownload,
            accentHex = 0xFF38BDF8,
            sharedServices = listOf("SharedStorage", "NetworkService"),
            keywords = listOf("windows", "iso", "qcow2", "loader")
        ),
        CamelAppDescriptor(
            id = CamelAppId.WINDOWS_RUNNER,
            title = "Win Runner",
            termuxPath = "/data/data/com.termux/files/home/windows-runner",
            category = CamelAppCategory.ENVIRONMENTS,
            batch = CamelIntegrationBatch.BATCH_5,
            isIntegrated = false,
            icon = Icons.Filled.DesktopWindows,
            accentHex = 0xFF60A5FA,
            sharedServices = listOf("TaskManager", "SharedStorage"),
            keywords = listOf("windows", "qemu", "kvm", "runner")
        )
    )

    fun get(id: CamelAppId): CamelAppDescriptor =
        modules.first { it.id == id }

    fun search(query: String): List<CamelAppDescriptor> {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return modules
        return modules.filter { app ->
            app.title.lowercase().contains(clean) ||
                app.category.label.lowercase().contains(clean) ||
                app.termuxPath.lowercase().contains(clean) ||
                app.keywords.any { it.contains(clean) }
        }
    }
}
