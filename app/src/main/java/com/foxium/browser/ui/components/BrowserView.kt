package com.foxium.browser.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.foxium.browser.adblock.TrackerBlocker
import com.foxium.browser.data.TabModel
import com.foxium.browser.ui.theme.FirefoxOrange
import com.foxium.browser.ui.theme.FirefoxPrivatePurple

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserView(
    tab: TabModel,
    isDesktopMode: Boolean,
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onProgressChange: (Int) -> Unit,
    onNavigationStateChange: (canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    webViewInstance: (WebView) -> Unit
) {
    var currentProgress by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        cacheMode = if (tab.isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                    }

                    if (tab.isPrivate) {
                        clearCache(true)
                        clearHistory()
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val requestUrl = request?.url?.toString() ?: ""
                            if (TrackerBlocker.shouldBlock(requestUrl)) {
                                return TrackerBlocker.createEmptyResponse()
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            url?.let { onUrlChange(it) }
                            onNavigationStateChange(view?.canGoBack() == true, view?.canGoForward() == true)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            currentProgress = 100
                            url?.let { onUrlChange(it) }
                            view?.title?.let { onTitleChange(it) }
                            onNavigationStateChange(view?.canGoBack() == true, view?.canGoForward() == true)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            currentProgress = newProgress
                            onProgressChange(newProgress)
                            if (newProgress >= 100) {
                                isLoading = false
                            }
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { onTitleChange(it) }
                        }
                    }

                    webViewInstance(this)

                    if (tab.url.isNotBlank() && !tab.isHome) {
                        loadUrl(tab.url)
                    }
                }
            },
            update = { webView ->
                // Update Desktop mode user agent
                val defaultUa = webView.settings.userAgentString
                if (isDesktopMode) {
                    webView.settings.userAgentString =
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    webView.settings.useWideViewPort = true
                    webView.settings.loadWithOverviewMode = true
                }

                if (tab.url.isNotBlank() && !tab.isHome && webView.url != tab.url) {
                    webView.loadUrl(tab.url)
                }
            }
        )

        // Firefox slim progress indicator at top of page content
        if (isLoading && currentProgress < 100) {
            LinearProgressIndicator(
                progress = currentProgress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.TopCenter),
                color = if (tab.isPrivate) FirefoxPrivatePurple else FirefoxOrange
            )
        }
    }
}
