package com.antigravity.go.web

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import com.antigravity.go.util.AppLogger

object WebNavigationHelper {
    fun openUrlSafely(context: Context, url: String, mainWebView: WebView? = null): Boolean {
        AppLogger.i("Navigation", "openUrlSafely: $url")
        return try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase() ?: ""
            val host = uri.host?.lowercase() ?: ""

            // Ignore non-navigable or pseudo schemes
            if (scheme == "javascript" || scheme == "about" || scheme == "data") {
                AppLogger.d("Navigation", "Ignored non-navigable scheme: $scheme")
                return false
            }

            // Handle intent:// schemes
            if (scheme == "intent") {
                return try {
                    val parsedIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(parsedIntent)
                    AppLogger.i("Navigation", "Successfully launched intent:// scheme")
                    true
                } catch (e: Exception) {
                    AppLogger.w("Navigation", "Failed to launch intent:// scheme: ${e.message}")
                    false
                }
            }

            val isInternalDomain = host.endsWith("antigravity.google.com") ||
                    host.endsWith("google.com") ||
                    host.endsWith("googleusercontent.com") ||
                    host.endsWith("gstatic.com") ||
                    host == "localhost" ||
                    host == "10.0.2.2" ||
                    host.startsWith("192.168.")

            if (isInternalDomain && mainWebView != null) {
                AppLogger.i("Navigation", "Loading internal URL in container WebView: $url")
                mainWebView.loadUrl(url)
                true
            } else if (scheme == "http" || scheme == "https") {
                AppLogger.i("Navigation", "Opening external URL via Custom Tabs / Browser: $url")
                try {
                    val customTabsIntent = CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    customTabsIntent.launchUrl(context, uri)
                    true
                } catch (e: Exception) {
                    AppLogger.w("Navigation", "CustomTabs failed (${e.message}), falling back to ACTION_VIEW with NEW_TASK")
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(browserIntent)
                        true
                    } catch (ex: Exception) {
                        AppLogger.e("Navigation", "Could not open browser intent: ${ex.message}", ex)
                        false
                    }
                }
            } else {
                try {
                    val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(genericIntent)
                    true
                } catch (ex: Exception) {
                    AppLogger.e("Navigation", "Could not open URI scheme '$scheme': ${ex.message}", ex)
                    false
                }
            }
        } catch (t: Throwable) {
            AppLogger.e("Navigation", "Fatal error in openUrlSafely: ${t.message}", t)
            false
        }
    }
}

class AntigravityWebChromeClient(
    private val onProgressUpdate: (Float) -> Unit,
    private val onTitleReceived: (String) -> Unit,
    private val onFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressUpdate(newProgress / 100f)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        title?.let {
            AppLogger.d("WebChromeClient", "Title: $it")
            onTitleReceived(it)
        }
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        AppLogger.i("WebChromeClient", "onShowFileChooser triggered")
        return onFileChooser(filePathCallback, fileChooserParams)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        AppLogger.i("WebChromeClient", "Granting web permissions: ${request?.resources?.joinToString()}")
        request?.grant(request.resources)
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (view == null || resultMsg == null) return false
        AppLogger.i("WebChromeClient", "onCreateWindow (isDialog=$isDialog, isUserGesture=$isUserGesture)")

        // 1. If user tapped a link with target="_blank", hitTestResult gives the target URL directly
        val hitTest = view.hitTestResult
        val directUrl = hitTest.extra
        if (!directUrl.isNullOrBlank()) {
            AppLogger.i("WebChromeClient", "onCreateWindow: direct hitTest URL resolved: $directUrl")
            WebNavigationHelper.openUrlSafely(view.context, directUrl, view)
            return false
        }

        // 2. Otherwise create a temporary WebView to capture script window.open navigation safely
        val ctx = view.context
        val tempWebView = WebView(ctx).apply {
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                    val targetUrl = request?.url?.toString()
                    if (!targetUrl.isNullOrBlank()) {
                        AppLogger.i("WebChromeClient", "onCreateWindow: captured URL from tempWebView: $targetUrl")
                        WebNavigationHelper.openUrlSafely(ctx, targetUrl, view)
                    }
                    v?.destroy()
                    return true
                }

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(v: WebView?, targetUrl: String?): Boolean {
                    if (!targetUrl.isNullOrBlank()) {
                        AppLogger.i("WebChromeClient", "onCreateWindow: captured URL from tempWebView: $targetUrl")
                        WebNavigationHelper.openUrlSafely(ctx, targetUrl, view)
                    }
                    v?.destroy()
                    return true
                }
            }
        }

        val transport = resultMsg.obj as? WebView.WebViewTransport
        if (transport != null) {
            transport.webView = tempWebView
            resultMsg.sendToTarget()
            return true
        }
        return false
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (consoleMessage != null) {
            val level = consoleMessage.messageLevel()?.name ?: "LOG"
            val msg = "${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
            AppLogger.log("WEB/$level", msg)
        }
        return super.onConsoleMessage(consoleMessage)
    }
}

