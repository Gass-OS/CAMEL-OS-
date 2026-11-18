package com.example.camel.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.camel.core.CamelOsPreferences
import com.example.camel.core.CamelPanelColour
import com.example.camel.core.CamelPanelWidth

/**
 * Shared panel-style math for [CamelTopPlasmaPanel] and the live preview in
 * [CamelSettingsScreen], so the preview can never drift from the real panel.
 *
 * Defaults reproduce today's look exactly: 60 dp height, full width, theme
 * surface at 96 % opacity, square corners, 1 dp outline.
 */

/** Resolves the panel base colour for a colour mode. Alpha is applied by the caller. */
@Composable
fun resolvePanelColour(mode: CamelPanelColour, customHex: Long): Color =
    when (mode) {
        CamelPanelColour.THEME_SURFACE -> MaterialTheme.colorScheme.surface
        CamelPanelColour.ACCENT -> MaterialTheme.colorScheme.primary
        CamelPanelColour.CUSTOM -> Color(customHex)
    }

/**
 * Effective corner radius in dp. A floating panel with the radius at its
 * default (0) uses 12 dp so it reads as a pill instead of a floating square;
 * full-width panels and any explicit radius are used as stored.
 */
fun effectivePanelCornerRadiusDp(widthMode: CamelPanelWidth, radiusDp: Float): Float =
    if (widthMode == CamelPanelWidth.FLOATING && radiusDp <= 0f) 12f else radiusDp

/**
 * Content scale for the top panel, relative to the original 60 dp height.
 * The whole panel row (buttons, icons, text) scales with the height setting,
 * so the panel never clips its content: at 10 dp it is a thin proportional
 * strip, at 80 dp everything grows the same way. Used by the real panel and
 * the settings preview so they cannot drift.
 */
fun panelContentScale(panelHeightDp: Float): Float =
    (panelHeightDp / 60f).coerceIn(0.15f, 1.5f)

/** True when any panel setting differs from the defaults (drives the Reset button). */
fun isPanelCustomised(preferences: CamelOsPreferences): Boolean {
    val defaults = CamelOsPreferences()
    return preferences.panelHeightDp != defaults.panelHeightDp ||
        preferences.panelWidthMode != defaults.panelWidthMode ||
        preferences.panelColourMode != defaults.panelColourMode ||
        preferences.panelCustomColourHex != defaults.panelCustomColourHex ||
        preferences.panelAlpha != defaults.panelAlpha ||
        preferences.panelCornerRadiusDp != defaults.panelCornerRadiusDp
}
