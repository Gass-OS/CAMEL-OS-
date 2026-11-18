package com.example.camel.ui

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.ScriptHandler
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

private const val BRIDGE_TAG = "CamelNative"

data class CamelMediaInfo(
    val folder: String,
    val origin: String,
    val playing: Boolean,
    val title: String,
    val artist: String,
    val position: Double,
    val duration: Double
)

object CamelMediaBus {
    private val _info = MutableStateFlow<CamelMediaInfo?>(null)
    val info: StateFlow<CamelMediaInfo?> = _info.asStateFlow()

    fun publish(info: CamelMediaInfo?) {
        _info.value = info
    }
}

typealias CamelNativeHandler = (method: String, params: JSONObject) -> JSONObject?

class CamelNativeBridge(
    private val webView: WebView,
    private val folder: String,
    private val origin: String
) {
    private val modules = mutableMapOf<String, CamelNativeHandler>()
    private var scriptHandler: ScriptHandler? = null
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
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            scriptHandler = WebViewCompat.addDocumentStartJavaScript(
                webView,
                CAMEL_MEDIA_OBSERVER_JS,
                setOf("https://*.camelos.local")
            )
        } else {
            Log.w(BRIDGE_TAG, "DOCUMENT_START_SCRIPT unsupported; media observer disabled")
        }
        return true
    }

    private fun normalizeOrigin(o: String): String = o.trim().removeSuffix("/")

    private fun originMatches(expected: String, source: Uri): Boolean {
        return normalizeOrigin(source.toString()) == expected
    }

    private fun handleMessage(data: String?) {
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
        scriptHandler?.remove()
        scriptHandler = null
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

private val CAMEL_MEDIA_OBSERVER_JS = """
(function() {
  if (window.__camelMediaObserverInstalled) return;
  window.__camelMediaObserverInstalled = true;
  var tracked = new Set();
  var pausedByCamel = null;

  function text(v) { return typeof v === 'string' ? v : ''; }

  function report() {
    var playing = false, title = '', artist = '', position = 0, duration = 0, active = null;
    tracked.forEach(function(el) {
      if (!el.paused && !el.ended) { playing = true; if (!active) active = el; }
    });
    if (active) {
      title = text(active.getAttribute('data-camel-title') || active.title || active.getAttribute('title'));
      artist = text(active.getAttribute('data-camel-artist'));
      position = Number(active.currentTime) || 0;
      duration = Number(active.duration) || 0;
      if (!isFinite(duration)) duration = 0;
    }
    try {
      if (!playing && navigator.mediaSession && navigator.mediaSession.playbackState === 'playing') playing = true;
    } catch (e) {}
    if (window.CamelNative && window.CamelNative.postMessage) {
      window.CamelNative.postMessage(JSON.stringify({
        id: 'media-' + Date.now(),
        module: 'media',
        method: 'state',
        params: { playing: playing, title: title, artist: artist, position: position, duration: duration }
      }));
    }
  }

  function track(el) {
    if (!el || tracked.has(el)) return;
    tracked.add(el);
    ['play','pause','ended','timeupdate','loadedmetadata'].forEach(function(evt) {
      el.addEventListener(evt, report, true);
    });
  }

  var originalPlay = HTMLMediaElement.prototype.play;
  HTMLMediaElement.prototype.play = function() {
    track(this);
    try { return originalPlay.apply(this, arguments); }
    finally { setTimeout(report, 0); }
  };
  var originalPause = HTMLMediaElement.prototype.pause;
  HTMLMediaElement.prototype.pause = function() {
    try { return originalPause.apply(this, arguments); }
    finally { setTimeout(report, 0); }
  };

  var observer = new MutationObserver(function(mutations) {
    mutations.forEach(function(m) {
      m.addedNodes.forEach(function(n) {
        if (n.nodeType === 1) {
          if (n.tagName === 'AUDIO' || n.tagName === 'VIDEO') track(n);
          n.querySelectorAll && n.querySelectorAll('audio,video').forEach(track);
        }
      });
    });
  });
  observer.observe(document.documentElement || document, { childList: true, subtree: true });
  document.querySelectorAll('audio,video').forEach(track);

  window.__camelMediaControl = function(action, value) {
    var target = null;
    tracked.forEach(function(el) {
      if (!el.paused && !el.ended) target = target || el;
    });
    if (!target) tracked.forEach(function(el) { target = target || el; });
    if (!target) return false;
    switch (action) {
      case 'toggle':
        if (target.paused) { target.play(); } else { target.pause(); }
        return true;
      case 'mute':
        target.muted = !!value;
        return true;
      case 'seek':
        if (isFinite(target.duration)) target.currentTime = Math.max(0, Math.min(target.duration, Number(value) || 0));
        return true;
      default: return false;
    }
  };

  document.addEventListener('visibilitychange', report, true);
  setInterval(report, 5000);
})();
""".trimIndent()