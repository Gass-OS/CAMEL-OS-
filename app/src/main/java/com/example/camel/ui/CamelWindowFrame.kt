package com.example.camel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.camel.core.CamelAppDescriptor
import com.example.camel.core.CamelAppId
import com.example.camel.core.CamelAppRegistry
import com.example.camel.core.CamelWindow
import com.example.camel.core.CamelWindowSnap
import com.example.camel.core.CamelWindowState

/**
 * Multi-window desktop chrome (Termux/XFCE-style multitasking).
 *
 * A window is a floating [Surface] with a title bar (icon, title, menu,
 * minimise / maximise / close), a drag-to-move title bar, a double-tap to
 * maximise/restore, and a bottom-right resize grip. The content is the
 * existing module rendering ([CamelWebApp] or the Europass screen) hosted
 * unchanged inside the window. Minimising hides the window without
 * destroying its WebView: the content leaves composition, the pool keeps
 * the WebView acquired (paused unless it is playing audio or recording),
 * and focusing the window restores it with its state intact.
 *
 * Zoom in v1 is maximise/restore: there is no separate content-zoom
 * control. Pin (always-on-top) is a title-bar menu toggle.
 */

/** Observe pointer downs without consuming them, so tapping a window
 * focuses it while the tap still reaches the content (e.g. the WebView). */
private fun Modifier.focusOnDown(onFocus: () -> Unit): Modifier = this.pointerInput(onFocus) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.any { it.changedToDown() }) onFocus()
        }
    }
}

