package com.example.camel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.camel.core.CamelAppDescriptor
import com.example.camel.core.CamelAppId
import com.example.camel.core.CamelAppRegistry
import com.example.camel.core.CamelOsPreferences
import com.example.camel.core.CamelWallpaperPreset
import com.example.camel.services.CamelSystemTelemetry
import com.example.ui.components.EuropassTab
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CamelDesktopDashboard(
    preferences: CamelOsPreferences,
    telemetry: CamelSystemTelemetry,
    onOpenApp: (CamelAppId, EuropassTab?) -> Unit,
    onLongClickApp: (CamelAppId) -> Unit,
    onOpenControlCenter: () -> Unit,
    onLongPressEmptySpace: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(15_000)
        }
    }

    val timeText = remember(nowMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))
    }
    val dateText = remember(nowMillis) {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(nowMillis))
    }

    val pinnedDesktopModules = remember(preferences.pinnedDesktopApps, preferences.disabledApps) {
        CamelAppRegistry.modules.filter {
            it.id.routeKey in preferences.pinnedDesktopApps &&
                it.id.routeKey !in preferences.disabledApps
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPressEmptySpace
            )
    ) {
        // Wallpaper Background Layer
        CamelWallpaperBackdrop(
            wallpaperPreset = preferences.wallpaperPreset,
            customWallpaperPath = preferences.customWallpaperPath,
            dimAlpha = preferences.wallpaperDimAlpha
        )

        // Clean KDE Plasma-style Workspace (clock/telemetry widget + ONLY user-pinned shortcuts)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 960.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (preferences.showClockOnDesktop || preferences.showTelemetryOnDesktop) {
                PlasmaDesktopHeaderWidget(
                    timeText = timeText,
                    dateText = dateText,
                    telemetry = telemetry,
                    showClock = preferences.showClockOnDesktop,
                    showTelemetry = preferences.showTelemetryOnDesktop,
                    onClickWidget = onOpenControlCenter
                )
            }

            if (pinnedDesktopModules.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 86.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pinnedDesktopModules, key = { it.id.name }) { app ->
                        PlasmaDesktopAppIcon(
                            app = app,
                            onClick = { onOpenApp(app.id, null) },
                            onLongClick = { onLongClickApp(app.id) },
                            customIconRef = preferences.customAppIcons[app.id.routeKey]
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CamelWallpaperBackdrop(
    wallpaperPreset: CamelWallpaperPreset,
    customWallpaperPath: String?,
    dimAlpha: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        val customFile = customWallpaperPath?.let { File(it) }?.takeIf { it.exists() }
        if (wallpaperPreset == CamelWallpaperPreset.CUSTOM_IMAGE && customFile != null) {
            AsyncImage(
                model = customFile,
                contentDescription = "Custom desktop wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (wallpaperPreset == CamelWallpaperPreset.GOLDEN_DUNES) {
            Image(
                painter = painterResource(id = R.drawable.img_camel_wallpaper),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(wallpaperPreset.topHex),
                                Color(wallpaperPreset.midHex),
                                Color(wallpaperPreset.bottomHex)
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = dimAlpha * 0.85f),
                            Color.Black.copy(alpha = dimAlpha * 0.55f),
                            Color.Black.copy(alpha = (dimAlpha + 0.12f).coerceAtMost(0.92f))
                        )
                    )
                )
        )
    }
}

@Composable
private fun PlasmaDesktopHeaderWidget(
    timeText: String,
    dateText: String,
    telemetry: CamelSystemTelemetry,
    showClock: Boolean,
    showTelemetry: Boolean,
    onClickWidget: () -> Unit
) {
    Surface(
        onClick = onClickWidget,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.44f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("desktop_clock_widget")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showClock) {
                Column {
                    Text(
                        text = timeText,
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (showTelemetry) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactTelemetryPill(
                        icon = if (telemetry.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
                        value = "${telemetry.batteryPercent}%"
                    )
                    CompactTelemetryPill(
                        icon = Icons.Filled.DeveloperBoard,
                        value = "${telemetry.ramPercent}%"
                    )
                    CompactTelemetryPill(
                        icon = Icons.Filled.Storage,
                        value = "${telemetry.storagePercent}%"
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactTelemetryPill(
    icon: ImageVector,
    value: String
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.52f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlasmaDesktopAppIcon(
    app: CamelAppDescriptor,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    customIconRef: String? = null
) {
    val accent = Color(app.accentHex)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag("app_icon_${app.id.routeKey}")
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                border = BorderStroke(
                    width = if (app.isIntegrated) 1.5.dp else 1.dp,
                    color = if (app.isIntegrated) accent.copy(alpha = 0.85f) else accent.copy(alpha = 0.35f)
                ),
                shadowElevation = if (app.isIntegrated) 8.dp else 2.dp,
                modifier = Modifier.size(64.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    accent.copy(alpha = 0.22f)
                                )
                            )
                        )
                ) {
                    CamelAppIcon(
                        app = app,
                        customIconRef = customIconRef,
                        modifier = Modifier.size(34.dp),
                        tint = accent
                    )
                }
            }

            if (app.isIntegrated) {
                Box(
                    modifier = Modifier
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = app.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (app.isIntegrated) FontWeight.SemiBold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
