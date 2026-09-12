package com.antigravity.go.web

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection

object PwaLauncherHelper {

    private const val CHROME_PACKAGE = "com.android.chrome"
    private const val ANTIGRAVITY_DARK_COLOR = 0xFF12141A.toInt()

    fun launchPwa(context: Context, url: String) {
        try {
            val colorSchemeParams = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ANTIGRAVITY_DARK_COLOR)
                .setNavigationBarColor(ANTIGRAVITY_DARK_COLOR)
                .setSecondaryToolbarColor(ANTIGRAVITY_DARK_COLOR)
                .build()

            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setUrlBarHidingEnabled(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
                .setDefaultColorSchemeParams(colorSchemeParams)
                .build()

            val uri = Uri.parse(url)

            // Prefer Chrome for shared Google account authentication
            val pm = context.packageManager
            val chromeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                `package` = CHROME_PACKAGE
            }

            if (chromeIntent.resolveActivity(pm) != null) {
                customTabsIntent.intent.`package` = CHROME_PACKAGE
            }

            customTabsIntent.launchUrl(context, uri)
        } catch (e: Exception) {
            // Fallback to standard view intent
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not launch PWA: ${ex.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun warmupChrome(context: Context, connection: CustomTabsServiceConnection) {
        try {
            CustomTabsClient.bindCustomTabsService(context, CHROME_PACKAGE, connection)
        } catch (_: Exception) {
            // Chrome might not be present, ignore
        }
    }
}