@Composable
fun CamelWindowFrame(
    window: CamelWindow,
    descriptor: CamelAppDescriptor,
    isFocused: Boolean,
    desktopWidthPx: Float,
    desktopHeightPx: Float,
    customIconRef: String?,
    onFocus: () -> Unit,
    onMove: (dxFrac: Float, dyFrac: Float) -> Unit,
    onResize: (dwFrac: Float, dhFrac: Float) -> Unit,
    onMinimise: () -> Unit,
    onToggleMaximise: () -> Unit,
    onClose: () -> Unit,
    onSnap: (CamelWindowSnap) -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isMaximised = window.state == CamelWindowState.MAXIMISED

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = if (isFocused) 12.dp else 4.dp,
        shape = RoundedCornerShape(if (isMaximised) 0.dp else 12.dp),
        border = BorderStroke(
            width = if (isFocused) 1.5.dp else 1.dp,
            color = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        modifier = modifier.focusOnDown(onFocus)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Title bar: drag to move, double-tap to maximise/restore.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(
                        if (isFocused) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        }
                    )
                    .pointerInput(window.appId, window.state) {
                        detectTapGestures(onDoubleTap = { onToggleMaximise() })
                    }
                    .pointerInput(window.appId, window.state) {
                        detectDragGestures(
                            onDragStart = { onFocus() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (!isMaximised) {
                                    onMove(
                                        dragAmount.x / desktopWidthPx,
                                        dragAmount.y / desktopHeightPx
                                    )
                                }
                            }
                        )
                    }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CamelAppIcon(
                    app = descriptor,
                    customIconRef = customIconRef,
                    modifier = Modifier.size(22.dp),
                    tint = Color(descriptor.accentHex)
                )
                Text(
                    text = descriptor.title,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Window menu",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    CamelWindowMenu(
                        expanded = menuExpanded,
                        pinned = window.pinned,
                        onDismiss = { menuExpanded = false },
                        onSnap = onSnap,
                        onTogglePin = onTogglePin,
                        onClose = onClose
                    )
                }
                IconButton(
                    onClick = onMinimise,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "Minimise",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onToggleMaximise,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (isMaximised) Icons.Filled.FilterNone else Icons.Filled.CropSquare,
                        contentDescription = if (isMaximised) "Restore" else "Maximise",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Content area. The resize grip floats over the bottom-right
            // corner; it is hidden while maximised (nothing to resize).
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                content()
                if (!isMaximised) {
                    ResizeGrip(
                        onResize = onResize,
                        desktopWidthPx = desktopWidthPx,
                        desktopHeightPx = desktopHeightPx,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun CamelWindowMenu(
    expanded: Boolean,
    pinned: Boolean,
    onDismiss: () -> Unit,
    onSnap: (CamelWindowSnap) -> Unit,
    onTogglePin: () -> Unit,
    onClose: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Snap left") },
            onClick = { onSnap(CamelWindowSnap.LEFT); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Snap right") },
            onClick = { onSnap(CamelWindowSnap.RIGHT); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Snap top-left") },
            onClick = { onSnap(CamelWindowSnap.TOP_LEFT); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Snap top-right") },
            onClick = { onSnap(CamelWindowSnap.TOP_RIGHT); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Snap bottom-left") },
            onClick = { onSnap(CamelWindowSnap.BOTTOM_LEFT); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Snap bottom-right") },
            onClick = { onSnap(CamelWindowSnap.BOTTOM_RIGHT); onDismiss() }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(if (pinned) "Unpin from top" else "Pin on top") },
            leadingIcon = {
                if (pinned) Icon(Icons.Filled.Check, contentDescription = null)
            },
            onClick = { onTogglePin(); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Close window") },
            onClick = { onClose(); onDismiss() }
        )
    }
}

/** Bottom-right corner resize grip: three diagonal lines. Dragging it
 * resizes the window; it never overlaps the content's own controls for
 * long because it only covers a 28 dp corner. */
@Composable
private fun ResizeGrip(
    onResize: (dwFrac: Float, dhFrac: Float) -> Unit,
    desktopWidthPx: Float,
    desktopHeightPx: Float,
    modifier: Modifier = Modifier
) {
    val gripColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
    Canvas(
        modifier = modifier
            .size(28.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onResize(dragAmount.x / desktopWidthPx, dragAmount.y / desktopHeightPx)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val step = 7.dp.toPx()
        val margin = 3.dp.toPx()
        val stroke = 2.dp.toPx()
        for (i in 0..2) {
            val o = i * step
            drawLine(
                color = gripColor,
                start = Offset(w - o - margin, h - margin),
                end = Offset(w - margin, h - o - margin),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Bottom taskbar: one item per open window (including minimised ones).
 * Tap an item to focus its window — restoring it if it was minimised.
 * The focused window is marked with a primary bar under its item.
 */
@Composable
fun CamelTaskbar(
    windows: List<CamelWindow>,
    focusedAppId: CamelAppId?,
    customAppIcons: Map<String, String>,
    onSelect: (CamelAppId) -> Unit,
    onClose: (CamelAppId) -> Unit,
    onShowDesktop: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Same surface treatment as the top panel by default; the top-panel
    // colour/alpha settings are deliberately not forced onto it.
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val ordered = windows.sortedBy { it.z }
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(ordered, key = { it.appId }) { window ->
                    val descriptor = CamelAppRegistry.get(window.appId)
                    val focused = window.appId == focusedAppId
                    Column {
                        Surface(
                            onClick = { onSelect(window.appId) },
                            color = if (focused) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (focused) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                }
                            ),
                            modifier = Modifier.testTag("taskbar_${window.appId.routeKey}")
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    end = 4.dp,
                                    top = 4.dp,
                                    bottom = 4.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CamelAppIcon(
                                    app = descriptor,
                                    customIconRef = customAppIcons[window.appId.routeKey],
                                    modifier = Modifier.size(22.dp),
                                    tint = Color(descriptor.accentHex)
                                )
                                Text(
                                    text = descriptor.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 110.dp)
                                )
                                IconButton(
                                    onClick = { onClose(window.appId) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Close ${descriptor.title}",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        // Focused marker: a primary bar under the item.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(
                                    if (focused) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    }
                                )
                        )
                    }
                }
            }
            VerticalDivider(
                modifier = Modifier
                    .height(36.dp)
                    .padding(horizontal = 2.dp)
            )
            IconButton(
                onClick = onShowDesktop,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.DesktopWindows,
                    contentDescription = "Show desktop",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
