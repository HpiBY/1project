package com.example.ozontracker.worker

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object PriceExtractor {

    private const val LOAD_TIMEOUT_MS = 20_000L
    private const val SETTLE_DELAY_MS = 1500L

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

    suspend fun fetchPrice(context: Context, url: String): Result {
        return withTimeoutOrNull(LOAD_TIMEOUT_MS + 5000) {
            withContext(Dispatchers.Main) {
                loadAndExtract(context, url)
            }
        } ?: Result.Error("Таймаут загрузки страницы")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun loadAndExtract(context: Context, url: String): Result =
        suspendCancellableCoroutine { cont ->
            val webView = WebView(context)
            var resumed = false

            fun finish(result: Result) {
                if (resumed) return
                resumed = true
                webView.stopLoading()
                webView.destroy()
                if (cont.isActive) cont.resume(result)
            }

            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.userAgentString = USER_AGENT
            webView.settings.loadWithOverviewMode = true
            webView.settings.useWideViewPort = true

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                    webView.postDelayed({
                        webView.evaluateJavascript(EXTRACTION_SCRIPT) { rawResult ->
                            finish(parseJsResult(rawResult))
                        }
                    }, SETTLE_DELAY_MS)
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    finish(Result.Error("Ошибка загрузки: $description"))
                }
            }

            cont.invokeOnCancellation {
                webView.stopLoading()
                webView.destroy()
            }

            webView.loadUrl(url)
        }

    private fun parseJsResult(raw: String?): Result {
        if (raw == null || raw == "null") return Result.Error("Цена не найдена на странице")
        val cleaned = raw.trim('"').replace("\\\"", "\"")
        if (cleaned.isBlank() || cleaned == "null") {
            return Result.Error("Цена не найдена на странице")
        }
        val digitsOnly = cleaned.filter { it.isDigit() }
        val price = digitsOnly.toLongOrNull()
        return if (price != null && price > 0) {
            Result.Success(price)
        } else {
            Result.Error("Не удалось распознать цену: '$cleaned'")
        }
    }

    private const val EXTRACTION_SCRIPT = """
        (function() {
            function textOf(el) { return el ? el.innerText || el.textContent || "" : ""; }

            var priceRegex = /[\d][\d\s]{1,}(?=[\s,.]{0,2}(?:\u20BD|Br|BYN|\u0440\.?))/i;
            function findAmount(text) {
                var m = text.match(priceRegex);
                return m ? m[0].replace(/\s/g, '') : null;
            }

            var widgetSelectors = [
                '[data-widget="webPrice"]',
                '[data-widget="webSale"]',
                '[data-widget="webOldPrice"]'
            ];
            for (var i = 0; i < widgetSelectors.length; i++) {
                var el = document.querySelector(widgetSelectors[i]);
                if (el) {
                    var found = findAmount(textOf(el));
                    if (found) return found;
                }
            }

            var candidates = document.querySelectorAll('[class*="price" i], [data-testid*="price" i]');
            for (var j = 0; j < candidates.length; j++) {
                var found2 = findAmount(textOf(candidates[j]));
                if (found2) return found2;
            }

            var bodyText = document.body ? document.body.innerText : "";
            var found3 = findAmount(bodyText);
            if (found3) return found3;

            return null;
        })();
    """

    sealed class Result {
        data class Success(val priceRub: Long) : Result()
        data class Error(val message: String) : Result()
    }
}
