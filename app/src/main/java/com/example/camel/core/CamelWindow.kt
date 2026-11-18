package com.example.camel.core

/**
 * Multi-window desktop model (Termux/XFCE-style multitasking).
 *
 * A window is one open app. Bounds are fractions (0..1) of the desktop
 * area — the space below the top panel and above the taskbar — so windows
 * keep their relative position when the taskbar appears/disappears or the
 * device rotates. A window's WebView stays acquired in [com.example.camel.ui.CamelWebViewPool]
 * for as long as the window exists (open or minimised); it is released
 * only when the window is closed.
 */

/** Maximum simultaneous windows in v1. WebViews are heavy: 18 concurrent
 * windows would OOM even 12 GB of RAM, so the cap is enforced in
 * [CamelOsViewModel.openApp] with a visible message instead of silent
 * eviction. */
const val MAX_WINDOWS = 4

enum class CamelWindowState {
    /** Floating window at [CamelWindow.bounds]. */
    NORMAL,

    /** Hidden; the WebView stays acquired (paused unless it is playing
     * audio or is the recorder) and is restored on focus. */
    MINIMISED,

    /** Fills the whole desktop area; [CamelWindow.bounds] is untouched so
     * restoring returns to the previous floating position. */
    MAXIMISED
}

enum class CamelWindowSnap {
    LEFT,
    RIGHT,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

/** Bounds as fractions (0..1) of the desktop area. */
data class CamelWindowBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    fun clamped(): CamelWindowBounds {
        val w = width.coerceIn(MIN_WINDOW_WIDTH, 1f)
        val h = height.coerceIn(MIN_WINDOW_HEIGHT, 1f)
        return copy(
            x = x.coerceIn(0f, (1f - w).coerceAtLeast(0f)),
            y = y.coerceIn(0f, (1f - h).coerceAtLeast(0f)),
            width = w,
            height = h
        )
    }

    companion object {
        const val MIN_WINDOW_WIDTH = 0.25f
        const val MIN_WINDOW_HEIGHT = 0.25f
    }
}

data class CamelWindow(
    val appId: CamelAppId,
    val bounds: CamelWindowBounds,
    val z: Int,
    val state: CamelWindowState = CamelWindowState.NORMAL,
    val pinned: Boolean = false,
    /** State to restore when a minimised window is focused again
     * (NORMAL or MAXIMISED, whichever it was before minimising). */
    val stateBeforeMinimise: CamelWindowState? = null
)

/** Cascading default bounds for a newly opened window, so windows overlap
 * like a real desktop instead of stacking exactly on top of each other. */
fun cascadeBounds(index: Int): CamelWindowBounds {
    val step = index % 4
    return CamelWindowBounds(
        x = (0.06f + 0.05f * step).coerceAtMost(0.26f),
        y = (0.05f + 0.05f * step).coerceAtMost(0.25f),
        width = 0.72f,
        height = 0.76f
    )
}

/** Bounds for a snap action: 2-column halves and 2x2 quadrants. */
fun snapBounds(snap: CamelWindowSnap): CamelWindowBounds = when (snap) {
    CamelWindowSnap.LEFT -> CamelWindowBounds(0f, 0f, 0.5f, 1f)
    CamelWindowSnap.RIGHT -> CamelWindowBounds(0.5f, 0f, 0.5f, 1f)
    CamelWindowSnap.TOP_LEFT -> CamelWindowBounds(0f, 0f, 0.5f, 0.5f)
    CamelWindowSnap.TOP_RIGHT -> CamelWindowBounds(0.5f, 0f, 0.5f, 0.5f)
    CamelWindowSnap.BOTTOM_LEFT -> CamelWindowBounds(0f, 0.5f, 0.5f, 0.5f)
    CamelWindowSnap.BOTTOM_RIGHT -> CamelWindowBounds(0.5f, 0.5f, 0.5f, 0.5f)
}
