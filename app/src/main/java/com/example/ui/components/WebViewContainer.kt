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

private val webViewCache = mutableMapOf<Int, WebView>()

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    tab: TabItem,
    viewModel: BrowserViewModel,
    extensions: List<ExtensionItem>,
    onProgressChanged: (Int) -> Unit,
    onLoadingStateChanged: (Boolean) -> Unit,
    onScrollStateChanged: (Boolean) -> Unit,
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
    val currentIsAdBlockerActive by rememberUpdatedState(isAdBlockerActive)
    val currentIsDarkReaderActive by rememberUpdatedState(isDarkReaderActive)
    val currentIsHttpsOnlyActive by rememberUpdatedState(isHttpsOnlyActive)
    val currentIsPrivacyShieldActive by rememberUpdatedState(isPrivacyShieldActive)
    val currentTabStateUpdated by rememberUpdatedState(tab)
    val currentViewModel by rememberUpdatedState(viewModel)

    var lastCommandedUrl by remember(tab.id) { mutableStateOf("") }

    val webView = remember(tab.id) {
        webViewCache.getOrPut(tab.id) {
            WebView(context).apply {
                scrollBarStyle = android.view.View.SCROLLBARS_OUTSIDE_OVERLAY
                isScrollbarFadingEnabled = true

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false

                settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                settings.layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.NORMAL
                settings.textZoom = 100

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    settings.offscreenPreRaster = true
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }

                CookieManager.getInstance().setAcceptCookie(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                }
            }
        }
    }

    // Bind webViewClient & webChromeClient dynamically inside composition lifecycle
    LaunchedEffect(webView, isAdBlockerActive, isDarkReaderActive, isHttpsOnlyActive, isPrivacyShieldActive, onScrollStateChanged) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener { _, _, _, _, oldScrollY ->
                val scrollY = webView.scrollY
                if (scrollY > oldScrollY + 24) {
                    onScrollStateChanged(false) // Scroll down -> hide
                } else if (scrollY < oldScrollY - 24) {
                    onScrollStateChanged(true)  // Scroll up -> show
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onLoadingStateChanged(true)
                onProgressChanged(10)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onLoadingStateChanged(false)
                onProgressChanged(100)

                val activeTabInfo = currentTabStateUpdated
                val pageTitle = view?.title ?: activeTabInfo.title
                val pageUrl = url ?: activeTabInfo.url
                currentViewModel.updateTabUrlAndTitle(activeTabInfo.id, pageUrl, pageTitle)
                currentViewModel.visitPage(pageTitle, pageUrl)

                if (currentIsDarkReaderActive) {
                    val darkCssInject = """
                        (function() {
                            var style = document.getElementById('zen-dark-reader-styles');
                            if (!style) {
                                style = document.createElement('style');
                                style.id = 'zen-dark-reader-styles';
                                style.innerHTML = "html, body, iframe, div, section, p, li, blockquote { background-color: #050609 !important; color: #f1f5f9 !important; border-color: #1e2235 !important; } a { color: #60a5fa !important; } h1, h2, h3, h4, h5, h6, b, strong, val { color: #ffffff !important; } img, video, canvas { filter: brightness(0.85) contrast(1.05) !important; }";
                                document.head.appendChild(style);
                            }
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(darkCssInject, null)
                } else {
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

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val urlString = request?.url?.toString() ?: return null
                val host = request.url?.host ?: ""

                if (currentIsHttpsOnlyActive && request.url?.scheme == "http") {
                    val upgradedUrl = request.url.buildUpon().scheme("https").build().toString()
                    currentViewModel.incrementTrackerCount()
                    try {
                        val responseStream = ByteArrayInputStream("".toByteArray())
                        return WebResourceResponse("text/plain", "UTF-8", 307, "Temporary Redirect", mapOf("Location" to upgradedUrl), responseStream)
                    } catch (e: Exception) {}
                }

                if (currentIsAdBlockerActive || currentIsPrivacyShieldActive) {
                    val isTracker = AD_TRACKER_DOMAINS.any { domain ->
                        host.contains(domain, ignoreCase = true) || urlString.contains(domain, ignoreCase = true)
                    }
                    if (isTracker) {
                        currentViewModel.incrementTrackerCount()
                        val emptyStream = ByteArrayInputStream("".toByteArray())
                        return WebResourceResponse("text/javascript", "UTF-8", emptyStream)
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                if (uri.scheme == "http" || uri.scheme == "https") {
                    return false
                }
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

    // Automatically clean up memory cache when tabs are closed to prevent leaks
    val allTabsState by viewModel.tabs.collectAsStateWithLifecycle()
    LaunchedEffect(allTabsState) {
        val activeIds = allTabsState.map { it.id }.toSet()
        val closedIds = webViewCache.keys.filter { it !in activeIds }
        closedIds.forEach { id ->
            webViewCache[id]?.let { wv ->
                wv.stopLoading()
                wv.clearHistory()
                wv.removeAllViews()
                wv.destroy()
            }
            webViewCache.remove(id)
        }
    }

    // Toggle Dark Reader styling dynamically and instantly in real-time without reloading
    LaunchedEffect(isDarkReaderActive) {
        val darkCssInject = if (isDarkReaderActive) {
            """
                (function() {
                    var style = document.getElementById('zen-dark-reader-styles');
                    if (!style) {
                        style = document.createElement('style');
                        style.id = 'zen-dark-reader-styles';
                        style.innerHTML = "html, body, iframe, div, section, p, li, blockquote { background-color: #050609 !important; color: #f1f5f9 !important; border-color: #1e2235 !important; } a { color: #60a5fa !important; } h1, h2, h3, h4, h5, h6, b, strong, val { color: #ffffff !important; } img, video, canvas { filter: brightness(0.85) contrast(1.05) !important; }";
                        document.head.appendChild(style);
                    }
                })();
            """.trimIndent()
        } else {
            """
                (function() {
                    var style = document.getElementById('zen-dark-reader-styles');
                    if (style) {
                        style.remove();
                    }
                })();
            """.trimIndent()
        }
        webView.evaluateJavascript(darkCssInject, null)
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

    // Sync back navigation local states
    LaunchedEffect(webView.url) {
        canGoBack = webView.canGoBack()
    }

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
        val tabUrl = tab.url
        val normTab = tabUrl.lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").trim().removeSuffix("/")
        val normLast = lastCommandedUrl.lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").trim().removeSuffix("/")
        val normCurrent = (webView.url ?: "").lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").trim().removeSuffix("/")

        if (tabUrl.isNotEmpty() && normTab != normLast && normTab != normCurrent) {
            lastCommandedUrl = tabUrl
            webView.loadUrl(tabUrl)
        }
    }

    // Native layout drawing
    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}