class AntigravityWebViewClient(
    private val context: Context,
    private val onPageStartedCallback: (String) -> Unit,
    private val onPageFinishedCallback: (String) -> Unit,
    private val onErrorCallback: (String) -> Unit,
    private val onHistoryUpdate: (canGoBack: Boolean, canGoForward: Boolean) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        AppLogger.d("WebViewClient", "shouldOverrideUrlLoading: $url")
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase() ?: ""
        val scheme = uri.scheme?.lowercase() ?: ""

        if (scheme == "javascript" || scheme == "about" || scheme == "data") {
            return false
        }

        val isInternalDomain = host.endsWith("antigravity.google.com") ||
                host.endsWith("google.com") ||
                host.endsWith("googleusercontent.com") ||
                host.endsWith("gstatic.com") ||
                host == "localhost" ||
                host == "10.0.2.2" ||
                host.startsWith("192.168.")

        return if (isInternalDomain) {
            false
        } else {
            WebNavigationHelper.openUrlSafely(context, url, view)
            true
        }
    }

    /**
     * Critical fix: When Google Sign-In or OAuth submits a form (POST) or redirects,
     * Android's default implementation calls dontResend.sendToTarget(), causing net::ERR_CACHE_MISS!
     * By calling resend.sendToTarget(), we instruct Chromium to resend the form data.
     */
    override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
        AppLogger.i("WebViewClient", "Form resubmission handled for Google OAuth")
        resend?.sendToTarget()
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        AppLogger.i("WebViewClient", "Page started: $url")
        url?.let { onPageStartedCallback(it) }
        view?.let { onHistoryUpdate(it.canGoBack(), it.canGoForward()) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        AppLogger.i("WebViewClient", "Page finished: $url")
        url?.let { onPageFinishedCallback(it) }
        view?.let { onHistoryUpdate(it.canGoBack(), it.canGoForward()) }

        // Flush cookies to disk so OAuth session persists
        CookieManager.getInstance().flush()

        // Inject meta viewport fix if page lacks a mobile viewport tag
        view?.evaluateJavascript(
            """
            (function() {
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';
                    document.getElementsByTagName('head')[0].appendChild(meta);
                }
            })();
            """.trimIndent(),
            null
        )
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val desc = error?.description?.toString() ?: ""
            val code = error?.errorCode ?: 0
            val failingUrl = request.url?.toString() ?: ""
            AppLogger.e("WebViewClient", "onReceivedError (code=$code): $desc for $failingUrl")
            onErrorCallback(desc.ifEmpty { "Network error connecting to Antigravity" })
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        val status = errorResponse?.statusCode ?: 0
        val failingUrl = request?.url?.toString() ?: ""
        if (request?.isForMainFrame == true || status >= 400) {
            AppLogger.w("WebViewClient", "HTTP Error $status for $failingUrl")
        }
    }
}
