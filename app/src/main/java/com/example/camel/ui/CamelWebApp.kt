package com.example.camel.ui

import android.Manifest
import android.app.Application
import android.annotation.SuppressLint
import android.content.Context
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.MutableContextWrapper
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import org.json.JSONObject
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader

private const val TAG = "CamelWebView"

private data class WebPermissionJob(
    val origin: String,
    val request: PermissionRequest? = null,
    val geoCallback: GeolocationPermissions.Callback? = null
)

private data class FileChooserJob(
    val callback: ValueCallback<Array<Uri>>,
    val allowMultiple: Boolean
)

private fun Context.hasPermission(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private fun PermissionRequest.supportedResources(): Array<String> =
    resources.filter {
        it == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
            it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
    }.toTypedArray()

private fun permissionForResource(resource: String): String? = when (resource) {
    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
    else -> null
}

private val VIEWPORT_REPAIR_JS = """
(function() {
  function apply() {
    var h = Math.max(1, window.visualViewport ? window.visualViewport.height : window.innerHeight);
    var w = Math.max(1, window.visualViewport ? window.visualViewport.width : window.innerWidth);
    var height = h + 'px', width = w + 'px';
    var html = document.documentElement, body = document.body, root = document.getElementById('root');
    html.style.setProperty('height', height, 'important');
    html.style.setProperty('min-height', height, 'important');
    body.style.setProperty('position', 'fixed', 'important');
    body.style.setProperty('top', '0px', 'important');
    body.style.setProperty('left', '0px', 'important');
    body.style.setProperty('width', width, 'important');
    body.style.setProperty('height', height, 'important');
    body.style.setProperty('margin', '0', 'important');
    if (root) {
      root.style.setProperty('position', 'fixed', 'important');
      root.style.setProperty('top', '0px', 'important');
      root.style.setProperty('left', '0px', 'important');
      root.style.setProperty('width', width, 'important');
      root.style.setProperty('height', height, 'important');
      root.style.setProperty('min-height', height, 'important');
      root.style.setProperty('overflow', 'auto', 'important');
      var app = root.firstElementChild;
      if (app) {
        app.style.setProperty('height', height, 'important');
        app.style.setProperty('min-height', height, 'important');
        app.style.setProperty('width', '100%', 'important');
      }
    }
  }
  window.__camelViewportResize = apply;
  window.addEventListener('resize', apply);
  if (window.visualViewport) window.visualViewport.addEventListener('resize', apply);
  apply();
})();
""".trimIndent()

private fun applyViewportRepair(view: WebView) {
    view.evaluateJavascript(VIEWPORT_REPAIR_JS, null)
}

private val AUDIO_STATE_JS = """
(function() {
  if (window.__camelAudioHooked) return;
  window.__camelAudioHooked = true;
  function report() {
    var playing = false;
    document.querySelectorAll('audio,video').forEach(function(el) {
      if (!el.paused && !el.ended && el.currentTime > 0) playing = true;
    });
    if (window.CamelAudioState) window.CamelAudioState.setPlaying(playing);
  }
  document.addEventListener('play', report, true);
  document.addEventListener('pause', report, true);
  document.addEventListener('ended', report, true);
})();
""".trimIndent()

internal fun parseChooserUris(data: Intent?, allowMultiple: Boolean): Array<Uri> {
    if (data == null) return emptyArray()
    val uris = mutableListOf<Uri>()
    data.clipData?.let { clip ->
        for (i in 0 until clip.itemCount) {
            clip.getItemAt(i).uri?.let(uris::add)
        }
    }
    data.data?.let(uris::add)
    val distinct = uris.distinct()
    return (if (allowMultiple) distinct else distinct.take(1)).toTypedArray()
}

internal fun Intent.toFileChooserIntent(acceptTypes: Array<String>, allowMultiple: Boolean): Intent {
    val mimeTypes = acceptTypes
        .asSequence()
        .flatMap { it.split(",").asSequence() }
        .map { it.trim().lowercase() }
        .filter { it.contains("/") }
        .distinct()
        .toList()
    val hasExplicitExtensions = acceptTypes.any { it.trim().startsWith(".") }

    type = when {
        mimeTypes.size == 1 -> mimeTypes.first()
        mimeTypes.isNotEmpty() -> "application/octet-stream"
        hasExplicitExtensions -> "application/octet-stream"
        else -> "*/*"
    }
    if (mimeTypes.size > 1) {
        putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
    }
    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    return this
}

class CamelWebViewController(
    private val appContext: Context,
    private val host: String,
    private val loader: WebViewAssetLoader
) {
    private val context = MutableContextWrapper(appContext)
    private var activePermission: WebPermissionJob? = null
    private var activeFileChooser: FileChooserJob? = null

    @Volatile
    var playingAudio: Boolean = false
        private set

    var attached: Boolean = false
        private set

    lateinit var view: WebView
        private set

    var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    var fileLauncher: ActivityResultLauncher<Intent>? = null
    var onBack: () -> Unit = {}

    fun attach(activity: Context) {
        attached = true
        context.setBaseContext(activity)
    }

    fun detach() {
        attached = false
        context.setBaseContext(appContext)
    }

    private inner class CamelAudioState {
        @JavascriptInterface
        fun setPlaying(playing: Boolean) {
            playingAudio = playing
        }
    }

    fun onPermissionResult(results: Map<String, Boolean>) {
        val job = activePermission ?: return
        activePermission = null

        if (job.request != null) {
            val granted = job.request.supportedResources().filter { resource ->
                val androidPermission = permissionForResource(resource)
                androidPermission == null ||
                    results[androidPermission] == true ||
                    context.hasPermission(androidPermission)
            }
            if (granted.isNotEmpty()) {
                job.request.grant(granted.toTypedArray())
            } else {
                job.request.deny()
                notifyPermissionDenied(job.origin)
            }
        }
        job.geoCallback?.let { callback ->
            val allowed = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            callback.invoke(job.origin, allowed, false)
        }
    }

    fun onFileResult(resultCode: Int, data: Intent?) {
        val chooser = activeFileChooser ?: return
        activeFileChooser = null
        val uris = parseChooserUris(data, chooser.allowMultiple)
        if (uris.isEmpty()) {
            Log.d(TAG, "FILE CHOOSER cancelled resultCode=$resultCode")
        } else {
            for (uri in uris) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }.onFailure { Log.d(TAG, "URI permission not persistable: $uri") }
            }
        }
        chooser.callback.onReceiveValue(uris)
    }

    fun cancelPending() {
        activePermission?.let { job ->
            job.request?.deny()
            job.geoCallback?.invoke(job.origin, false, false)
        }
        activePermission = null
        activeFileChooser?.callback?.onReceiveValue(null)
        activeFileChooser = null
    }

    private fun notifyPermissionDenied(origin: String) {
        view.evaluateJavascript(
            "window.dispatchEvent(new CustomEvent('camel-permission-denied'," +
                "{detail:{origin:${JSONObject.quote(origin)}}}));",
            null
        )
    }

    fun create(): WebView {
        view = WebView(context).apply {
            addJavascriptInterface(CamelAudioState(), "CamelAudioState")
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = false
                allowContentAccess = false
                setGeolocationEnabled(true)
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    if (request.url.path?.endsWith("registerSW.js") == true) {
                        return WebResourceResponse(
                            "application/javascript", "utf-8", "".byteInputStream()
                        )
                    }
                    return loader.shouldInterceptRequest(request.url)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    Log.e(TAG, "LOAD ERROR ${request.url}: ${error.errorCode} ${error.description}")
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    response: WebResourceResponse
                ) {
                    Log.e(TAG, "HTTP ERROR ${response.statusCode} ${request.url}")
                }

                override fun onPageFinished(view: WebView, url: String) {
                    Log.d(TAG, "PAGE FINISHED $url")
                    applyViewportRepair(view)
                    view.evaluateJavascript(AUDIO_STATE_JS, null)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    Log.d(TAG, "JS ${message.message()} at ${message.sourceId()}:${message.lineNumber()}")
                    return true
                }

                override fun onPermissionRequest(request: PermissionRequest) {
                    val origin = request.origin?.toString().orEmpty()
                    if (Uri.parse(origin).host != host) {
                        Log.w(TAG, "Rejected permission origin: $origin")
                        request.deny()
                        return
                    }
                    val supported = request.supportedResources()
                    if (supported.isEmpty()) {
                        request.deny()
                        return
                    }
                    val missing = supported.mapNotNull(::permissionForResource)
                        .distinct()
                        .filterNot(context::hasPermission)

                    if (missing.isEmpty()) {
                        request.grant(supported)
                        return
                    }

                    cancelPending()
                    activePermission = WebPermissionJob(origin = origin, request = request)
                    permissionLauncher?.launch(missing.toTypedArray()) ?: run {
                        activePermission = null
                        request.deny()
                        Log.e(TAG, "Permission launcher not registered yet")
                    }
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String,
                    callback: GeolocationPermissions.Callback
                ) {
                    if (Uri.parse(origin).host != host) {
                        Log.w(TAG, "Rejected location origin: $origin")
                        callback.invoke(origin, false, false)
                        return
                    }
                    if (context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    ) {
                        callback.invoke(origin, true, false)
                        return
                    }

                    cancelPending()
                    activePermission = WebPermissionJob(origin = origin, geoCallback = callback)
                    permissionLauncher?.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                        ?: run {
                            activePermission = null
                            callback.invoke(origin, false, false)
                            Log.e(TAG, "Permission launcher not registered yet")
                        }
                }

                override fun onShowFileChooser(
                    webView: WebView,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams
                ): Boolean {
                    activeFileChooser?.callback?.onReceiveValue(null)
                    val allowMultiple = fileChooserParams.mode ==
                        FileChooserParams.MODE_OPEN_MULTIPLE
                    activeFileChooser = FileChooserJob(filePathCallback, allowMultiple)
                    return try {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).toFileChooserIntent(
                            fileChooserParams.acceptTypes,
                            allowMultiple
                        )
                        Log.d(
                            TAG,
                            "FILE PICKER types=${fileChooserParams.acceptTypes.toList()} " +
                                "multiple=$allowMultiple"
                        )
                        fileLauncher?.launch(intent) ?: run {
                            activeFileChooser = null
                            filePathCallback.onReceiveValue(null)
                            Log.e(TAG, "File launcher not registered yet")
                        }
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "FILE PICKER ERROR", e)
                        activeFileChooser = null
                        filePathCallback.onReceiveValue(null)
                        true
                    }
                }
            }
            setOnKeyListener { _, keyCode, event ->
                keyCode == android.view.KeyEvent.KEYCODE_BACK &&
                    event.action == android.view.KeyEvent.ACTION_DOWN && canGoBack()
            }
            loadUrl("https://$host/index.html")
        }
        return view
    }

    fun destroy() {
        cancelPending()
        view.stopLoading()
        view.destroy()
    }
}

