package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PhotoStorageHelper {

    fun createCameraImageUri(context: Context): Pair<Uri, File> {
        val cacheDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(cacheDir, "capture_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return uri to file
    }

    fun persistPhotoToAppStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
            val destFile = File(photosDir, "cv_avatar_${UUID.randomUUID()}.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val decoded = BitmapFactory.decodeStream(input) ?: return null
                val scaled = scaleBitmapDown(decoded, 720)
                FileOutputStream(destFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                if (scaled !== decoded) decoded.recycle()
                scaled.recycle()
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            null
        }
    }

    fun persistFileToAppStorage(context: Context, sourceFile: File): String? {
        return persistPhotoToAppStorage(context, Uri.fromFile(sourceFile))
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}
