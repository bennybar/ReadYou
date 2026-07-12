package me.ash.reader.ui.component.webview

import android.content.Context
import android.net.http.SslError
import android.util.Log
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import coil.imageLoader
import me.ash.reader.ui.ext.isUrl
import okio.buffer
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.net.HttpURLConnection
import java.net.URI

const val INJECTION_TOKEN = "/android_asset_font/"

class WebViewClient(
    private val context: Context,
    private val refererDomain: String?,
    private val onOpenLink: (url: String) -> Unit,
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        val url = request?.url?.toString()
        if (url != null && url.contains(INJECTION_TOKEN)) {
            try {
                val assetPath = url.substring(
                    url.indexOf(INJECTION_TOKEN) + INJECTION_TOKEN.length,
                    url.length
                )
                return WebResourceResponse(
                    "text/HTML",
                    "UTF-8",
                    context.assets.open(assetPath)
                )
            } catch (e: Exception) {
                Log.e("RLog", "WebView shouldInterceptRequest: $e")
            }
        } else if (url != null && url.isUrl()) {
            // Images prefetched by ReaderWorker live in Coil's disk cache. The WebView renderer has
            // its own network stack and would otherwise re-download them, so serve them from there:
            // that is what makes a prefetched article readable offline.
            cachedImageResponse(url)?.let { return it }
            try {
                var connection = URI.create(url).toURL().openConnection() as HttpURLConnection
                if (connection.responseCode == 403) {
                    connection.disconnect()
                    connection = URI.create(url).toURL().openConnection() as HttpURLConnection
                    connection.setRequestProperty("Referer", refererDomain)
                    val inputStream = DataInputStream(connection.inputStream)
                    return WebResourceResponse(connection.contentType, "UTF-8", inputStream)
                }
            } catch (e: Exception) {
                Log.e("RLog", "shouldInterceptRequest url: $e")
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    private fun cachedImageResponse(url: String): WebResourceResponse? {
        return try {
            val diskCache = context.imageLoader.diskCache ?: return null
            diskCache.openSnapshot(url)?.use { snapshot ->
                val bytes = diskCache.fileSystem.source(snapshot.data).buffer().readByteArray()
                val mimeType = sniffImageMimeType(bytes) ?: return null
                WebResourceResponse(mimeType, null, ByteArrayInputStream(bytes))
            }
        } catch (e: Exception) {
            Log.e("RLog", "WebView cachedImageResponse: $e")
            null
        }
    }

    private fun sniffImageMimeType(bytes: ByteArray): String? {
        fun byteAt(index: Int): Int = if (index < bytes.size) bytes[index].toInt() and 0xFF else -1
        return when {
            byteAt(0) == 0xFF && byteAt(1) == 0xD8 -> "image/jpeg"
            byteAt(0) == 0x89 && byteAt(1) == 0x50 -> "image/png"
            byteAt(0) == 0x47 && byteAt(1) == 0x49 && byteAt(2) == 0x46 -> "image/gif"
            byteAt(0) == 0x52 && byteAt(8) == 0x57 && byteAt(9) == 0x45 -> "image/webp"
            else -> null
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view!!.evaluateJavascript(OnImgClickScript, null)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (null == request?.url) return false
        val url = request.url.toString()
        if (url.isNotEmpty()) onOpenLink(url)
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        super.onReceivedError(view, request, error)
        Log.e("RLog", "RYWebView onReceivedError: $error")
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handler?.cancel()
    }

    companion object {
        private const val OnImgClickScript = """
            javascript:(function() {
                var imgs = document.getElementsByTagName("img");
                for(var i = 0; i < imgs.length; i++){
                    imgs[i].pos = i;
                    imgs[i].onclick = function(event) {
                        event.preventDefault();
                        window.${JavaScriptInterface.NAME}.onImgTagClick(this.src, this.alt);
                    }
                }
            })()
            """
    }
}
