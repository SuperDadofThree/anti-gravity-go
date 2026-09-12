package com.antigravity.go.web

import android.webkit.WebSettings

object UserAgentHelper {
    // Clean, modern Chrome for Android user agent without WebView markers (no '; wv', no 'Version/4.0')
    // This allows Google OAuth (accounts.google.com) to succeed without '403: disallowed_useragent'
    const val CHROME_MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    // Clean, modern Desktop Chrome user agent for Desktop Mode toggle
    const val CHROME_DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    fun applyUserAgent(settings: WebSettings, desktopMode: Boolean) {
        if (desktopMode) {
            settings.userAgentString = CHROME_DESKTOP_UA
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        } else {
            settings.userAgentString = CHROME_MOBILE_UA
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        }
    }
}
