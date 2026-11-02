package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A highly-optimized Compose component to render Adsterra Banner ads safely inside a WebView.
 *
 * @param adKey The 32-character Adsterra placement key (e.g. "b3b2c1f0d3a5e6f7...")
 * @param width The banner width in pixels (typically 320 for mobile)
 * @param height The banner height in pixels (typically 50 for standard mobile banners)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdsterraAdBanner(
    adKey: String,
    adDomain: String = "www.highperformanceformat.com",
    width: Int = 320,
    height: Int = 50,
    modifier: Modifier = Modifier
) {
    if (adKey.isBlank() || adKey == "YOUR_ADSTERRA_KEY_HERE") {
        // Return an empty box if no key is configured yet
        return
    }

    val finalDomain = if (adDomain.isBlank()) "www.highperformanceformat.com" else adDomain

    // Prevent application crashes if user mistakenly configures extremely large values (such as placement IDs) in width or height
    val safeWidth = if (width in 50..1000) width else 300
    val safeHeight = if (height in 20..1000) height else 250

    // Standard Adsterra script payload wrapped in a responsive, centered HTML structure
    val htmlPayload = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                body, html {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    background-color: transparent;
                    overflow: hidden;
                }
            </style>
        </head>
        <body>
            <div id="ad-container">
                <script type="text/javascript">
                    atOptions = {
                        'key' : '$adKey',
                        'format' : 'iframe',
                        'height' : $safeHeight,
                        'width' : $safeWidth,
                        'params' : {}
                    };
                </script>
                <script type="text/javascript" src="https://$finalDomain/$adKey/invoke.js"></script>
            </div>
        </body>
        </html>
    """.trimIndent()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(safeHeight.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    // Transparent background so it blends perfectly with your app themes
                    setBackgroundColor(0)
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            url: String?
                        ): Boolean {
                            // When user clicks the ad, open it in the system browser so it does not replace your app view
                            url?.let {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(it))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            return true // block WebView navigation, open externally
                        }
                    }
                    
                    loadDataWithBaseURL("https://$finalDomain", htmlPayload, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                // WebView updates if recomposed with a new key
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(safeHeight.dp)
        )
    }
}
