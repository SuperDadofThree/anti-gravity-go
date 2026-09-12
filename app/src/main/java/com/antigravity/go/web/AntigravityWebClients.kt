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
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent

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
        title?.let { onTitleReceived(it) }
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onFileChooser(filePathCallback, fileChooserParams)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        // Grant permissions for camera/mic/protected media if requested by the web app
        request?.grant(request.resources)
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        // Handle window.open by redirecting to the main WebView instance
        val transport = resultMsg?.obj as? WebView.WebViewTransport
        transport?.webView = view
        resultMsg?.sendToTarget()
        return true
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
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
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase() ?: ""

        // Domains that should stay within the Antigravity container
        val isInternalDomain = host.endsWith("antigravity.google.com") ||
                host.endsWith("google.com") ||
                host.endsWith("googleusercontent.com") ||
                host.endsWith("gstatic.com") ||
                host == "localhost" ||
                host == "10.0.2.2" ||
                host.startsWith("192.168.")

        return if (isInternalDomain) {
            // Stay inside web container
            false
        } else {
            // Open external links (e.g. GitHub repos, external docs, third-party OAuth) via Chrome Custom Tab
            try {
                val customTabsIntent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                customTabsIntent.launchUrl(context, uri)
                true
            } catch (e: Exception) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                    true
                } catch (ex: Exception) {
                    false
                }
            }
        }
    }

    /**
     * Critical fix: When Google Sign-In or OAuth submits a form (POST) or redirects,
     * Android's default implementation calls dontResend.sendToTarget(), causing net::ERR_CACHE_MISS!
     * By calling resend.sendToTarget(), we instruct Chromium to resend the form data.
     */
    override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
        resend?.sendToTarget()
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { onPageStartedCallback(it) }
        view?.let { onHistoryUpdate(it.canGoBack(), it.canGoForward()) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
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
            // Gracefully recover from ERR_CACHE_MISS by directly issuing a fresh GET request to the target URL
            if (desc.contains("ERR_CACHE_MISS", ignoreCase = true)) {
                view?.post {
                    view.loadUrl(request.url.toString())
                }
                return
            }
            onErrorCallback(desc.ifEmpty { "Network error connecting to Antigravity" })
        }
    }
}
