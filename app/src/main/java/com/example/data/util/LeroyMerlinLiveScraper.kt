package com.example.data.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.coroutines.resume

/**
 * Automatyczny scraper działający w tle na urządzeniu mobilnym (Headless WebView).
 * Pobiera rzeczywiste dane produktu z oficjalnej strony leroymerlin.pl
 * (nazwę, aktualną cenę, numer referencyjny LM, kod EAN, wymiary, skład oraz zdjęcie)
 * i wkleja je bezpośrednio do pól formularza aplikacji.
 */
object LeroyMerlinLiveScraper {
    private const val TAG = "LeroyMerlinLiveScraper"

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun searchAndExtract(
        context: Context,
        query: String
    ): ExtractedProduct? = suspendCancellableCoroutine { continuation ->
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post {
            try {
                val clean = query.trim()
                if (clean.isBlank()) {
                    if (continuation.isActive) continuation.resume(null)
                    return@post
                }

                val encoded = URLEncoder.encode(clean, "UTF-8")
                val targetUrl = "https://www.leroymerlin.pl/szukaj?q=$encoded"
                Log.d(TAG, "Rozpoczynanie pobierania danych z leroymerlin.pl dla: '$clean' -> $targetUrl")

                var hasResumed = false
                val webView = WebView(context)

                fun finishWith(result: ExtractedProduct?) {
                    if (!hasResumed && continuation.isActive) {
                        hasResumed = true
                        continuation.resume(result)
                        try {
                            webView.stopLoading()
                            webView.destroy()
                        } catch (e: Exception) {
                            Log.e(TAG, "Błąd zamykania WebView: ${e.message}")
                        }
                    }
                }

                // Bezpieczny timeout po 12 sekundach
                mainHandler.postDelayed({
                    if (!hasResumed) {
                        Log.w(TAG, "Timeout pobierania z leroymerlin.pl dla: $clean")
                        finishWith(null)
                    }
                }, 12000)

                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    userAgentString =
                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(webView, true)

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "Strona leroymerlin.pl załadowana: $url. Uruchamianie analizy danych...")

                        evaluateExtraction(view ?: webView) { extracted ->
                            if (extracted != null && isValidProduct(extracted)) {
                                Log.i(TAG, "Znaleziono produkt na leroymerlin.pl: ${extracted.name}, cena: ${extracted.price}")
                                finishWith(extracted)
                            } else {
                                // Oczekiwanie na dynamiczny rendering React/Next.js (1.5 sekundy)
                                mainHandler.postDelayed({
                                    evaluateExtraction(view ?: webView) { retry ->
                                        if (retry != null && isValidProduct(retry)) {
                                            Log.i(TAG, "Znaleziono produkt po renderingu: ${retry.name}")
                                            finishWith(retry)
                                        } else {
                                            finishWith(null)
                                        }
                                    }
                                }, 1500)
                            }
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        Log.w(TAG, "Błąd ładowania WebView: ${error?.description}")
                    }
                }

                webView.loadUrl(targetUrl)

                continuation.invokeOnCancellation {
                    mainHandler.post {
                        try {
                            webView.stopLoading()
                            webView.destroy()
                        } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd scrapera: ${e.message}", e)
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    private fun isValidProduct(p: ExtractedProduct): Boolean {
        val lower = p.name.lowercase().trim()
        if (lower.isBlank() ||
            lower.contains("404") ||
            lower.contains("ups, coś poszło nie tak") ||
            lower.contains("sklepy budowlano-dekoracyjne") ||
            lower.contains("nie znaleźliśmy produktów") ||
            lower.contains("błąd")
        ) {
            return false
        }
        return p.price > 0.0 || p.lmNumber.isNotBlank() || p.imageUrl.isNotBlank()
    }

    private fun evaluateExtraction(
        webView: WebView,
        onResult: (ExtractedProduct?) -> Unit
    ) {
        val jsCode = """
            (function() {
                var res = {
                    name: "",
                    price: 0,
                    imageUrl: "",
                    lmNumber: "",
                    ean: "",
                    dimensions: "",
                    composition: ""
                };

                // 1. Sprawdź znaczniki Schema.org JSON-LD (<script type="application/ld+json">)
                var scripts = document.querySelectorAll('script[type="application/ld+json"]');
                for (var i = 0; i < scripts.length; i++) {
                    try {
                        var text = scripts[i].innerText;
                        if (!text) continue;
                        var data = JSON.parse(text);
                        var items = Array.isArray(data) ? data : (data['@graph'] || [data]);
                        for (var j = 0; j < items.length; j++) {
                            var it = items[j];
                            if (it && (it['@type'] === 'Product' || it['@type'] === 'IndividualProduct')) {
                                if (it.name) res.name = it.name;
                                if (it.sku) res.lmNumber = String(it.sku);
                                if (it.productID && !res.lmNumber) res.lmNumber = String(it.productID);
                                if (it.gtin13 || it.gtin || it.gtin8) res.ean = String(it.gtin13 || it.gtin || it.gtin8);
                                if (it.image) {
                                    if (typeof it.image === 'string') res.imageUrl = it.image;
                                    else if (Array.isArray(it.image) && it.image[0]) res.imageUrl = it.image[0];
                                    else if (it.image.url) res.imageUrl = it.image.url;
                                }
                                if (it.offers) {
                                    var offers = Array.isArray(it.offers) ? it.offers : [it.offers];
                                    for (var k = 0; k < offers.length; k++) {
                                        var p = parseFloat(offers[k].price);
                                        if (!isNaN(p) && p > 0) {
                                            res.price = p;
                                            break;
                                        }
                                    }
                                }
                                if (it.description) {
                                    var dimMatch = it.description.match(/(\d{2,3}\s*[xX*×]\s*\d{2,3}\s*cm)/i);
                                    if (dimMatch) res.dimensions = dimMatch[1];
                                }
                            }
                        }
                    } catch(e) {}
                }

                // 2. Sprawdź stan Next.js hydration
                if (!res.name || res.price === 0) {
                    try {
                        if (window.__NEXT_DATA__ && window.__NEXT_DATA__.props) {
                            var pProps = window.__NEXT_DATA__.props.pageProps || {};
                            var prod = pProps.product || (pProps.initialState && pProps.initialState.product);
                            if (prod) {
                                if (!res.name && prod.name) res.name = prod.name;
                                if (!res.lmNumber && (prod.sku || prod.referenceNumber || prod.id)) {
                                    res.lmNumber = String(prod.sku || prod.referenceNumber || prod.id);
                                }
                                if (!res.ean && prod.ean) res.ean = String(prod.ean);
                                if (res.price === 0 && (prod.price || prod.currentPrice)) {
                                    res.price = parseFloat(prod.price || prod.currentPrice) || 0;
                                }
                                if (!res.imageUrl && (prod.mainImage || (prod.images && prod.images[0]))) {
                                    res.imageUrl = prod.mainImage || prod.images[0];
                                }
                            }
                        }
                    } catch(e) {}
                }

                // 3. Sprawdź tagi OpenGraph
                if (!res.name) {
                    var ogT = document.querySelector('meta[property="og:title"]');
                    if (ogT && ogT.content && !ogT.content.toLowerCase().includes('sklepy budowlano')) {
                        res.name = ogT.content.replace(/\s*[-|]\s*Leroy Merlin.*/i, '').trim();
                    }
                }
                if (!res.imageUrl) {
                    var ogI = document.querySelector('meta[property="og:image"]');
                    if (ogI && ogI.content) res.imageUrl = ogI.content;
                }
                if (res.price === 0) {
                    var ogP = document.querySelector('meta[property="product:price:amount"]');
                    if (ogP && ogP.content) res.price = parseFloat(ogP.content) || 0;
                }

                // 4. Elementy DOM karty produktu
                if (!res.name) {
                    var h1 = document.querySelector('h1[data-qa="product-title"], h1[data-testid="product-title"], h1');
                    if (h1 && !h1.innerText.toLowerCase().includes('404')) {
                        res.name = h1.innerText.replace(/\s*[-|]\s*Leroy Merlin.*/i, '').trim();
                    }
                }

                if (res.price === 0) {
                    var priceSel = [
                        '[data-qa="product-price"]',
                        '[data-testid="product-price"]',
                        '.product-price',
                        '.main-price',
                        '[data-qa="price"]',
                        'span[class*="price-value"]',
                        'span[class*="price_main"]'
                    ];
                    for (var s = 0; s < priceSel.length; s++) {
                        var pel = document.querySelector(priceSel[s]);
                        if (pel) {
                            var cleanP = pel.innerText.replace(/\s/g, '').replace(',', '.').replace(/[^\d.]/g, '');
                            var pVal = parseFloat(cleanP);
                            if (!isNaN(pVal) && pVal > 0) {
                                res.price = pVal;
                                break;
                            }
                        }
                    }
                }

                // 5. Pierwsza karta z wyników wyszukiwania jeśli lista produktów
                if (!res.name || res.price === 0) {
                    var card = document.querySelector('article[data-qa="product-card"], div[data-qa="product-card"], [data-testid="product-card"], div[class*="product-card"]');
                    if (card) {
                        var cTitle = card.querySelector('h3, h2, [data-qa="product-title"], a[class*="title"]');
                        var cPrice = card.querySelector('[data-qa="product-price"], [class*="price"]');
                        var cImg = card.querySelector('img');
                        var cLink = card.querySelector('a[href*="/produkt/"], a[href*="-p"]');

                        if (cTitle && cTitle.innerText.trim()) res.name = cTitle.innerText.trim();
                        if (cPrice) {
                            var cpClean = cPrice.innerText.replace(/\s/g, '').replace(',', '.').replace(/[^\d.]/g, '');
                            var cpVal = parseFloat(cpClean);
                            if (!isNaN(cpVal)) res.price = cpVal;
                        }
                        if (cImg && cImg.src) res.imageUrl = cImg.src;
                        if (cLink && cLink.href) {
                            var m = cLink.href.match(/p?(\d{7,9})(?:\.html|\?|${'$'}|#|\/)/);
                            if (m) res.lmNumber = m[1];
                        }
                    }
                }

                // 6. Parsowanie wymiarów z nazwy produktu (np. "Dywan 160x230 cm")
                if (!res.dimensions && res.name) {
                    var mDim = res.name.match(/(\d{2,3}\s*[xX*×]\s*\d{2,3}\s*cm)/i);
                    if (mDim) res.dimensions = mDim[1];
                }

                // 7. Numer referencyjny LM z URL jeśli na karcie produktu
                if (!res.lmNumber) {
                    var uMatch = window.location.href.match(/p?(\d{7,9})(?:\.html|\?|${'$'}|#|\/)/);
                    if (uMatch) res.lmNumber = uMatch[1];
                }

                return JSON.stringify(res);
            })();
        """.trimIndent()

        webView.evaluateJavascript(jsCode) { rawJson ->
            try {
                if (rawJson != null && rawJson != "null") {
                    val unquoted = if (rawJson.startsWith("\"") && rawJson.endsWith("\"")) {
                        rawJson.substring(1, rawJson.length - 1)
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                    } else {
                        rawJson
                    }

                    val obj = JSONObject(unquoted)
                    val name = obj.optString("name").trim()
                    val price = obj.optDouble("price", 0.0)
                    val img = obj.optString("imageUrl").trim()
                    val lm = obj.optString("lmNumber").trim()
                    val ean = obj.optString("ean").trim()
                    val dim = obj.optString("dimensions").trim()
                    val comp = obj.optString("composition").trim()

                    if (name.isNotBlank()) {
                        onResult(
                            ExtractedProduct(
                                name = name,
                                price = price,
                                imageUrl = img,
                                lmNumber = lm,
                                ean = ean,
                                dimensions = dim,
                                composition = comp
                            )
                        )
                        return@evaluateJavascript
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd parsowania JSON ze strony leroymerlin.pl: ${e.message}")
            }
            onResult(null)
        }
    }
}