object CamelWebViewPool {
    private data class Entry(val controller: CamelWebViewController, val host: String)

    private val entries = mutableMapOf<String, Entry>()
    private var trimLevel: Int = ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW

    fun onTrimMemory(level: Int) {
        trimLevel = level
        if (level < ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) return
        val victims = synchronized(this) {
            entries.filter { (_, entry) ->
                !entry.controller.attached && !entry.controller.playingAudio
            }.map { it.key }
        }
        if (victims.isEmpty()) return
        Log.w(TAG, "TRIM_MEMORY level=$level releasing idle WebViews: $victims")
        victims.forEach(::discard)
    }

    @Synchronized
    fun suspendIdle(folder: String) {
        val controller = entries[folder]?.controller ?: return
        if (controller.attached || controller.playingAudio) return
        // Intentionally no pauseTimers/onPause: those silently stop audio playback.
        // Idle WebViews are released only on explicit close or memory pressure.
    }

    @Synchronized
    fun acquire(
        folder: String,
        host: String,
        context: Context,
        loader: WebViewAssetLoader
    ): CamelWebViewController {
        val existing = entries[folder]
        if (existing != null && existing.host == host) return existing.controller
        existing?.controller?.destroy()
        val controller = CamelWebViewController(context, host, loader).also { it.create() }
        entries[folder] = Entry(controller, host)
        return controller
    }

