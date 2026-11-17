package com.example.camel.ui

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CamelWebApp(folder: String, onClose: () -> Unit) {
    val context = LocalContext.current
    var web by remember { mutableStateOf<WebView?>(null) }
    var pendingFile by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingWebPermission by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingGeo by remember {
        mutableStateOf<Pair<String, GeolocationPermissions.Callback>?>(null)
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        pendingWebPermission?.let { request ->
            val granted = request.resources.filter { resource ->
                when (resource) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                        results[Manifest.permission.CAMERA]
                            ?: (context.checkSelfPermission(Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED)
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                        results[Manifest.permission.RECORD_AUDIO]
                            ?: (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                                PackageManager.PERMISSION_GRANTED)
                    else -> false
                }
            }
            if (granted.isNotEmpty()) request.grant(granted.toTypedArray())
            else request.deny()
            pendingWebPermission = null
        }

        pendingGeo?.let { (origin, callback) ->
            val fine = context.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = context.checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            callback.invoke(origin, fine || coarse, false)
            pendingGeo = null
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        pendingFile?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(res.resultCode, res.data)
        )
        pendingFile = null
    }


    val host = remember(folder) { "${folder.lowercase().replace("_", "-")}.camelos.local" }
    val loader = remember(folder) {
        val assets = WebViewAssetLoader.AssetsPathHandler(context)
        WebViewAssetLoader.Builder()
            .setDomain(host)
            .addPathHandler("/") { path -> assets.handle("apps/$folder/$path") }
            .build()
    }

    BackHandler {
        val w = web
        if (w != null && w.canGoBack()) w.goBack() else onClose()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.allowFileAccess = false
                settings.setGeolocationEnabled(true)

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView, request: WebResourceRequest
                    ): WebResourceResponse? {
                        if (request.url.path?.endsWith("registerSW.js") == true)
                            return WebResourceResponse("application/javascript", "utf-8", "".byteInputStream())
                        val response = loader.shouldInterceptRequest(request.url)
                        Log.d(
                            "CamelWebView",
                            "RESOURCE ${request.url} -> ${response?.mimeType ?: "NOT INTERCEPTED"}"
                        )
                        return response
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        Log.e(
                            "CamelWebView",
                            "LOAD ERROR ${request.url}: ${error.errorCode} ${error.description}"
                        )
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        response: WebResourceResponse
                    ) {
                        Log.e(
                            "CamelWebView",
                            "HTTP ERROR ${response.statusCode} ${request.url}"
                        )
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        Log.d("CamelWebView", "PAGE FINISHED $url")

                        view.evaluateJavascript(
                            """(function() {
                                const root = document.getElementById('root');
                                return JSON.stringify({
                                    url: location.href,
                                    title: document.title,
                                    ready: document.readyState,
                                    rootExists: !!root,
                                    rootLength: root ? root.innerHTML.length : -1,
                                    rootText: root ? root.innerText.substring(0, 300) : '',
                                    bodyLength: document.body.innerHTML.length,
                                    bodyWidth: document.body.getBoundingClientRect().width,
                                    bodyHeight: document.body.getBoundingClientRect().height,
                                    rootDisplay: root ? getComputedStyle(root).display : '',
                                    rootVisibility: root ? getComputedStyle(root).visibility : '',
                                    bodyBackground: getComputedStyle(document.body).backgroundColor
                                });
                            })()""".trimIndent()
                        ) { result ->
                            Log.d("CamelWebView", "DOM INSPECTION: $result")
                            view.evaluateJavascript(
                                """
                                (function() {
                                  const root = document.getElementById("root");
                                  const html = document.documentElement;
                                  const body = document.body;
                                  const r = root ? root.getBoundingClientRect() : null;
                                  const first = root ? root.firstElementChild : null;
                                  const f = first ? first.getBoundingClientRect() : null;
                                  return JSON.stringify({
                                    viewport: innerWidth + "x" + innerHeight,
                                    screen: screen.width + "x" + screen.height,
                                    html: { height: html.getBoundingClientRect().height, scrollHeight: html.scrollHeight, overflow: getComputedStyle(html).overflow },
                                    body: { height: body.getBoundingClientRect().height, scrollHeight: body.scrollHeight, overflow: getComputedStyle(body).overflow },
                                    root: r ? { width: r.width, height: r.height, top: r.top, left: r.left, overflow: getComputedStyle(root).overflow } : null,
                                    firstChild: f ? { width: f.width, height: f.height, top: f.top, left: f.left, display: getComputedStyle(first).display } : null
                                  });
                                })()
                                """.trimIndent()
                            ) { result ->
                                Log.d("CamelWebView", "LAYOUT INSPECTION: $result")
                            }
                                
                                // CAMEL_VIEWPORT_REPAIR
                                view.evaluateJavascript(
                                    """
                                    (function() {
                                      let style = document.getElementById("camel-viewport-fix");
                                      if (!style) {
                                        style = document.createElement("style");
                                        style.id = "camel-viewport-fix";
                                        style.textContent = "html, body { height: 100% !important; min-height: 100% !important; } body { width: 100% !important; margin: 0 !important; } #root { width: 100% !important; height: 100% !important; min-height: 100vh !important; } #root > * { min-height: 100vh !important; }";
                                        document.head.appendChild(style);
                                      }
                                      // CAMEL_FIXED_ROOT_REPAIR
                                      const html = document.documentElement;
                                      const body = document.body;
                                      const root = document.getElementById("root");
                                      html.style.setProperty("height", "100vh", "important");
                                      html.style.setProperty("min-height", "100vh", "important");
                                      body.style.setProperty("position", "fixed", "important");
                                      body.style.setProperty("inset", "0", "important");
                                      body.style.setProperty("width", "100vw", "important");
                                      body.style.setProperty("height", "100vh", "important");
                                      body.style.setProperty("margin", "0", "important");
                                      if (root) {
                                        root.style.setProperty("position", "fixed", "important");
                                        root.style.setProperty("inset", "0", "important");
                                        root.style.setProperty("width", "100vw", "important");
                                        root.style.setProperty("height", "100vh", "important");
                                        root.style.setProperty("min-height", "100vh", "important");
                                        root.style.setProperty("overflow", "auto", "important");
                                        if (root.firstElementChild) {
                                          root.firstElementChild.style.setProperty("min-height", "100vh", "important");
                                          root.firstElementChild.style.setProperty("height", "100vh", "important");
                                        }
                                      }
                                      window.dispatchEvent(new Event("resize"));
                                      return JSON.stringify({
                                        htmlHeight: document.documentElement.getBoundingClientRect().height,
                                        bodyHeight: document.body.getBoundingClientRect().height,
                                        rootHeight: document.getElementById("root")?.getBoundingClientRect().height
                                      });
                                    })()
                                    """.trimIndent()
                                ) { result ->
                                    Log.d("CamelWebView", "VIEWPORT FIX RESULT: $result")
    view.evaluateJavascript(
        """
        (function() {
          const root = document.getElementById("root");
          const first = root?.firstElementChild;
          const parent = root?.parentElement;
          const info = el => {
            if (!el) return null;
            const c = getComputedStyle(el);
            const r = el.getBoundingClientRect();
            return {
              tag: el.tagName,
              id: el.id,
              className: String(el.className || ""),
              rect: {width:r.width,height:r.height,top:r.top,left:r.left},
              offsetHeight:el.offsetHeight,
              clientHeight:el.clientHeight,
              display:c.display,
              position:c.position,
              height:c.height,
              minHeight:c.minHeight,
              flex:c.flex,
              alignItems:c.alignItems,
              cssText:el.style.cssText
            };
          };
          return JSON.stringify({
            html:info(document.documentElement),
            body:info(document.body),
            parent:info(parent),
            root:info(root),
            firstChild:info(first)
          });
        })()
        """.trimIndent()
    ) { details ->
        Log.d("CamelWebView", "ROOT STYLE DETAILS: $details")

    // CAMEL_CSS_FORENSICS
    view.evaluateJavascript(
        """
        (function() {
          const probe = document.createElement("div");
          probe.id = "camel-viewport-probe";
          probe.style.cssText = "position:fixed!important;top:0!important;left:0!important;width:20px!important;height:100vh!important;min-height:100vh!important;display:block!important;visibility:hidden!important;pointer-events:none!important;";
          document.body.appendChild(probe);

          const rules = [];
          for (const sheet of document.styleSheets) {
            try {
              for (const rule of sheet.cssRules) {
                if (/(#root|100dvh|100vh|html|body)/i.test(rule.cssText)) {
                  rules.push(rule.cssText.substring(0, 500));
                }
              }
            } catch (e) {
              rules.push("UNREADABLE STYLESHEET: " + e.message);
            }
          }

          const r = document.getElementById("root");
          const m = document.querySelector("main");
          const pr = probe.getBoundingClientRect();
          return JSON.stringify({
            innerHeight: innerHeight,
            visualViewportHeight: visualViewport ? visualViewport.height : null,
            vhProbe: {height:pr.height,top:pr.top},
            rootHeight:r ? r.getBoundingClientRect().height : null,
            mainHeight:m ? m.getBoundingClientRect().height : null,
            rules:rules.slice(0,80)
          });
        })()
        """.trimIndent()
    ) { result ->
        Log.d("CamelWebView", "CAMEL CSS FORENSICS: $result")
    }
        
        // CAMEL_PIXEL_VIEWPORT_REPAIR
        view.evaluateJavascript(
            """
            (function() {
              function applyCamelViewport() {
                const h = Math.max(1, window.visualViewport ? window.visualViewport.height : window.innerHeight);
                const w = Math.max(1, window.visualViewport ? window.visualViewport.width : window.innerWidth);
                const height = h + "px";
                const width = w + "px";
                const html = document.documentElement;
                const body = document.body;
                const root = document.getElementById("root");
                html.style.setProperty("height", height, "important");
                html.style.setProperty("min-height", height, "important");
                body.style.setProperty("position", "fixed", "important");
                body.style.setProperty("top", "0px", "important");
                body.style.setProperty("left", "0px", "important");
                body.style.setProperty("width", width, "important");
                body.style.setProperty("height", height, "important");
                if (root) {
                  root.style.setProperty("position", "fixed", "important");
                  root.style.setProperty("top", "0px", "important");
                  root.style.setProperty("left", "0px", "important");
                  root.style.setProperty("width", width, "important");
                  root.style.setProperty("height", height, "important");
                  root.style.setProperty("min-height", height, "important");
                  root.style.setProperty("overflow", "auto", "important");
                  const app = root.firstElementChild;
                  if (app) {
                    app.style.setProperty("height", height, "important");
                    app.style.setProperty("min-height", height, "important");
                    app.style.setProperty("width", "100%", "important");
                  }
                }
                return JSON.stringify({viewportHeight:h, appliedHeight:height, html:html.getBoundingClientRect().height, body:body.getBoundingClientRect().height, root:root ? root.getBoundingClientRect().height : -1, app:root && root.firstElementChild ? root.firstElementChild.getBoundingClientRect().height : -1});
              }
              window.__camelViewportResize = applyCamelViewport;
              window.addEventListener("resize", applyCamelViewport);
              if (window.visualViewport) window.visualViewport.addEventListener("resize", applyCamelViewport);
              return applyCamelViewport();
            })()
            """.trimIndent()
        ) { result ->
            Log.d("CamelWebView", "CAMEL PIXEL REPAIR: $result")
        }

    }

                                }
                        }
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        Log.e(
                            "CamelWebView",
                            "JS ${message.message()} at ${message.sourceId()}:${message.lineNumber()}"
                        )
                        return true
                    }
                    override fun onPermissionRequest(request: PermissionRequest) {
                        if (request.origin.host != host) {
                            Log.w("CamelWebView", "Rejected permission origin: ${request.origin}")
                            request.deny()
                            return
                        }
                        val required = request.resources.mapNotNull { resource ->
                            when (resource) {
                                PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                                    Manifest.permission.CAMERA
                                PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                                    Manifest.permission.RECORD_AUDIO
                                else -> null
                            }
                        }.distinct()

                        if (required.isEmpty()) {
                            request.deny()
                            return
                        }

                        val missing = required.filter {
                            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
                        }

                        if (missing.isEmpty()) {
                            val granted = request.resources.filter {
                                it == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                                    it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
                            }
                            request.grant(granted.toTypedArray())
                            return
                        }

                        pendingGeo?.let { (origin, callback) ->
                            callback.invoke(origin, false, false)
                            pendingGeo = null
                        }
                        pendingWebPermission?.deny()
                        pendingWebPermission = request
                        permLauncher.launch(missing.toTypedArray())
                    }

                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String,
                        callback: GeolocationPermissions.Callback
                    ) {
                        if (Uri.parse(origin).host != host) {
                            Log.w("CamelWebView", "Rejected location origin: $origin")
                            callback.invoke(origin, false, false)
                            return
                        }

                        val fine = context.checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val coarse = context.checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (fine || coarse) {
                            callback.invoke(origin, true, false)
                            return
                        }

                        pendingWebPermission?.deny()
                        pendingWebPermission = null
                        pendingGeo?.let { (oldOrigin, oldCallback) ->
                            oldCallback.invoke(oldOrigin, false, false)
                        }
                        pendingGeo = origin to callback
                        permLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }

                    override fun onShowFileChooser(
                        v: WebView,
                        cb: ValueCallback<Array<Uri>>,
                        params: FileChooserParams
                    ): Boolean {
                        pendingFile?.onReceiveValue(null)
                        pendingFile = cb

                        try {
                            val acceptedTypes = params.acceptTypes
                                .flatMap { it.split(",") }
                                .map { it.trim().lowercase() }
                                .filter { it.contains("/") && it != "*/*" }
                                .distinct()

                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = acceptedTypes.singleOrNull() ?: "*/*"

                                if (acceptedTypes.size > 1) {
                                    putExtra(
                                        Intent.EXTRA_MIME_TYPES,
                                        acceptedTypes.toTypedArray()
                                    )
                                }

                                putExtra(
                                    Intent.EXTRA_ALLOW_MULTIPLE,
                                    params.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                                )
                            }

                            Log.d(
                                "CamelWebView",
                                "FILE PICKER types=$acceptedTypes multiple=${params.mode == FileChooserParams.MODE_OPEN_MULTIPLE}"
                            )

                            fileLauncher.launch(intent)
                        } catch (e: Exception) {
                            Log.e("CamelWebView", "FILE PICKER ERROR", e)
                            pendingFile?.onReceiveValue(null)
                            pendingFile = null
                        }

                        return true
                    }
                }
                loadUrl("https://$host/index.html")
                web = this
            }
        },
        onRelease = {
            pendingFile?.onReceiveValue(null)
            pendingFile = null
            pendingWebPermission?.deny()
            pendingWebPermission = null
            pendingGeo?.let { (origin, callback) ->
                callback.invoke(origin, false, false)
            }
            pendingGeo = null
            it.destroy()
        }
    )
}
