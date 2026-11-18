package com.example.camel.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.camel.core.CamelAppRegistry
import com.example.camel.core.folderToAppId
import com.example.camel.ui.controlCamelMedia

@Composable
fun CamelDynamicIsland(onOpenApp: (String) -> Unit) {
    val media by CamelMediaBus.info.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = media != null,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
    ) {
        val info = media ?: return@AnimatedVisibility
        val descriptor = remember(info.folder) {
            folderToAppId(info.folder)?.let { CamelAppRegistry.get(it) }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (info.playing) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (info.playing) "Playing" else "Paused",
                        tint = Color(0xFFE8BD63),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = info.title.ifBlank { descriptor?.title ?: "Media" },
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (info.artist.isNotBlank()) {
                            Text(
                                text = info.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = { controlCamelMedia(info.folder, "toggle") }) {
                        Icon(
                            imageVector = if (info.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/pause",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (expanded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { controlCamelMedia(info.folder, "mute", true) }) {
                            Icon(Icons.Filled.VolumeOff, contentDescription = "Mute", modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "${formatSeconds(info.position)} / ${formatSeconds(info.duration)}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { controlCamelMedia(info.folder, "seek", info.position + 10) }) {
                            Icon(Icons.Filled.FastForward, contentDescription = "Seek +10s", modifier = Modifier.size(18.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1f))
                        if (descriptor != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { onOpenApp(info.folder); expanded = false }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(descriptor.icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Open ${descriptor.title}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSeconds(value: Double): String {
    val total = value.toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
