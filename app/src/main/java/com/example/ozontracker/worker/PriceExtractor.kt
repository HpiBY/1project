package com.example.ozontracker.worker

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object PriceExtractor {

    private const val LOAD_TIMEOUT_MS = 35_000L
    private const val SETTLE_DELAY_MS = 2500L
    private const val WARMUP_URL = "https://ozon.by/"

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

    suspend fun fetchPrice(context: Context, url: String): Result {
        return try {
            withTimeoutOrNull(LOAD_TIMEOUT_MS + 10_000) {
                withContext(Dispatchers.Main) {
                    loadAndExtract(context, url)
                }
            } ?: Result.Error("Таймаут загрузки страницы (возможно, капча)")
        } catch (e: Throwable) {
            Result.Error("Сбой WebView: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun loadAndExtract(context: Context, targetUrl: String): Result =
        suspendCancellableCoroutine { cont ->
            try {
                val webView = WebView(context)
                var resumed = false
                var warmedUp = false

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

                fun finish(result: Result) {
                    if (resumed) return
                    resumed = true
                    try {
                        webView.stopLoading()
                        webView.destroy()
                    } catch (_: Throwable) {
                    }
                    if (cont.isActive) cont.resume(result)
                }

                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.settings.userAgentString = USER_AGENT
                webView.settings.loadWithOverviewMode = true
                webView.settings.useWideViewPort = true

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        if (!warmedUp) {
                            warmedUp = true
                            webView.postDelayed({
                                if (!resumed) webView.loadUrl(targetUrl)
                            }, SETTLE_DELAY_MS)
                            return
                        }

                        webView.postDelayed({
                            try {
                                webView.evaluateJavascript(EXTRACTION_SCRIPT) { rawResult ->
                                    finish(parseJsResult(rawResult))
                                }
                            } catch (e: Throwable) {
                                finish(Result.Error("Ошибка JS: ${e.message}"))
                            }
                        }, SETTLE_DELAY_MS)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        if (!warmedUp) {
                            warmedUp = true
                            webView.postDelayed({
                                if (!resumed) webView.loadUrl(targetUrl)
                            }, SETTLE_DELAY_MS)
                        } else {
                            finish(Result.Error("Ошибка загрузки: $description"))
                        }
                    }
                }

                cont.invokeOnCancellation {
                    try {
                        webView.stopLoading()
                        webView.destroy()
