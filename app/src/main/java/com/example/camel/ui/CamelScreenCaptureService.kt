package com.example.camel.ui

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import java.io.File

class CamelScreenCaptureService : Service() {

    companion object {
        const val ACTION_START = "com.example.camel.CAPTURE_START"
        const val ACTION_PAUSE = "com.example.camel.CAPTURE_PAUSE"
        const val ACTION_RESUME = "com.example.camel.CAPTURE_RESUME"
        const val ACTION_STOP = "com.example.camel.CAPTURE_STOP"

        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_TOKEN = "capture_token"
        const val EXTRA_WIDTH = "capture_width"
        const val EXTRA_HEIGHT = "capture_height"
        const val EXTRA_FPS = "capture_fps"
        const val EXTRA_BITRATE = "capture_bitrate"

        private const val CHANNEL = "camel_screen_capture"
        private const val NOTIFICATION_ID = 7312
        private const val PREFS = "camel_screen_capture_state"

        fun state(context: Context) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private var projection: MediaProjection? = null
    private var recorder: MediaRecorder? = null
    private var display: VirtualDisplay? = null
    private var output: File? = null
    private var token: String = ""
    private var startedAt = 0L
    private var stopped = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCapture(intent)
            ACTION_PAUSE -> pauseCapture()
            ACTION_RESUME -> resumeCapture()
            ACTION_STOP -> finishCapture()
        }
        return START_NOT_STICKY
    }

    private fun startCapture(intent: Intent) {
        try {
            token = intent.getStringExtra(EXTRA_TOKEN) ?: ""
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)

            @Suppress("DEPRECATION")
            val resultData = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
            } else {
                intent.getParcelableExtra(EXTRA_RESULT_DATA)
            }

            if (token.isBlank() || resultData == null || resultCode != Activity.RESULT_OK) {
                saveState("error", "Screen capture permission was not granted")
                stopSelf()
                return
            }

            createNotificationChannel()

            val notification = Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle("CAMEL-OS Screen Recorder")
                .setContentText("Screen recording is active")
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            val manager = getSystemService(MediaProjectionManager::class.java)
            projection = manager.getMediaProjection(resultCode, resultData)
                ?: throw IllegalStateException("MediaProjection unavailable")

            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    finishCapture(stopProjection = false)
                }
            }, Handler(Looper.getMainLooper()))

            val metrics: DisplayMetrics = resources.displayMetrics
            val width = (metrics.widthPixels and -2).coerceAtLeast(2)
            val height = (metrics.heightPixels and -2).coerceAtLeast(2)
            val density = metrics.densityDpi

            val fps = intent.getIntExtra(EXTRA_FPS, 30).coerceIn(24, 60)
            val requestedBitrate = intent.getIntExtra(EXTRA_BITRATE, 8_000_000)
            val bitrate = requestedBitrate.coerceIn(2_000_000, 30_000_000)

            val folder = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: filesDir
            if (!folder.exists()) folder.mkdirs()

            output = File(folder, "CAMEL-Screen-$token.mp4")

            recorder = if (Build.VERSION.SDK_INT >= 31) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder?.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(output!!.absolutePath)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(width, height)
                setVideoFrameRate(fps)
                setVideoEncodingBitRate(bitrate)
                prepare()
            }

            display = projection?.createVirtualDisplay(
                "CAMEL-OS Screen Capture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder!!.surface,
                null,
                Handler(Looper.getMainLooper())
            )

            recorder?.start()
            startedAt = SystemClock.elapsedRealtime()
            stopped = false

            state(this).edit()
                .putString("token", token)
                .putString("status", "recording")
                .putString("path", output!!.absolutePath)
                .putInt("width", width)
                .putInt("height", height)
                .putInt("fps", fps)
                .putLong("duration", 0L)
                .apply()

        } catch (e: Exception) {
            saveState("error", e.message ?: "Unable to start screen capture")
            releaseResources()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun pauseCapture() {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                recorder?.pause()
                saveState("paused")
            }
        } catch (e: Exception) {
            saveState("error", e.message ?: "Pause failed")
        }
    }

    private fun resumeCapture() {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                recorder?.resume()
                saveState("recording")
            }
        } catch (e: Exception) {
            saveState("error", e.message ?: "Resume failed")
        }
    }

    private fun finishCapture(stopProjection: Boolean = true) {
        if (stopped) return
        stopped = true

        val duration = if (startedAt > 0L) {
            SystemClock.elapsedRealtime() - startedAt
        } else 0L

        var success = false
        try {
            recorder?.stop()
            success = output?.exists() == true && (output?.length() ?: 0L) > 0L
        } catch (_: Exception) {
            output?.delete()
        }

        releaseResources()

        if (stopProjection) {
            try {
                projection?.stop()
            } catch (_: Exception) {
            }
        }

        if (success) {
            state(this).edit()
                .putString("status", "completed")
                .putLong("duration", duration)
                .putLong("size", output?.length() ?: 0L)
                .apply()
        } else {
            saveState("error", "Recording was too short or could not be finalized")
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseResources() {
        try { display?.release() } catch (_: Exception) {}
        display = null

        try { recorder?.reset() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null

        projection = null
    }

    private fun saveState(status: String, error: String? = null) {
        state(this).edit()
            .putString("token", token)
            .putString("status", status)
            .putString("error", error ?: "")
            .apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL,
                "CAMEL-OS Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        if (!stopped && recorder != null) {
            finishCapture()
        }
        super.onDestroy()
    }
}
