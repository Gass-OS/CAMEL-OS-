package com.example.camel.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.camel.core.CamelAppDescriptor
import com.example.camel.core.CamelAppIconOverrides
import java.io.File

/**
 * The single place an app icon is rendered. A gallery-image override is
 * shown with Coil (cropped to the tile); a built-in override or no override
 * falls back to the registry [ImageVector] with the app's accent tint.
 * A missing image file falls back to the registry icon as well, so a
 * deleted file can never leave a blank tile.
 */
@Composable
fun CamelAppIcon(
    app: CamelAppDescriptor,
    customIconRef: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color(app.accentHex)
) {
    val imagePath = customIconRef
        ?.takeIf { it.startsWith("file:") }
        ?.removePrefix("file:")
        ?.takeIf { it.isNotBlank() }
    val imageFile = imagePath?.let { File(it) }?.takeIf { it.exists() }
    if (imageFile != null) {
        AsyncImage(
            model = imageFile,
            contentDescription = app.title,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(8.dp))
        )
    } else {
        val vector = if (customIconRef != null) {
            CamelAppIconOverrides.builtInIcon(
                customIconRef.removePrefix("icon:")
            ) ?: app.icon
        } else {
            app.icon
        }
        Icon(
            vector,
            contentDescription = app.title,
            tint = tint,
            modifier = modifier
        )
    }
}
