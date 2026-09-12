package com.antigravity.go.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.antigravity.go.theme.AntigravityBlue
import com.antigravity.go.web.AntigravityWebChromeClient
import com.antigravity.go.web.AntigravityWebViewClient
import com.antigravity.go.web.UserAgentHelper
import com.antigravity.go.web.WebContainerState

@Composable
fun ContainerScreen(
    initialUrl: String,
    onFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences(WebContainerState.PREFS_NAME, Context.MODE_PRIVATE) }

    var currentUrl by remember {
        mutableStateOf(sharedPrefs.getString(WebContainerState.KEY_SAVED_URL, initialUrl) ?: initialUrl)
    }
    var isDesktopMode by remember {
        mutableStateOf(sharedPrefs.getBoolean(WebContainerState.KEY_DESKTOP_MODE, false))
    }
    var isDevBarVisible by remember {
        mutableStateOf(sharedPrefs.getBoolean(WebContainerState.KEY_DEV_BAR, true))
    }
    var zoomPercent by remember {
        mutableStateOf(sharedPrefs.getInt(WebContainerState.KEY_ZOOM_PERCENT, 100))
    }

    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showQuickMenu by remember { mutableStateOf(false) }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Back handling: navigates web history or double-press to exit
    BackHandler {
        val wv = webViewInstance
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                onExitApp()
            } else {
                lastBackPressTime = now
                Toast.makeText(context, "Press back again to exit Antigravity Go", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Web View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Enable Hardware Acceleration & Viewport
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    isFocusable = true
                    isFocusableInTouchMode = true

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        allowFileAccess = true
                        allowContentAccess = true
                        mediaPlaybackRequiresUserGesture = false
                        textZoom = zoomPercent
                    }

                    // Setup User Agent
                    UserAgentHelper.applyUserAgent(settings, isDesktopMode)

                    // Cookies
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    // Download Manager integration
                    setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                        try {
                            val request = DownloadManager.Request(Uri.parse(url)).apply {
                                val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
                                setTitle(filename)
                                setDescription("Downloading file from Antigravity...")
                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                            }
                            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                            Toast.makeText(ctx, "Download started...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }

                    webChromeClient = AntigravityWebChromeClient(
                        onProgressUpdate = { p ->
                            progress = p
                            isLoading = p < 1.0f
                        },
                        onTitleReceived = { /* Can set window title */ },
                        onFileChooser = onFileChooser
                    )

                    webViewClient = AntigravityWebViewClient(
                        context = ctx,
                        onPageStartedCallback = {
                            isLoading = true
                            errorMessage = null
                        },
                        onPageFinishedCallback = {
                            isLoading = false
                        },
                        onErrorCallback = { err ->
                            errorMessage = err
                            isLoading = false
                        },
                        onHistoryUpdate = { back, fwd ->
                            canGoBack = back
                            canGoForward = fwd
                        }
                    )

                    loadUrl(currentUrl)
                    webViewInstance = this
                }
            },
            update = { wv ->
                webViewInstance = wv
            }
        )

        // Loading Progress Bar at top
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = AntigravityBlue,
                trackColor = Color.Transparent
            )
        }

        // Error State Card (when connection fails)
        errorMessage?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Connection Notice",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    errorMessage = null
                                    webViewInstance?.reload()
                                }
                            ) {
                                Text("Retry")
                            }
                            Button(
                                onClick = { showSettingsDialog = true }
                            ) {
                                Text("Settings")
                            }
                        }
                    }
                }
            }
        }

        // Bottom UI Section: Developer Bar & Floating Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Quick Control Mini-Bar (Floating Island)
            AnimatedVisibility(
                visible = showQuickMenu,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Desktop/Mobile Mode Toggle
                        QuickMenuButton(
                            label = if (isDesktopMode) "Desktop" else "Mobile",
                            sublabel = "Mode",
                            isActive = isDesktopMode,
                            onClick = {
                                isDesktopMode = !isDesktopMode
                                sharedPrefs.edit().putBoolean(WebContainerState.KEY_DESKTOP_MODE, isDesktopMode).apply()
                                webViewInstance?.let { wv ->
                                    UserAgentHelper.applyUserAgent(wv.settings, isDesktopMode)
                                    wv.reload()
                                }
                            }
                        )

                        // Zoom Minus
                        QuickMenuButton(
                            label = "-",
                            sublabel = "Zoom",
                            onClick = {
                                if (zoomPercent > 60) {
                                    zoomPercent -= 15
                                    sharedPrefs.edit().putInt(WebContainerState.KEY_ZOOM_PERCENT, zoomPercent).apply()
                                    webViewInstance?.settings?.textZoom = zoomPercent
                                }
                            }
                        )

                        // Current Zoom / Reset
                        QuickMenuButton(
                            label = "$zoomPercent%",
                            sublabel = "Reset",
                            onClick = {
                                zoomPercent = 100
                                sharedPrefs.edit().putInt(WebContainerState.KEY_ZOOM_PERCENT, zoomPercent).apply()
                                webViewInstance?.settings?.textZoom = 100
                            }
                        )

                        // Zoom Plus
                        QuickMenuButton(
                            label = "+",
                            sublabel = "Zoom",
                            onClick = {
                                if (zoomPercent < 200) {
                                    zoomPercent += 15
                                    sharedPrefs.edit().putInt(WebContainerState.KEY_ZOOM_PERCENT, zoomPercent).apply()
                                    webViewInstance?.settings?.textZoom = zoomPercent
                                }
                            }
                        )

                        // Toggle Dev Soft Key Bar
                        QuickMenuButton(
                            label = "</>",
                            sublabel = "Keys",
                            isActive = isDevBarVisible,
                            onClick = {
                                isDevBarVisible = !isDevBarVisible
                                sharedPrefs.edit().putBoolean(WebContainerState.KEY_DEV_BAR, isDevBarVisible).apply()
                            }
                        )

                        // Settings / URL Dialog
                        QuickMenuButton(
                            label = "⚙",
                            sublabel = "Config",
                            onClick = {
                                showSettingsDialog = true
                                showQuickMenu = false
                            }
                        )
                    }
                }
            }

            // Developer Soft Keys Row (Esc, Tab, Ctrl, Alt, Braces, Arrows)
            AnimatedVisibility(
                visible = isDevBarVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                DeveloperKeyBar(webView = webViewInstance)
            }
        }

        // Floating Action Trigger (Menu Button bottom right)
        FloatingActionButton(
            onClick = { showQuickMenu = !showQuickMenu },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = if (isDevBarVisible) 56.dp else 24.dp
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .size(44.dp),
            shape = CircleShape,
            containerColor = if (showQuickMenu) AntigravityBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            contentColor = if (showQuickMenu) Color.White else MaterialTheme.colorScheme.onSurface,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Text(
                text = if (showQuickMenu) "✕" else "✦",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentUrl = currentUrl,
            onDismiss = { showSettingsDialog = false },
            onSaveUrl = { newUrl ->
                currentUrl = newUrl
                sharedPrefs.edit().putString(WebContainerState.KEY_SAVED_URL, newUrl).apply()
                webViewInstance?.loadUrl(newUrl)
            },
            onClearCache = {
                webViewInstance?.clearCache(true)
                CookieManager.getInstance().removeAllCookies(null)
                webViewInstance?.reload()
                Toast.makeText(context, "Cache and cookies cleared", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun QuickMenuButton(
    label: String,
    sublabel: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isActive) AntigravityBlue else MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = sublabel,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
