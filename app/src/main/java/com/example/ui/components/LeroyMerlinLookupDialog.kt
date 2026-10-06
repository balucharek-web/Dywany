package com.example.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.LeroyMerlinProduct
import org.json.JSONObject

class LeroyWebBridge(
    private val onProductExtracted: (LeroyMerlinProduct) -> Unit
) {
    @JavascriptInterface
    fun onProductFound(
        title: String,
        priceStr: String,
        size: String,
        collection: String,
        composition: String
    ) {
        Handler(Looper.getMainLooper()).post {
            val price = priceStr.toDoubleOrNull()
            onProductExtracted(
                LeroyMerlinProduct(
                    title = title.trim(),
                    pricePln = price,
                    size = size.trim(),
                    collection = collection.trim(),
                    composition = composition.trim()
                )
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeroyMerlinLookupDialog(
    ean: String,
    onApplyProduct: (LeroyMerlinProduct) -> Unit,
    onDismiss: () -> Unit
) {
    val cleanEan = remember(ean) { ean.trim() }
    val searchUrl = remember(cleanEan) {
        "https://www.leroymerlin.pl/szukaj?q=${Uri.encode(cleanEan)}"
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var extractedProduct by remember { mutableStateOf<LeroyMerlinProduct?>(null) }
    var showWebViewPreview by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ładowanie strony Leroy Merlin dla kodu $cleanEan...") }

    val extractionJs = """
        (function() {
            function tryExtract() {
                var title = '';
                var price = '';
                var size = '';
                var collection = '';
                var composition = '';

                // Sprawdzenie strony produktu
                var h1 = document.querySelector('h1');
                var dataQaTitle = document.querySelector('[data-qa="product-title"]');
                var metaTitle = document.querySelector('meta[property="og:title"]');

                if (h1 && h1.innerText && h1.innerText.trim().length > 3) {
                    title = h1.innerText.trim();
                } else if (dataQaTitle && dataQaTitle.innerText) {
                    title = dataQaTitle.innerText.trim();
                } else if (metaTitle && metaTitle.getAttribute('content')) {
                    title = metaTitle.getAttribute('content').trim();
                }

                // Sprawdzenie listy wyników wyszukiwania (pierwsza karta produktu)
                if (!title) {
                    var firstCardLink = document.querySelector('a[data-qa="product-title"]') ||
                                        document.querySelector('article h3 a') ||
                                        document.querySelector('.product-card h3 a') ||
                                        document.querySelector('h3 a');
                    if (firstCardLink && firstCardLink.innerText) {
                        title = firstCardLink.innerText.trim();
                    }
                }

                // Oczyszczenie nazwy
                if (title) {
                    title = title.replace(/[\r\n]+/g, ' ')
                                 .replace(/\s*[-–|]\s*Leroy Merlin.*$/i, '')
                                 .replace(/\s*[-–|]\s*Sklep.*$/i, '')
                                 .trim();
                }

                // Cena
                var priceElem = document.querySelector('[data-qa="main-price"]') ||
                                document.querySelector('[data-qa="product-price"]') ||
                                document.querySelector('.price') ||
                                document.querySelector('meta[property="product:price:amount"]');
                if (priceElem) {
                    var rawPrice = priceElem.innerText || priceElem.getAttribute('content') || '';
                    rawPrice = rawPrice.replace(/[^\d.,]/g, '').replace(',', '.').trim();
                    price = rawPrice;
                }

                // Wymiary z tytułu
                if (title) {
                    var sizeMatch = title.match(/\b\d{2,3}\s*[xX*×]\s*\d{2,3}(?:\s*cm)?\b/);
                    if (sizeMatch) {
                        size = sizeMatch[0].replace(/\s+/g, ' ');
                        if (!size.toLowerCase().includes('cm')) size += ' cm';
                    }
                }

                // Marka / Kolekcja (Inspire, Artens, itp.)
                var knownBrands = ['Inspire', 'Artens', 'Agnella', 'Ragolle', 'Balta', 'Lano', 'Osta'];
                for (var i = 0; i < knownBrands.length; i++) {
                    if (title.toLowerCase().includes(knownBrands[i].toLowerCase())) {
                        collection = knownBrands[i];
                        break;
                    }
                }

                // Skład surowcowy
                var bodyText = (document.body ? document.body.innerText : '').toLowerCase();
                if (bodyText.includes('100% wełna') || title.toLowerCase().includes('wełna')) {
                    composition = '100% Wełna';
                } else if (bodyText.includes('polipropylen') || bodyText.includes('heat-set') || bodyText.includes('bcf')) {
                    composition = '100% Polipropylen';
                } else if (bodyText.includes('poliester')) {
                    composition = '100% Poliester';
                } else if (bodyText.includes('bawełna')) {
                    composition = '100% Bawełna';
                } else if (bodyText.includes('juta')) {
                    composition = '100% Juta';
                }

                if (title && title.length > 3 && !title.toLowerCase().includes('nie znaleziono') && !title.toLowerCase().includes('brak wyników')) {
                    if (window.AndroidLeroyBridge && window.AndroidLeroyBridge.onProductFound) {
                        window.AndroidLeroyBridge.onProductFound(title, price, size, collection, composition);
                        return true;
                    }
                }
                return false;
            }

            tryExtract();
            var attempts = 0;
            var timer = setInterval(function() {
                attempts++;
                if (tryExtract() || attempts > 12) {
                    clearInterval(timer);
                }
            }, 1200);
        })();
    """.trimIndent()

    fun forceExtractFromDom() {
        webViewInstance?.evaluateJavascript(
            """
            (function() {
                var t = document.querySelector('h1')?.innerText || 
                        document.querySelector('[data-qa="product-title"]')?.innerText || 
                        document.querySelector('a[data-qa="product-title"]')?.innerText ||
                        document.title || '';
                t = t.replace(/[\r\n]+/g, ' ').replace(/\s*[-–|]\s*Leroy Merlin.*$/i, '').trim();

                var p = (document.querySelector('[data-qa="main-price"]')?.innerText || 
                         document.querySelector('[data-qa="product-price"]')?.innerText || 
                         document.querySelector('.price')?.innerText || '').replace(/[^\d.,]/g, '').replace(',', '.').trim();

                var sMatch = t.match(/\b\d{2,3}\s*[xX*×]\s*\d{2,3}(?:\s*cm)?\b/);
                var s = sMatch ? sMatch[0] : '';
                if (s && !s.toLowerCase().includes('cm')) s += ' cm';

                return JSON.stringify({ title: t, price: p, size: s });
            })();
            """.trimIndent()
        ) { rawJson ->
            try {
                if (!rawJson.isNullOrBlank() && rawJson != "null") {
                    val cleanJson = if (rawJson.startsWith("\"") && rawJson.endsWith("\"")) {
                        org.json.JSONTokener(rawJson).nextValue().toString()
                    } else rawJson
                    val obj = JSONObject(cleanJson)
                    val t = obj.optString("title").trim()
                    val p = obj.optString("price").toDoubleOrNull()
                    val s = obj.optString("size").trim()

                    if (t.isNotBlank() && !t.contains("szukaj", ignoreCase = true) && !t.contains("brak wyników", ignoreCase = true)) {
                        extractedProduct = LeroyMerlinProduct(
                            title = t,
                            pricePln = p,
                            size = s,
                            barcode = cleanEan,
                            productUrl = searchUrl
                        )
                        statusMessage = "Znaleziono dane w Leroy Merlin!"
                    }
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.stopLoading()
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Wyszukiwanie w Leroy Merlin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Zamknij")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Szukany kod EAN:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = cleanEan,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Karta wyniku, jeśli pobrano produkt
                if (extractedProduct != null) {
                    val p = extractedProduct!!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("leroy_found_product_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Znaleziono w Leroy Merlin!",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20),
                                    fontSize = 13.sp
                                )
                            }

                            Text(
                                text = p.title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color(0xFF1B5E20)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (p.size.isNotBlank()) {
                                    Text(
                                        text = "Wymiary: ${p.size}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                if (p.pricePln != null && p.pricePln > 0.0) {
                                    Text(
                                        text = "Cena: ${p.pricePln.toInt()} zł",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }

                            if (p.collection.isNotBlank() || p.composition.isNotBlank()) {
                                Text(
                                    text = listOf(p.collection, p.composition).filter { it.isNotBlank() }.joinToString(" • "),
                                    fontSize = 11.sp,
                                    color = Color(0xFF558B2F)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    onApplyProduct(p.copy(barcode = cleanEan))
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_leroy_product_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Wstaw te dane do dywanu", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Pasek stanu ładowania
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = statusMessage,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Opcje podglądu WebView i ręcznego odczytu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showWebViewPreview = !showWebViewPreview },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (showWebViewPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showWebViewPreview) "Ukryj podgląd strony" else "Pokaż stronę sklepu",
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { forceExtractFromDom() },
                        modifier = Modifier.height(32.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Odczytaj ze strony", fontSize = 11.sp)
                    }
                }

                // Wbudowany WebView (widoczny lub ukryty o zerowej wysokości gdy zwinięty)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (showWebViewPreview) 260.dp else 1.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (showWebViewPreview) 1.dp else 0.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.userAgentString =
                                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                addJavascriptInterface(
                                    LeroyWebBridge { product ->
                                        extractedProduct = product
                                        isLoading = false
                                        statusMessage = "Pomyślnie odczytano nazwę z Leroy Merlin!"
                                    },
                                    "AndroidLeroyBridge"
                                )

                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        statusMessage = "Strona załadowana. Sprawdzam dane produktu..."
                                        view?.evaluateJavascript(extractionJs, null)
                                    }
                                }

                                webViewInstance = this
                                loadUrl(searchUrl)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (showWebViewPreview) {
                    Text(
                        text = "Wskazówka: Jeśli Leroy Merlin wyświetli weryfikację anty-botową, zaznacz ją na powyższym podglądzie, a aplikacja natychmiast odczyta dane.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (extractedProduct != null) {
                Button(
                    onClick = {
                        extractedProduct?.let { onApplyProduct(it.copy(barcode = cleanEan)) }
                        onDismiss()
                    }
                ) {
                    Text("Zastosuj")
                }
            } else {
                TextButton(onClick = { forceExtractFromDom() }) {
                    Text("Pobierz dane")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
