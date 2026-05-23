package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ExtensionItem
import com.example.data.database.TabItem
import com.example.ui.BrowserViewModel
import java.io.ByteArrayInputStream

// List of standard tracker/ad domains to intercept & block (uBlock Origin / Privacy Armor Sim)
private val AD_TRACKER_DOMAINS = listOf(
    "doubleclick.net",
    "google-analytics.com",
    "googlesyndication.com",
    "googleadservices.com",
    "adservice.google.com",
    "pagead2.googlesyndication.com",
    "analytics.twitter.com",
    "ads-twitter.com",
    "facebook.net",
    "connect.facebook.net",
    "fbcdn.net/rsrc.php",
    "scorecardresearch.com",
    "adnxs.com",
    "hotjar.com",
    "mixpanel.com",
    "amplitude.com",
    "segment.io",
    "crazyegg.com"
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    tab: TabItem,
    viewModel: BrowserViewModel,
    extensions: List<ExtensionItem>,
    onProgressChanged: (Int) -> Unit,
    onLoadingStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Check which extensions are active
    val isAdBlockerActive = remember(extensions) {
        extensions.find { it.id == "ublock" }?.isEnabled == true
    }
    val isDarkReaderActive = remember(extensions) {
        extensions.find { it.id == "darkreader" }?.isEnabled == true
    }
    val isHttpsOnlyActive = remember(extensions) {
        extensions.find { it.id == "https_only" }?.isEnabled == true
    }
    val isPrivacyShieldActive = remember(extensions) {
        extensions.find { it.id == "private_shield" }?.isEnabled == true
    }
    val isUserAgentSwitcherActive = remember(extensions) {
        extensions.find { it.id == "user_agent_switcher" }?.isEnabled == true
    }

    // Capture the WebView instance
    val webView = remember {
        WebView(context).apply {
            // Optimize scrollbars and render pipelines to avoid unnecessary recalculation passes
            scrollBarStyle = android.view.View.SCROLLBARS_OUTSIDE_OVERLAY
            isScrollbarFadingEnabled = true

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            // High resolution rendering details configurations
            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            settings.layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.NORMAL
            settings.textZoom = 100 // Ensure crisp native pixel font density scale

            // Mixed content mode for https/http resources
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            // Standard cookie support
            CookieManager.getInstance().setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            }
        }
    }

    // Sync extension properties and User Agent configurations dynamically
    LaunchedEffect(isUserAgentSwitcherActive) {
        if (isUserAgentSwitcherActive) {
            // Firefox Mobile User Agent standard
            webView.settings.userAgentString = "Mozilla/5.0 (Android 13; Mobile; rv:119.0) Gecko/119.0 Firefox/119.0"
        } else {
            // Default native browser agent
            webView.settings.userAgentString = null
        }
    }

    // Use rememberUpdatedState to prevent stale tab closure reference captures in WebViewClient
    val currentTabState by rememberUpdatedState(tab)

    var canGoBack by remember { mutableStateOf(false) }

    // Intercept physical system back press
    BackHandler(enabled = canGoBack) {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    // Intercept VM-triggered back press (from toolbar)
    val backTrigger by viewModel.backNavigationTrigger.collectAsStateWithLifecycle()
    LaunchedEffect(backTrigger) {
        if (backTrigger > 0 && webView.canGoBack()) {
            webView.goBack()
        }
    }

    // Observe tab navigation requests
    LaunchedEffect(tab.url) {
        val currentUrl = webView.url ?: ""
        val normalizedCurrent = currentUrl.trim().removeSuffix("/")
        val normalizedTab = tab.url.trim().removeSuffix("/")
        // Prevent recursive reloading of same URL
        if (normalizedCurrent != normalizedTab && tab.url.isNotEmpty()) {
            webView.loadUrl(tab.url)
        }
    }

    // Configure client-side interceptors & filters
    LaunchedEffect(isAdBlockerActive, isDarkReaderActive, isHttpsOnlyActive, isPrivacyShieldActive, tab.id) {
        webView.webViewClient = object : WebViewClient() {
            
            // Handle page loading events
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onLoadingStateChanged(true)
                onProgressChanged(10)
                canGoBack = view?.canGoBack() ?: false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onLoadingStateChanged(false)
                onProgressChanged(100)
                canGoBack = view?.canGoBack() ?: false

                // Backprop url & title changes to our state machine
                val pageTitle = view?.title ?: currentTabState.title
                val pageUrl = url ?: currentTabState.url
                viewModel.updateTabUrlAndTitle(currentTabState.id, pageUrl, pageTitle)
                viewModel.visitPage(pageTitle, pageUrl)

                // 2. Dark Reader Inject CSS filter
                if (isDarkReaderActive) {
                    val darkCssInject = """
                        (function() {
                            var style = document.getElementById('zen-dark-reader-styles');
                            if (!style) {
                                style = document.createElement('style');
                                style.id = 'zen-dark-reader-styles';
                                style.innerHTML = "html, body, iframe, div, section, p, li, blockquote { background-color: #1a1a24 !important; color: #f0f0f5 !important; border-color: #2a2a35 !important; } a { color: #bb86fc !important; } h1, h2, h3, h4, h5, h6, b, strong, val { color: #fdfdfd !important; } img, video, canvas { filter: brightness(0.85) contrast(1.05) !important; }";
                                document.head.appendChild(style);
                            }
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(darkCssInject, null)
                } else {
                    // Try to remove styling if present
                    val lightModeTheme = """
                        (function() {
                            var style = document.getElementById('zen-dark-reader-styles');
                            if (style) {
                                style.remove();
                            }
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(lightModeTheme, null)
                }
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                canGoBack = view?.canGoBack() ?: false
            }

            // Ad blocking & tracker filtering interception (uBlock Sim & Privacy Shield combo)
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val urlString = request?.url?.toString() ?: return null
                val host = request.url?.host ?: ""

                // 1. HTTPS-Only safe redirection
                if (isHttpsOnlyActive && request.url?.scheme == "http") {
                    val upgradedUrl = request.url.buildUpon().scheme("https").build().toString()
                    // Report a secure block/upgrade
                    view?.post {
                        viewModel.incrementTrackerCount()
                    }
                    // Return redirection response
                    try {
                        val responseStream = ByteArrayInputStream("".toByteArray())
                        return WebResourceResponse("text/plain", "UTF-8", 307, "Temporary Redirect", mapOf("Location" to upgradedUrl), responseStream)
                    } catch (e: Exception) {
                        // fallback
                    }
                }

                // 2. Tracker Intercept & Blocks (uBlock / Privacy shield active)
                if (isAdBlockerActive || isPrivacyShieldActive) {
                    val isTracker = AD_TRACKER_DOMAINS.any { domain -> 
                        host.contains(domain, ignoreCase = true) || urlString.contains(domain, ignoreCase = true) 
                    }
                    if (isTracker) {
                        // Increment dynamic interface block counter
                        view?.post {
                            viewModel.incrementTrackerCount()
                        }
                        // Stop actual loading of tracker domain asset, returning empty content
                        val emptyStream = ByteArrayInputStream("".toByteArray())
                        return WebResourceResponse("text/javascript", "UTF-8", emptyStream)
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val url = uri.toString()
                
                // Allow direct navigation of links
                if (uri.scheme == "http" || uri.scheme == "https") {
                    return false
                }
                
                // Block/redirect intent protocols or non-web schemas
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressChanged(newProgress)
            }
        }
    }

    // Native layout drawing
    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}
