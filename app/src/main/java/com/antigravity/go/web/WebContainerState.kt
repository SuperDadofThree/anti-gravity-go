package com.antigravity.go.web

data class WebContainerState(
    val url: String = DEFAULT_URL,
    val isLoading: Boolean = true,
    val progress: Float = 0f,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isDesktopMode: Boolean = false,
    val zoomPercent: Int = 100,
    val isDevBarVisible: Boolean = true,
    val isPullToRefreshEnabled: Boolean = false,
    val errorMessage: String? = null,
    val isSettingsDialogOpen: Boolean = false
) {
    companion object {
        const val DEFAULT_URL = "https://antigravity.google.com"
        const val PREFS_NAME = "antigravity_go_prefs"
        const val KEY_SAVED_URL = "saved_url"
        const val KEY_DESKTOP_MODE = "saved_desktop_mode"
        const val KEY_DEV_BAR = "saved_dev_bar"
        const val KEY_ZOOM_PERCENT = "saved_zoom_percent"
    }
}
