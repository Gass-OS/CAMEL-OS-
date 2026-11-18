package com.example.camel.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Per-app custom icons (Feature 1 of the desktop customisation track).
 *
 * The registry icon is always the default. An override is stored in
 * [CamelOsPreferences.customAppIcons] as routeKey -> reference, where a
 * reference is one of:
 * - "icon:<Name>" — another built-in Material icon, e.g. "icon:Star".
 * - "file:<absolute path>" — a gallery image copied into the app's
 *   filesDir/app_icons/<routeKey>.jpg by [CamelOsViewModel].
 *
 * References are stored in DataStore as a string set of
 * "routeKey|reference" pairs (DataStore has no map type; same trick as the
 * pinned-app sets). Malformed entries are ignored on read, so a bad value
 * can never break the desktop — the registry icon is shown instead.
 * Real icon packs (reading installed icon-pack apps) are deliberately out
 * of v1; see CUSTOMISATION-PLAN.md.
 */
object CamelAppIconOverrides {

    fun parse(raw: Set<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (entry in raw) {
            val routeKey = entry.substringBefore("|", "")
            val reference = entry.substringAfter("|", "")
            if (routeKey.isBlank() || reference.isBlank()) continue
            if (!isValidReference(reference)) continue
            result[routeKey] = reference
        }
        return result
    }

    fun encode(overrides: Map<String, String>): Set<String> =
        overrides.entries
            .filter { (routeKey, reference) ->
                routeKey.isNotBlank() && isValidReference(reference)
            }
            .map { (routeKey, reference) -> "$routeKey|$reference" }
            .toSet()

    fun isValidReference(reference: String): Boolean {
        return when {
            reference.startsWith("icon:") ->
                builtInIcon(reference.removePrefix("icon:")) != null
            reference.startsWith("file:") ->
                reference.removePrefix("file:").isNotBlank()
            else -> false
        }
    }

    /** Built-in icon for a catalogue name, or null if the name is unknown. */
    fun builtInIcon(name: String): ImageVector? =
        BUILT_IN.firstOrNull { (iconName, _) -> iconName == name }?.second

    /** The icon vector to show for [descriptor] given the stored overrides. */
    fun iconVectorFor(
        descriptor: CamelAppDescriptor,
        overrides: Map<String, String>
    ): ImageVector {
        val reference = overrides[descriptor.id.routeKey] ?: return descriptor.icon
        if (!reference.startsWith("icon:")) return descriptor.icon
        return builtInIcon(reference.removePrefix("icon:")) ?: descriptor.icon
    }

    /** Curated built-in picker grid: the registry icons plus common extras. */
    val BUILT_IN: List<Pair<String, ImageVector>> = listOf(
        "AccessAlarms" to Icons.Filled.AccessAlarms,
        "Badge" to Icons.Filled.Badge,
        "Calculate" to Icons.Filled.Calculate,
        "CalendarMonth" to Icons.Filled.CalendarMonth,
        "Chat" to Icons.Filled.Chat,
        "CloudDownload" to Icons.Filled.CloudDownload,
        "DesktopWindows" to Icons.Filled.DesktopWindows,
        "DownloadForOffline" to Icons.Filled.DownloadForOffline,
        "EditNote" to Icons.Filled.EditNote,
        "Email" to Icons.Filled.Email,
        "Explore" to Icons.Filled.Explore,
        "Favorite" to Icons.Filled.Favorite,
        "Folder" to Icons.Filled.Folder,
        "FolderZip" to Icons.Filled.FolderZip,
        "Home" to Icons.Filled.Home,
        "LiveTv" to Icons.Filled.LiveTv,
        "Map" to Icons.Filled.Map,
        "Memory" to Icons.Filled.Memory,
        "Mic" to Icons.Filled.Mic,
        "MusicNote" to Icons.Filled.MusicNote,
        "OndemandVideo" to Icons.Filled.OndemandVideo,
        "Person" to Icons.Filled.Person,
        "PhotoCamera" to Icons.Filled.PhotoCamera,
        "PhotoLibrary" to Icons.Filled.PhotoLibrary,
        "Radio" to Icons.Filled.Radio,
        "School" to Icons.Filled.School,
        "Star" to Icons.Filled.Star,
        "Terminal" to Icons.Filled.Terminal,
        "WbSunny" to Icons.Filled.WbSunny,
        "Work" to Icons.Filled.Work
    )
}