    @Synchronized
    fun discard(folder: String) {
        entries.remove(folder)?.controller?.let { controller ->
            controller.view.post { controller.destroy() }
        }
    }
}

fun releaseCamelWebView(folder: String) = CamelWebViewPool.discard(folder)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CamelWebApp(folder: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val appContext = remember { context.applicationContext }
    val host = remember(folder) { "${folder.lowercase().replace("_", "-")}.camelos.local" }
    val loader = remember(folder) {
        val assets = WebViewAssetLoader.AssetsPathHandler(context)
        WebViewAssetLoader.Builder()
            .setDomain(host)
            .addPathHandler("/vendor/") { path -> assets.handle("vendor/$path") }
            .addPathHandler("/") { path -> assets.handle("apps/$folder/$path") }
            .build()
    }

    val controller = remember(folder, host) {
        CamelWebViewPool.acquire(folder, host, appContext, loader)
    }
    if (context is android.app.Activity) controller.attach(context)
    controller.onBack = onClose

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        controller.onPermissionResult(results)
    }
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        controller.onFileResult(result.resultCode, result.data)
    }

    DisposableEffect(controller) {
        controller.permissionLauncher = permissionLauncher
        controller.fileLauncher = fileLauncher
        onDispose {
            controller.detach()
            if (controller.permissionLauncher === permissionLauncher) controller.permissionLauncher = null
            if (controller.fileLauncher === fileLauncher) controller.fileLauncher = null
        }
    }

    BackHandler {
        val w = controller.view
        if (w.canGoBack()) w.goBack() else onClose()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { controller.view },
        onRelease = {
            // WebViews are pooled for the lifetime of the process. Detaching from the
            // Compose hierarchy during navigation must never destroy native state.
        }
    )
}
