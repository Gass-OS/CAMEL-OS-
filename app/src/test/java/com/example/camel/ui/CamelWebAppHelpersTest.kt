package com.example.camel.ui

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CamelWebAppHelpersTest {

    @Test
    fun singleMimeTypeBecomesIntentType() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .toFileChooserIntent(arrayOf("audio/*"), allowMultiple = false)
        assertEquals("audio/*", intent.type)
        assertEquals(false, intent.hasExtra(Intent.EXTRA_MIME_TYPES))
    }

    @Test
    fun extensionsPlusMimeKeepSingleMimeType() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .toFileChooserIntent(
                arrayOf("audio/*,.mp3,.flac,.wav"),
                allowMultiple = true
            )
        assertEquals("audio/*", intent.type)
        assertEquals(true, intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
    }

    @Test
    fun multipleMimeTypesUseOctetStreamWithExtra() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .toFileChooserIntent(arrayOf("audio/*", "video/*"), allowMultiple = true)
        assertEquals("application/octet-stream", intent.type)
        val extras = intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)
        assertEquals(2, extras?.size)
        assertTrue(extras!!.contains("audio/*"))
        assertTrue(extras.contains("video/*"))
    }

    @Test
    fun chooserResultWithSingleUriIsParsed() {
        val uri = Uri.parse("content://com.android.providers.media.documents/document/audio%3A1")
        val intent = Intent().setData(uri)
        val parsed = parseChooserUris(intent, allowMultiple = false)
        assertEquals(1, parsed.size)
        assertEquals(uri, parsed[0])
    }

    @Test
    fun chooserResultClipDataHonorsMultipleFlag() {
        val first = Uri.parse("content://provider/1")
        val second = Uri.parse("content://provider/2")
        val clip = android.content.ClipData.newRawUri("files", first).apply { addItem(android.content.ClipData.Item(second)) }
        val intent = Intent().apply { setClipData(clip) }

        val single = parseChooserUris(intent, allowMultiple = false)
        assertEquals(1, single.size)

        val multiple = parseChooserUris(intent, allowMultiple = true)
        assertEquals(2, multiple.size)
    }

    @Test
    fun cancelledChooserReturnsEmptyArray() {
        assertEquals(0, parseChooserUris(null, allowMultiple = false).size)
    }
}
