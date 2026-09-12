package com.antigravity.go.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt
import com.antigravity.go.theme.AntigravityBlue
import com.antigravity.go.util.AppLogger
import com.antigravity.go.web.AntigravityWebChromeClient
import com.antigravity.go.web.AntigravityWebViewClient
import com.antigravity.go.web.UserAgentHelper
import com.antigravity.go.web.WebContainerState
import com.antigravity.go.web.WebNavigationHelper

@Composable
fun ContainerScreen(
    initialUrl: String,
    onFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onRequestAccountPicker: (onSelected: (String) -> Unit) -> Unit,
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
    var zoomPercent by remember {
        mutableStateOf(sharedPrefs.getInt(WebContainerState.KEY_ZOOM_PERCENT, 100))
    }

    var selectedGoogleAccount by remember {
        mutableStateOf(sharedPrefs.getString(WebContainerState.KEY_SELECTED_ACCOUNT, null))
    }
    val hasPromptedAccount = remember {
        sharedPrefs.getBoolean(WebContainerState.KEY_PROMPTED_ACCOUNT, false)
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

    // Helper to log in with selected account
    fun navigateToGoogleAccount(accountEmail: String) {
        selectedGoogleAccount = accountEmail
        sharedPrefs.edit()
            .putString(WebContainerState.KEY_SELECTED_ACCOUNT, accountEmail)
            .apply()

        try {
            val encodedEmail = java.net.URLEncoder.encode(accountEmail, "UTF-8")
            val encodedContinue = java.net.URLEncoder.encode(WebContainerState.DEFAULT_URL, "UTF-8")
            val loginUrl = "https://accounts.google.com/AccountChooser?Email=$encodedEmail&continue=$encodedContinue"
            webViewInstance?.loadUrl(loginUrl)
            Toast.makeText(context, "Signing in as $accountEmail...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            webViewInstance?.loadUrl(WebContainerState.DEFAULT_URL)
        }
    }

    // Auto prompt on first launch if not yet selected
    LaunchedEffect(Unit) {
        if (!hasPromptedAccount && selectedGoogleAccount == null) {
            sharedPrefs.edit().putBoolean(WebContainerState.KEY_PROMPTED_ACCOUNT, true).apply()
            onRequestAccountPicker { email ->
                navigateToGoogleAccount(email)
            }
        }
    }

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

    var bubbleOffsetX by remember { mutableFloatStateOf(0f) }
    var bubbleOffsetY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val bubbleSizePx = with(density) { 56.dp.toPx() }
        val marginEndPx = with(density) { 20.dp.toPx() }
        val marginBottomPx = with(density) { 28.dp.toPx() }

        val minOffsetX = -(maxWidthPx - marginEndPx - bubbleSizePx)
        val maxOffsetX = marginEndPx
        val minOffsetY = -(maxHeightPx - marginBottomPx - bubbleSizePx)
        val maxOffsetY = marginBottomPx

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
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    errorMessage = null
                                    val target = webViewInstance?.url ?: currentUrl
                                    webViewInstance?.loadUrl(target)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Retry")
                            }
                            Button(
                                onClick = {
                                    val target = webViewInstance?.url ?: currentUrl
                                    WebNavigationHelper.openUrlSafely(context, target)
                                },
                                modifier = Modifier.weight(1.4f)
                            ) {
                                Text("Open in Browser")
                            }
                            Button(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Settings")
                            }
                        }
                    }
                }
            }
        }

        // Dimmed scrim when Quick Menu is open (tap to dismiss)
        if (showQuickMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showQuickMenu = false }
            )
        }

        // Enlarged Quick Control Menu (Floating Island)
        AnimatedVisibility(
            visible = showQuickMenu,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 96.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Desktop/Mobile Mode Toggle
                    QuickMenuButton(
                        icon = if (isDesktopMode) "🖥" else "📱",
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

                    // 2. Home Button
                    QuickMenuButton(
                        icon = "⌂",
                        label = "Home",
                        sublabel = "Reload",
                        onClick = {
                            showQuickMenu = false
                            webViewInstance?.loadUrl(currentUrl)
                        }
                    )

                    // 3. Open in External Browser
                    QuickMenuButton(
                        icon = "↗",
                        label = "Browser",
                        sublabel = "External",
                        onClick = {
                            showQuickMenu = false
                            val urlToOpen = webViewInstance?.url ?: currentUrl
                            WebNavigationHelper.openUrlSafely(context, urlToOpen)
                        }
                    )

                    // 4. Google Account Picker
                    QuickMenuButton(
                        icon = "👤",
                        label = if (selectedGoogleAccount != null) "Account" else "Sign In",
                        sublabel = if (selectedGoogleAccount != null) {
                            val name = selectedGoogleAccount!!.substringBefore("@")
                            if (name.length > 7) name.take(6) + "…" else name
                        } else "Google",
                        isActive = selectedGoogleAccount != null,
                        onClick = {
                            showQuickMenu = false
                            onRequestAccountPicker { email ->
                                navigateToGoogleAccount(email)
                            }
                        }
                    )

                    // 5. Settings / Endpoint Dialog
                    QuickMenuButton(
                        icon = "⚙",
                        label = "Settings",
                        sublabel = "Config",
                        onClick = {
                            showSettingsDialog = true
                            showQuickMenu = false
                        }
                    )
                }
            }
        }

        // Draggable Floating Action Bubble
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset { IntOffset(bubbleOffsetX.roundToInt(), bubbleOffsetY.roundToInt()) }
                .padding(end = 20.dp, bottom = 28.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .size(56.dp)
                .shadow(10.dp, CircleShape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var isDrag = false
                        var totalDistance = 0f
                        val touchSlop = viewConfiguration.touchSlop

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!isDrag) {
                                    showQuickMenu = !showQuickMenu
                                }
                                break
                            }
                            val delta = change.positionChange()
                            totalDistance += kotlin.math.hypot(delta.x, delta.y)
                            if (!isDrag && totalDistance > touchSlop) {
                                isDrag = true
                            }
                            if (isDrag) {
                                change.consume()
                                bubbleOffsetX = (bubbleOffsetX + delta.x).coerceIn(minOffsetX, maxOffsetX)
                                bubbleOffsetY = (bubbleOffsetY + delta.y).coerceIn(minOffsetY, maxOffsetY)
                            }
                        }
                    }
                },
            shape = CircleShape,
            color = if (showQuickMenu) AntigravityBlue else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showQuickMenu) "✕" else "✦",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (showQuickMenu) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentUrl = currentUrl,
            selectedAccount = selectedGoogleAccount,
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
            },
            onPromptAccountPicker = {
                onRequestAccountPicker { email ->
                    navigateToGoogleAccount(email)
                }
            }
        )
    }
}

@Composable
private fun QuickMenuButton(
    icon: String,
    label: String,
    sublabel: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isActive) AntigravityBlue else MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = sublabel,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
