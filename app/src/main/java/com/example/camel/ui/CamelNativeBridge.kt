package com.example.camel.ui

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

private const val BRIDGE_TAG = "CamelNative"

typealias CamelNativeHandler = (method: String, params: JSONObject) -> JSONObject?

class CamelNativeBridge(
    private val webView: WebView,
    private val folder: String,
    private val origin: String
) {
    private val modules = mutableMapOf<String, CamelNativeHandler>()
    private var replyProxy: JavaScriptReplyProxy? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun registerModule(name: String, handler: CamelNativeHandler) {
        modules[name] = handler
    }

    fun install(): Boolean {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            Log.w(BRIDGE_TAG, "WEB_MESSAGE_LISTENER unsupported; bridge disabled for $origin")
            return false
        }
        // Expose a JavaScript interface for the page to send messages
        webView.addJavascriptInterface(CamelNativeJsInterface(this), "CamelNative")
        // Also listen for postMessage from the page
        WebViewCompat.addWebMessageListener(
            webView,
            "CamelNative",
            setOf(normalizeOrigin(origin)),
            object : WebViewCompat.WebMessageListener {
                override fun onPostMessage(
                    view: WebView,
                    message: WebMessageCompat,
                    sourceOrigin: Uri,
                    isMainFrame: Boolean,
                    replyProxy: JavaScriptReplyProxy
                ) {
                    if (!originMatches(normalizeOrigin(origin), sourceOrigin)) {
                        Log.w(BRIDGE_TAG, "Rejected bridge message from $sourceOrigin")
                        return
                    }
                    this@CamelNativeBridge.replyProxy = replyProxy
                    handleMessage(message.data)
                }
            }
        )
        return true
    }

    private fun normalizeOrigin(o: String): String = o.trim().removeSuffix("/")

    private fun originMatches(expected: String, source: Uri): Boolean {
        return normalizeOrigin(source.toString()) == expected
    }

    // Internal (not private): the file-private CamelNativeJsInterface below
    // forwards window.CamelNative.postMessage() here, and a private member is
    // visible only inside this class. Internal keeps it module-only, not
    // public API, and it is not @JavascriptInterface, so JS cannot call it
    // directly. The origin check stays on the WebMessageListener path.
    internal fun handleMessage(data: String?) {
        if (data.isNullOrBlank()) return
        try {
            val request = JSONObject(data)
            val id = request.opt("id")
            val module = request.optString("module", "")
            val method = request.optString("method", "")
            val params = request.optJSONObject("params") ?: JSONObject()
            val handler = modules[module]
            val response = JSONObject().apply {
                put("id", id)
                if (handler != null) {
                    val result = handler(method, params)
                    put("ok", true)
                    if (result != null) put("result", result)
                } else {
                    put("ok", false)
                    put("error", "Unknown module: $module")
                }
            }
            // Post reply on main thread for thread safety
            mainHandler.post { replyProxy?.postMessage(response.toString()) }
        } catch (e: Exception) {
            Log.e(BRIDGE_TAG, "Bad bridge message", e)
            mainHandler.post {
                try {
                    replyProxy?.postMessage(
                        JSONObject().put("ok", false).put("error", "Malformed request").toString()
                    )
                } catch (ignored: Exception) {
                    Log.w(BRIDGE_TAG, "Could not send error reply", ignored)
                }
            }
        }
    }

    fun postToPage(payload: JSONObject) {
        mainHandler.post { replyProxy?.postMessage(payload.toString()) }
    }

    fun close() {
        replyProxy = null
        modules.clear()
    }
}

private class CamelNativeJsInterface @JvmOverloads constructor(
    private val bridge: CamelNativeBridge
) {
    @JavascriptInterface
    fun postMessage(message: String) {
        // Called from JS via window.CamelNative.postMessage(message)
        bridge.handleMessage(message)
    }
}
