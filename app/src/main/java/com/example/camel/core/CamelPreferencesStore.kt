package com.example.camel.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.camelOsDataStore: DataStore<Preferences> by preferencesDataStore(name = "camel_os_prefs")

data class CamelOsPreferences(
    val themePreset: CamelThemePreset = CamelThemePreset.MIDNIGHT_GOLD,
    val wallpaperPreset: CamelWallpaperPreset = CamelWallpaperPreset.GOLDEN_DUNES,
    val customWallpaperPath: String? = null,
    val wallpaperDimAlpha: Float = 0.35f,
    val fontStyle: CamelFontStyle = CamelFontStyle.PLUS_JAKARTA,
    val doNotDisturb: Boolean = false,
    val showTelemetryOnDesktop: Boolean = true,
    val showClockOnDesktop: Boolean = true,
    val pinnedDesktopApps: Set<String> = emptySet(),
    val pinnedPanelApps: Set<String> = emptySet(),
    val disabledApps: Set<String> = emptySet(),
    /** Per-app custom icons: routeKey -> "icon:<Name>" or "file:<path>". */
    val customAppIcons: Map<String, String> = emptyMap()
)

class CamelPreferencesStore(private val context: Context) {

    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val WALLPAPER_PRESET = stringPreferencesKey("wallpaper_preset")
        val CUSTOM_WALLPAPER_PATH = stringPreferencesKey("custom_wallpaper_path")
        val WALLPAPER_DIM_ALPHA = floatPreferencesKey("wallpaper_dim_alpha")
        val FONT_STYLE = stringPreferencesKey("font_style")
        val DO_NOT_DISTURB = booleanPreferencesKey("do_not_disturb")
        val SHOW_TELEMETRY = booleanPreferencesKey("show_telemetry")
        val SHOW_CLOCK = booleanPreferencesKey("show_clock_desktop")
        val PINNED_DESKTOP_APPS = stringSetPreferencesKey("pinned_desktop_apps")
        val PINNED_PANEL_APPS = stringSetPreferencesKey("pinned_panel_apps")
        val DISABLED_APPS = stringSetPreferencesKey("disabled_apps")
        val CUSTOM_APP_ICONS = stringSetPreferencesKey("custom_app_icons")
    }

    val preferencesFlow: Flow<CamelOsPreferences> = context.camelOsDataStore.data.map { prefs ->
        CamelOsPreferences(
            themePreset = CamelThemePreset.fromId(prefs[Keys.THEME_PRESET]),
            wallpaperPreset = CamelWallpaperPreset.fromId(prefs[Keys.WALLPAPER_PRESET]),
            customWallpaperPath = prefs[Keys.CUSTOM_WALLPAPER_PATH],
            wallpaperDimAlpha = prefs[Keys.WALLPAPER_DIM_ALPHA] ?: 0.35f,
            fontStyle = CamelFontStyle.fromId(prefs[Keys.FONT_STYLE]),
            doNotDisturb = prefs[Keys.DO_NOT_DISTURB] ?: false,
            showTelemetryOnDesktop = prefs[Keys.SHOW_TELEMETRY] ?: true,
            showClockOnDesktop = prefs[Keys.SHOW_CLOCK] ?: true,
            pinnedDesktopApps = prefs[Keys.PINNED_DESKTOP_APPS] ?: emptySet(),
            pinnedPanelApps = prefs[Keys.PINNED_PANEL_APPS] ?: emptySet(),
            disabledApps = prefs[Keys.DISABLED_APPS] ?: emptySet(),
            customAppIcons = CamelAppIconOverrides.parse(prefs[Keys.CUSTOM_APP_ICONS] ?: emptySet())
        )
    }

    suspend fun setThemePreset(preset: CamelThemePreset) {
        context.camelOsDataStore.edit { it[Keys.THEME_PRESET] = preset.id }
    }

    suspend fun setWallpaperPreset(preset: CamelWallpaperPreset) {
        context.camelOsDataStore.edit { it[Keys.WALLPAPER_PRESET] = preset.id }
    }

    suspend fun setCustomWallpaperPath(path: String?) {
        context.camelOsDataStore.edit { prefs ->
            if (path.isNullOrBlank()) {
                prefs.remove(Keys.CUSTOM_WALLPAPER_PATH)
            } else {
                prefs[Keys.CUSTOM_WALLPAPER_PATH] = path
                prefs[Keys.WALLPAPER_PRESET] = CamelWallpaperPreset.CUSTOM_IMAGE.id
            }
        }
    }

    suspend fun setWallpaperDimAlpha(alpha: Float) {
        context.camelOsDataStore.edit { it[Keys.WALLPAPER_DIM_ALPHA] = alpha.coerceIn(0.1f, 0.85f) }
    }

    suspend fun setFontStyle(fontStyle: CamelFontStyle) {
        context.camelOsDataStore.edit { it[Keys.FONT_STYLE] = fontStyle.id }
    }

    suspend fun setDoNotDisturb(enabled: Boolean) {
        context.camelOsDataStore.edit { it[Keys.DO_NOT_DISTURB] = enabled }
    }

    suspend fun setShowTelemetry(enabled: Boolean) {
        context.camelOsDataStore.edit { it[Keys.SHOW_TELEMETRY] = enabled }
    }

    suspend fun setShowClock(enabled: Boolean) {
        context.camelOsDataStore.edit { it[Keys.SHOW_CLOCK] = enabled }
    }

    suspend fun toggleDesktopShortcut(routeKey: String) {
        context.camelOsDataStore.edit { prefs ->
            val current = prefs[Keys.PINNED_DESKTOP_APPS] ?: emptySet()
            prefs[Keys.PINNED_DESKTOP_APPS] = if (routeKey in current) current - routeKey else current + routeKey
        }
    }

    suspend fun clearDesktopShortcuts() {
        context.camelOsDataStore.edit { it[Keys.PINNED_DESKTOP_APPS] = emptySet() }
    }

    suspend fun togglePanelShortcut(routeKey: String) {
        context.camelOsDataStore.edit { prefs ->
            val current = prefs[Keys.PINNED_PANEL_APPS] ?: emptySet()
            prefs[Keys.PINNED_PANEL_APPS] = if (routeKey in current) current - routeKey else current + routeKey
        }
    }

    suspend fun toggleAppEnabled(routeKey: String) {
        context.camelOsDataStore.edit { prefs ->
            val current = prefs[Keys.DISABLED_APPS] ?: emptySet()
            prefs[Keys.DISABLED_APPS] = if (routeKey in current) current - routeKey else current + routeKey
        }
    }

    /** Set a custom icon reference for one app, or clear it when [reference] is null. */
    suspend fun setCustomAppIcon(routeKey: String, reference: String?) {
        context.camelOsDataStore.edit { prefs ->
            val current = CamelAppIconOverrides.parse(prefs[Keys.CUSTOM_APP_ICONS] ?: emptySet())
            val updated = if (reference == null) {
                current - routeKey
            } else if (CamelAppIconOverrides.isValidReference(reference)) {
                current + (routeKey to reference)
            } else {
                current
            }
            prefs[Keys.CUSTOM_APP_ICONS] = CamelAppIconOverrides.encode(updated)
        }
    }

    suspend fun resetCustomAppIcons() {
        context.camelOsDataStore.edit { it[Keys.CUSTOM_APP_ICONS] = emptySet() }
    }
}
