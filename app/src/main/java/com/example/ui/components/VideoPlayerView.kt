package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerView(
    url: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val sanitizedUrl = remember(url) {
        if (url.contains("0.0.1.44") || url.contains("localhost") || url.contains("127.0.0.1") || url.contains("10.0.2.2") || url.contains("0.0.0.0")) {
            val tmdbIdPattern = Regex("(?:movie|tv|tmdb)[/=](\\d+)")
            val match = tmdbIdPattern.find(url)
            val tmdbId = match?.groupValues?.get(1)?.toIntOrNull() ?: 818511
            
            val isTv = url.contains("tv") || url.contains("season") || url.contains("episode")
            if (isTv) {
                val seasonMatch = Regex("season[=/](\\d+)").find(url)
                val episodeMatch = Regex("episode[=/](\\d+)").find(url)
                val season = seasonMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val episode = episodeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                "https://vidsrc.to/embed/tv/$tmdbId/$season/$episode"
            } else {
                "https://vidsrc.to/embed/movie/$tmdbId"
            }
        } else {
            url
        }
    }
    
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    // For full-screen video support in HTML5 WebView
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Lock screen to landscape while playing to emulate a TV/theater experience
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        
        onDispose {
            activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Capture physical back button to close player or exit full screen
    BackHandler {
        if (customView != null) {
            // Exit HTML5 full screen
            customViewCallback?.onCustomViewHidden()
        } else {
            onClose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (customView != null) {
            // Render the HTML5 full screen container
            AndroidView(
                factory = {
                    FrameLayout(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        background = android.graphics.Color.BLACK.let { android.graphics.drawable.ColorDrawable(it) }
                        addView(customView)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Standard player layout
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        keepScreenOn = true // Prevent phone from sleeping while watching
                        
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Highly secure, sandboxed settings to prevent ads from opening new tabs/windows
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            
                            // Ad-blocking and popup configurations
                            setSupportMultipleWindows(false) // Block ads trying to open target="_blank"
                            javaScriptCanOpenWindowsAutomatically = false // Block window.open() popup spam
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                
                                // Inject custom CSS/JS to hide typical overlay elements, ad banners, and popups on scraper websites
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        var css = 'div[class*="ad"], div[id*="ad"], .banner, .popunder, #popunder, iframe[src*="ad"] { display: none !important; pointer-events: none !important; width: 0px !important; height: 0px !important; }';
                                        var head = document.head || document.getElementsByTagName('head')[0];
                                        var style = document.createElement('style');
                                        style.type = 'text/css';
                                        style.appendChild(document.createTextNode(css));
                                        head.appendChild(style);
                                        
                                        // Overwrite window.open to do nothing
                                        window.open = function() { return null; };
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                            }

                            // Block redirects to non-streaming domains (extremely effective ad block!)
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val destinationUrl = request?.url?.toString() ?: ""
                                
                                // Only allow loading the video embed/player domains and their stream providers
                                val allowedDomains = listOf(
                                    "vidsrc.to", "vidsrc.me", "embed.su", "autoembed.co", "autoembed.to", "multiembed.to", 
                                    "twitch.tv", "youtube.com", "vidplay.online", "filemoon.to", "rabbitstream.net", 
                                    "megacloud.tv", "vidsrc.stream", "2embed.cc", "2embed.to", "superembed.cc", 
                                    "kinobox.to", "vidsrc.xyz", "moviesapi.club", "player.vimeo.com"
                                )
                                val isAllowed = allowedDomains.any { destinationUrl.contains(it) } || 
                                               destinationUrl.contains("about:blank") || 
                                               destinationUrl.startsWith("data:")
                                
                                android.util.Log.d("VideoPlayerView", "Navigating to: $destinationUrl (Allowed: $isAllowed)")
                                return if (isAllowed) {
                                    false // Let WebView load it
                                } else {
                                    // Block redirect popups
                                    android.util.Log.d("VideoPlayerView", "Blocked popup redirect to: $destinationUrl")
                                    true 
                                }
                            }

                            // Intercept and drop requests to common advertising networks, analytics, and malware scrapers
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val requestUrl = request?.url?.toString() ?: ""
                                val adBlockKeywords = listOf(
                                    "google-analytics", "doubleclick", "adnxs", "popads", "popcash", "onclick",
                                    "exoclick", "yandex", "histats", "propellerads", "adsterra", "coin-hive"
                                )
                                
                                val isAdRequest = adBlockKeywords.any { requestUrl.contains(it) }
                                if (isAdRequest) {
                                    // Return empty resource response to block the advertisement loading
                                    return WebResourceResponse(
                                        "text/javascript",
                                        "UTF-8",
                                        ByteArrayInputStream("".toByteArray())
                                    )
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            // Support full screen playback of HTML5 videos
                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                customView = view
                                customViewCallback = callback
                            }

                            override fun onHideCustomView() {
                                customView = null
                                customViewCallback = null
                            }
                        }

                        loadUrl(sanitizedUrl)
                    }
                },
                update = { webView ->
                    // Re-load only if URL differs to prevent flicker
                    if (webView.url != sanitizedUrl) {
                        webView.loadUrl(sanitizedUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top control overlay bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Player",
                    tint = Color.White
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Open in External Browser Button (Critical fallback for Emulators/CORS)
                IconButton(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(sanitizedUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("VideoPlayerView", "Failed to open external browser: ${e.message}")
                        }
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open in Browser",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { webViewRef?.reload() },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Player",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
