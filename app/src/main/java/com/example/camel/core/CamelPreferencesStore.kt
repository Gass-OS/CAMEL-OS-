package com.example.camel.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.camelOsDataStore: DataStore<Preferences> by preferencesDataStore(name = "camel_os_prefs")

/** Top panel width: full width, or a floating pill with side margins. */
enum class CamelPanelWidth(val id: String) {
    FULL("full"),
    FLOATING("floating");

    companion object {
        fun fromId(id: String?): CamelPanelWidth =
            entries.firstOrNull { it.id == id } ?: FULL
    }
}

/** Top panel colour source: theme surface, theme accent, or a custom colour. */
enum class CamelPanelColour(val id: String) {
    THEME_SURFACE("theme_surface"),
    ACCENT("accent"),
    CUSTOM("custom");

    companion object {
        fun fromId(id: String?): CamelPanelColour =
            entries.firstOrNull { it.id == id } ?: THEME_SURFACE
    }
}

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
    val customAppIcons: Map<String, String> = emptyMap(),
    /** Top panel height in dp (60–84). 60 is the original height — the panel can grow, never shrink below stock. */
    val panelHeightDp: Float = 60f,
    val panelWidthMode: CamelPanelWidth = CamelPanelWidth.FULL,
    val panelColourMode: CamelPanelColour = CamelPanelColour.THEME_SURFACE,
    /** Custom panel colour (ARGB hex), used when panelColourMode is CUSTOM. */
    val panelCustomColourHex: Long = 0xFFD4AF37,
    /** Top panel opacity (0.50–1.00). Today's look: 0.96. Never below 0.50. */
    val panelAlpha: Float = 0.96f,
    /** Top panel corner radius in dp (0–24). 0 = square; floating panels use 12 dp when 0. */
    val panelCornerRadiusDp: Float = 0f
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
        val PANEL_HEIGHT_DP = floatPreferencesKey("panel_height_dp")
        val PANEL_WIDTH_MODE = stringPreferencesKey("panel_width_mode")
        val PANEL_COLOUR_MODE = stringPreferencesKey("panel_colour_mode")
        val PANEL_CUSTOM_COLOUR_HEX = longPreferencesKey("panel_custom_colour_hex")
        val PANEL_ALPHA = floatPreferencesKey("panel_alpha")
        val PANEL_CORNER_RADIUS_DP = floatPreferencesKey("panel_corner_radius_dp")
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
            customAppIcons = CamelAppIconOverrides.parse(prefs[Keys.CUSTOM_APP_ICONS] ?: emptySet()),
            panelHeightDp = (prefs[Keys.PANEL_HEIGHT_DP] ?: 60f).coerceIn(60f, 84f),
            panelWidthMode = CamelPanelWidth.fromId(prefs[Keys.PANEL_WIDTH_MODE]),
            panelColourMode = CamelPanelColour.fromId(prefs[Keys.PANEL_COLOUR_MODE]),
            panelCustomColourHex = prefs[Keys.PANEL_CUSTOM_COLOUR_HEX] ?: 0xFFD4AF37,
            panelAlpha = prefs[Keys.PANEL_ALPHA] ?: 0.96f,
            panelCornerRadiusDp = prefs[Keys.PANEL_CORNER_RADIUS_DP] ?: 0f
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

    suspend fun setPanelHeightDp(heightDp: Float) {
        context.camelOsDataStore.edit { it[Keys.PANEL_HEIGHT_DP] = heightDp.coerceIn(60f, 84f) }
    }

    suspend fun setPanelWidthMode(mode: CamelPanelWidth) {
        context.camelOsDataStore.edit { it[Keys.PANEL_WIDTH_MODE] = mode.id }
    }

    suspend fun setPanelColourMode(mode: CamelPanelColour) {
        context.camelOsDataStore.edit { it[Keys.PANEL_COLOUR_MODE] = mode.id }
    }

    suspend fun setPanelCustomColourHex(hex: Long) {
        context.camelOsDataStore.edit { it[Keys.PANEL_CUSTOM_COLOUR_HEX] = hex }
    }

    suspend fun setPanelAlpha(alpha: Float) {
        context.camelOsDataStore.edit { it[Keys.PANEL_ALPHA] = alpha.coerceIn(0.5f, 1f) }
    }

    suspend fun setPanelCornerRadiusDp(radiusDp: Float) {
        context.camelOsDataStore.edit { it[Keys.PANEL_CORNER_RADIUS_DP] = radiusDp.coerceIn(0f, 24f) }
    }

    /** Restore all five panel settings to today's defaults. */
    suspend fun resetPanelSettings() {
        context.camelOsDataStore.edit { prefs ->
            prefs.remove(Keys.PANEL_HEIGHT_DP)
            prefs.remove(Keys.PANEL_WIDTH_MODE)
            prefs.remove(Keys.PANEL_COLOUR_MODE)
            prefs.remove(Keys.PANEL_CUSTOM_COLOUR_HEX)
            prefs.remove(Keys.PANEL_ALPHA)
            prefs.remove(Keys.PANEL_CORNER_RADIUS_DP)
        }
    }
}
