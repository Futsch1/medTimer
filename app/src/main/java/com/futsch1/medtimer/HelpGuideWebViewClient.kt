package com.futsch1.medtimer

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

// The Kotlin override is present in bytecode, but WebKit 1.17.1's lint detector does not recognize it.
@SuppressLint("MissingOnRenderProcessGone")
internal class HelpGuideWebViewClient(
    private val assetLoader: WebViewAssetLoader,
    private val onRendererGone: (WebView) -> Unit,
    private val openExternalLink: (Uri) -> Unit
) : WebViewClient() {
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onRendererGone(view)
        return true
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        assetLoader.shouldInterceptRequest(request.url)

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        if (uri.scheme == "https" && uri.host == "appassets.androidplatform.net" && uri.path?.startsWith("/assets/") == true) {
            return false
        }
        if (uri.scheme == "https" || uri.scheme == "http") openExternalLink(uri)
        return true
    }
}
