package com.antigravity.go.web

import android.webkit.WebSettings

object UserAgentHelper {
    // A clean, modern Chrome for Android user agent without WebView markers (no '; wv', no 'Version/4.0')
    // This allows Google OAuth (accounts.google.com) to succeed without '403: disallowed_useragent'
    const val CHROME_MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 14; Mobile; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    // A clean, modern Desktop Chrome user agent for Desktop Mode toggle
    const val CHROME_DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    fun sanitizeUserAgent(originalUA: String?): String {
        if (originalUA.isNullOrBlank()) return CHROME_MOBILE_UA
        // Strip out Android WebView indicators that Google OAuth detects
        return originalUA
            .replace("; wv", "")
            .replace(Regex("Version/\\d+\\.\\d+\\s?"), "")
    }

    fun applyUserAgent(settings: WebSettings, desktopMode: Boolean) {
        if (desktopMode) {
            settings.userAgentString = CHROME_DESKTOP_UA
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        } else {
            val defaultUa = WebSettings.getDefaultUserAgent(null)
            settings.userAgentString = sanitizeUserAgent(defaultUa)
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        }
    }
}
