package com.example

import android.media.MediaPlayer
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ai.configureAppCheck
import com.example.camel.core.CamelOsViewModel
import com.example.camel.ui.CamelBootSplash
import com.example.camel.ui.CamelOsShell
import com.example.camel.ui.CamelPermissionWelcomeScreen
import com.example.camel.ui.CamelWebViewPool
import com.example.viewmodel.EuropassViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var bootPlayer: MediaPlayer? = null

    companion object {
        @Volatile
        private var trimCallbacksRegistered = false
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()

        if (!trimCallbacksRegistered) {
            synchronized(this) {
                if (!trimCallbacksRegistered) {
                    application.registerComponentCallbacks(object : ComponentCallbacks2 {
                        override fun onConfigurationChanged(newConfig: Configuration) {}
                        override fun onLowMemory() = CamelWebViewPool.onTrimMemory(
                            ComponentCallbacks2.TRIM_MEMORY_COMPLETE
                        )
                        override fun onTrimMemory(level: Int) = CamelWebViewPool.onTrimMemory(level)
                    })
                    trimCallbacksRegistered = true
                }
            }
        }

        // Configure Firebase App Check before any Compose UI, ViewModel, or Firebase operations.
        configureAppCheck(this, intent)

        bootPlayer = MediaPlayer.create(this, R.raw.camel_boot)?.also { player ->
            player.setOnCompletionListener {
                it.release()
                if (bootPlayer === it) bootPlayer = null
            }
            player.start()
        }

        setContent {
            var showBoot by remember { mutableStateOf(true) }

            val permissionPrefs = remember {
                getSharedPreferences("camel_os_permissions", MODE_PRIVATE)
            }
            var showPermissionSetup by remember {
                mutableStateOf(!permissionPrefs.getBoolean("completed", false))
            }

            LaunchedEffect(Unit) {
                delay(3400)
                showBoot = false
            }

            if (showBoot) {
                CamelBootSplash()
            } else if (showPermissionSetup) {
                CamelPermissionWelcomeScreen(
                    onContinue = {
                        permissionPrefs.edit()
                            .putBoolean("completed", true)
                            .apply()
                        showPermissionSetup = false
                    }
                )
            } else {
                val osViewModel: CamelOsViewModel = viewModel()
                val europassViewModel: EuropassViewModel = viewModel()
                CamelOsShell(
                    osViewModel = osViewModel,
                    europassViewModel = europassViewModel
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    override fun onDestroy() {
        bootPlayer?.release()
        bootPlayer = null
        super.onDestroy()
    }
}
