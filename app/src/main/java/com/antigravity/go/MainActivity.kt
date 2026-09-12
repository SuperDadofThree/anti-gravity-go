package com.antigravity.go

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import com.antigravity.go.theme.AntigravityGoTheme
import com.antigravity.go.ui.PwaLaunchScreen
import com.antigravity.go.web.PwaLauncherHelper
import com.antigravity.go.web.WebContainerState

class MainActivity : ComponentActivity() {

    private var customTabsClient: CustomTabsClient? = null
    private var customTabsConnection: CustomTabsServiceConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedPrefs = getSharedPreferences(WebContainerState.PREFS_NAME, MODE_PRIVATE)
        val defaultUrl = intent?.data?.toString() ?: sharedPrefs.getString(
            WebContainerState.KEY_SAVED_URL,
            WebContainerState.DEFAULT_URL
        ) ?: WebContainerState.DEFAULT_URL

        // Pre-warm Chrome connection for fast launch
        setupCustomTabsWarmup()

        // Auto-launch the PWA on cold startup
        if (savedInstanceState == null) {
            PwaLauncherHelper.launchPwa(this, defaultUrl)
        }

        setContent {
            AntigravityGoTheme {
                PwaLaunchScreen(
                    currentUrl = defaultUrl,
                    onLaunchUrl = { url ->
                        PwaLauncherHelper.launchPwa(this@MainActivity, url)
                    }
                )
            }
        }
    }

    private fun setupCustomTabsWarmup() {
        customTabsConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                customTabsClient = client
                client.warmup(0)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                customTabsClient = null
            }
        }
        customTabsConnection?.let {
            PwaLauncherHelper.warmupChrome(this, it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        customTabsConnection?.let {
            try {
                unbindService(it)
            } catch (_: Exception) {}
        }
    }
}
