package com.example.ui.components

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LeroyMerlinProduct
import com.example.data.util.LeroyMerlinParser
import org.json.JSONObject

class LeroyWebBridge(
    private val onDetailedProductExtracted: (LeroyMerlinProduct) -> Unit,
    private val onPageStatusReported: (String) -> Unit
) {
    @JavascriptInterface
    fun onDetailedProductFound(jsonStr: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                val obj = JSONObject(jsonStr)
                val title = obj.optString("title", "").trim()
                if (!LeroyMerlinParser.isValidProductTitle(title)) return@post

                val cleanTitle = LeroyMerlinParser.cleanProductTitle(title)
                val price = obj.optString("price").replace(",", ".").toDoubleOrNull()
                val promoPrice = obj.optString("promoPrice").replace(",", ".").toDoubleOrNull()

                var size = obj.optString("size", "").trim()
                if (size.isBlank()) size = LeroyMerlinParser.extractSize(title)

                var collection = obj.optString("collection", "").trim()
                if (collection.isBlank()) collection = LeroyMerlinParser.extractCollection(title)

                var composition = obj.optString("composition", "").trim()
                if (composition.isBlank()) composition = LeroyMerlinParser.extractComposition(title)

                var color = obj.optString("color", "").trim()
                if (color.isBlank()) color = LeroyMerlinParser.extractColor(title)

                var ref = obj.optString("ref", "").trim()
                val url = obj.optString("url", "")
                if (ref.isBlank()) ref = LeroyMerlinParser.extractRefCode(url)

                val pile = obj.optString("pileHeight", "").trim()
                val weight = obj.optString("weightGsm", "").trim()
                val imageUrl = obj.optString("imageUrl", "").trim()
                val ean = obj.optString("ean", "").trim()
                val pattern = LeroyMerlinParser.extractPatternSuggestion(cleanTitle)

                onDetailedProductExtracted(
                    LeroyMerlinProduct(
                        title = cleanTitle,
                        pricePln = price,
                        promoPricePln = promoPrice,
                        size = size,
                        collection = collection,
                        composition = composition,
                        barcode = ean,
                        productUrl = url,
                        imageUrl = imageUrl,
                        refCode = ref,
                        color = color,
                        pileHeightMm = pile,
                        weightGsm = weight,
                        patternSuggestion = pattern
                    )
                )
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
    }

    @JavascriptInterface
    fun onPageStatus(status: String) {
        Handler(Looper.getMainLooper()).post {
            onPageStatusReported(status)
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
    val context = LocalContext.current
    var searchQuery by remember(ean) { mutableStateOf(ean.trim()) }
    var activeUrl by remember { mutableStateOf(LeroyMerlinParser.buildSearchUrl(ean.trim())) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var pageStatus by remember { mutableStateOf<String?>(null) } // "404", "HOMEPAGE", "CAPTCHA", "FOUND"
    var extractedProduct by remember { mutableStateOf<LeroyMerlinProduct?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Przeglądarka, 1: Aplikacja LM / Schowek, 2: Szablony

    // Tryb pełnoekranowy (Fullscreen) - ułatwia nawigację po stronie sklepu
    var isFullscreen by remember { mutableStateOf(false) }

    // Pola edycyjne dla znalezionego produktu
    var editTitle by remember { mutableStateOf("") }
    var editPrice by remember { mutableStateOf("") }
    var editPromoPrice by remember { mutableStateOf("") }
    var editSize by remember { mutableStateOf("") }
    var editCollection by remember { mutableStateOf("") }
    var editComposition by remember { mutableStateOf("") }
    var editColor by remember { mutableStateOf("") }
    var editRefCode by remember { mutableStateOf("") }
    var editPileHeight by remember { mutableStateOf("") }
    var editWeight by remember { mutableStateOf("") }
    var editPatternType by remember { mutableIntStateOf(0) }

    fun updateEditableProduct(product: LeroyMerlinProduct) {
        extractedProduct = product
        editTitle = product.title
        editPrice = product.pricePln?.toInt()?.toString() ?: ""
        editPromoPrice = product.promoPricePln?.toInt()?.toString() ?: ""
        editSize = product.size
        editCollection = product.collection
        editComposition = product.composition
        editColor = product.color
        editRefCode = product.refCode
        editPileHeight = product.pileHeightMm
        editWeight = product.weightGsm
        editPatternType = product.patternSuggestion
        pageStatus = "FOUND"
    }

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
        if (clipText.isNotBlank()) {
            val parsed = LeroyMerlinParser.parsePastedTextOrUrl(clipText, fallbackBarcode = searchQuery)
            if (parsed != null) {
                updateEditableProduct(parsed)
                Toast.makeText(context, "Odczytano dane produktu ze schowka!", Toast.LENGTH_SHORT).show()
            } else {
                searchQuery = clipText
                activeUrl = if (clipText.startsWith("http")) clipText else LeroyMerlinParser.buildSearchUrl(clipText)
                webViewInstance?.loadUrl(activeUrl)
                Toast.makeText(context, "Wklejono tekst do wyszukiwarki", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Schowek jest pusty", Toast.LENGTH_SHORT).show()
        }
    }

    fun openInExternalApp() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Nie można otworzyć zewnętrznej aplikacji", Toast.LENGTH_SHORT).show()
        }
    }

    // Zaawansowany skrypt usuwający banery/popupy i odczytujący 100% specyfikacji
    val extractionJs = """
        (function() {
            // 1. Wstrzyknięcie czyszczącego CSS
            try {
                var cleanerId = 'dywanexpo-cleaner-style';
                if (!document.getElementById(cleanerId)) {
                    var st = document.createElement('style');
                    st.id = cleanerId;
                    st.innerHTML = `
                        #onetrust-consent-sdk, #onetrust-banner-sdk, .ot-sdk-container,
                        [data-qa="cookie-banner"], .cookie-banner, .cookie-notice,
                        .commercial-banner, [class*="coupon"], [class*="voucher"],
                        [class*="banner-promo"], [class*="app-banner"], [class*="download-app"],
                        .sticky-banner, [class*="floating-banner"], #freshworks-container {
                            display: none !important;
                            visibility: hidden !important;
                            opacity: 0 !important;
                            pointer-events: none !important;
                            height: 0 !important;
                        }
                        body {
                            overflow: auto !important;
                            padding-top: 0 !important;
                        }
                    `;
                    document.head.appendChild(st);
                }
            } catch (e) {}

            function analyze() {
                var docTitle = (document.title || '').trim();
                var h1 = (document.querySelector('h1')?.innerText || '').trim();

                // Sprawdź 404
                if (h1.includes('404') || h1.toLowerCase().includes('nie znaleziono strony') || docTitle.includes('404')) {
                    if (window.AndroidLeroyBridge && window.AndroidLeroyBridge.onPageStatus) {
                        window.AndroidLeroyBridge.onPageStatus('404');
                    }
                    return;
                }

                // Sprawdź Captcha / DataDome
                var bodyText = (document.body ? document.body.innerText : '') || '';
                var lowerBody = bodyText.toLowerCase();
                if (lowerBody.includes('datadome') || lowerBody.includes('geo.captcha') || lowerBody.includes('please enable js')) {
                    if (window.AndroidLeroyBridge && window.AndroidLeroyBridge.onPageStatus) {
                        window.AndroidLeroyBridge.onPageStatus('CAPTCHA');
                    }
                    return;
                }

                // Sprawdź stronę główną
                if (docTitle.toLowerCase().includes('sklepy budowlano-dekoracyjne') || h1.toLowerCase().includes('sklepy budowlano-dekoracyjne')) {
                    if (window.AndroidLeroyBridge && window.AndroidLeroyBridge.onPageStatus) {
                        window.AndroidLeroyBridge.onPageStatus('HOMEPAGE');
                    }
                    return;
                }

                var title = '';
                var price = '';
                var promoPrice = '';
                var size = '';
                var collection = '';
                var composition = '';
                var ref = '';
                var ean = '';
                var color = '';
                var pileHeight = '';
                var weightGsm = '';
                var imageUrl = '';

                // A. Parsowanie danych strukturalnych JSON-LD
                try {
                    var ldScripts = document.querySelectorAll('script[type="application/ld+json"]');
                    for (var i = 0; i < ldScripts.length; i++) {
                        try {
                            var data = JSON.parse(ldScripts[i].innerText);
                            var pObj = null;
                            if (data['@type'] === 'Product') pObj = data;
                            else if (Array.isArray(data)) pObj = data.find(function(it) { return it['@type'] === 'Product'; });
                            else if (data['@graph'] && Array.isArray(data['@graph'])) pObj = data['@graph'].find(function(it) { return it['@type'] === 'Product'; });

                            if (pObj) {
                                if (pObj.name) title = pObj.name.trim();
                                if (pObj.sku) ref = pObj.sku.toString();
                                if (pObj.gtin13) ean = pObj.gtin13.toString();
                                if (pObj.image) {
                                    imageUrl = Array.isArray(pObj.image) ? pObj.image[0] : (pObj.image.url || pObj.image);
                                }
                                if (pObj.brand) {
                                    collection = typeof pObj.brand === 'string' ? pObj.brand : (pObj.brand.name || '');
                                }
                                if (pObj.offers) {
                                    var off = Array.isArray(pObj.offers) ? pObj.offers[0] : pObj.offers;
                                    if (off && off.price) price = off.price.toString();
                                }
                                break;
                            }
                        } catch (e) {}
                    }
                } catch (e) {}

                // B. Tytuł z DOM (jeśli brak w ld+json)
                if (!title) {
                    var titleElem = document.querySelector('[data-qa="product-title"]') ||
                                    document.querySelector('h1[data-qa]') ||
                                    document.querySelector('.product-detail__title') ||
                                    document.querySelector('.product-title') ||
                                    document.querySelector('h1');
                    if (titleElem && titleElem.innerText) title = titleElem.innerText.trim();
                }

                // Pierwsza karta na liście wyników
                if (!title) {
                    var firstCard = document.querySelector('article h3 a') ||
                                    document.querySelector('[data-qa="product-card"] h3 a') ||
                                    document.querySelector('.product-card h3 a') ||
                                    document.querySelector('[data-qa="product-card"] a[data-qa="product-title"]');
                    if (firstCard && firstCard.innerText) title = firstCard.innerText.trim();
                }

                // C. Tabela cech i specyfikacji technicznych
                try {
                    var specElements = document.querySelectorAll('tr, dt, [data-qa="spec-item"], .specification-row, .technical-table tr, li');
                    specElements.forEach(function(row) {
                        var rowText = (row.innerText || '').toLowerCase();
                        if ((rowText.includes('szerokość') || rowText.includes('długość')) && !size) {
                            var m = rowText.match(/\b\d{2,3}\s*(?:cm)?\b/g);
                            if (m && m.length >= 2) {
                                size = m[0].replace(/[^\d]/g, '') + 'x' + m[1].replace(/[^\d]/g, '') + ' cm';
                            }
                        }
                        if ((rowText.includes('skład') || rowText.includes('materiał')) && !composition) {
                            var val = row.querySelector('dd, td, [data-qa="spec-value"]')?.innerText || row.innerText;
                            composition = val.replace(/skład|materiał|podstawowy|:/gi, '').trim();
                        }
                        if ((rowText.includes('kolor') || rowText.includes('kolorystyka')) && !color) {
                            var valCol = row.querySelector('dd, td, [data-qa="spec-value"]')?.innerText || row.innerText;
                            color = valCol.replace(/kolor|kolorystyka|gama|:/gi, '').trim();
                        }
                        if ((rowText.includes('wysokość runa') || rowText.includes('grubość')) && !pileHeight) {
                            var valP = row.querySelector('dd, td, [data-qa="spec-value"]')?.innerText || row.innerText;
                            pileHeight = valP.replace(/wysokość runa|grubość|:/gi, '').trim();
                        }
                        if ((rowText.includes('gramatura') || rowText.includes('waga')) && !weightGsm) {
                            var valW = row.querySelector('dd, td, [data-qa="spec-value"]')?.innerText || row.innerText;
                            weightGsm = valW.replace(/gramatura|waga|:/gi, '').trim();
                        }
                        if ((rowText.includes('marka') || rowText.includes('kolekcja')) && !collection) {
                            var valB = row.querySelector('dd, td, [data-qa="spec-value"]')?.innerText || row.innerText;
                            collection = valB.replace(/marka|kolekcja|produktu|:/gi, '').trim();
                        }
                        if (rowText.includes('kod ean') && !ean) {
                            var valE = row.querySelector('dd, td, [data-qa="spec-value"]')?.innerText || row.innerText;
                            ean = valE.replace(/[^\d]/g, '').trim();
                        }
                        if ((rowText.includes('ref') || rowText.includes('indeks') || rowText.includes('numer artykułu')) && !ref) {
                            var valR = row.querySelector('dd, td, [data-qa="spec-value"]')?.innerText || row.innerText;
                            ref = valR.replace(/[^\d]/g, '').trim();
                        }
                    });
                } catch (e) {}

                // D. Ceny z DOM (promocyjna i regularna)
                var priceElem = document.querySelector('[data-qa="main-price"]') ||
                                document.querySelector('[data-qa="product-price"]') ||
                                document.querySelector('.price-value') ||
                                document.querySelector('.price');
                if (priceElem && !price) {
                    price = (priceElem.innerText || '').replace(/[^\d.,]/g, '').replace(',', '.').trim();
                }

                var strikedElem = document.querySelector('[data-qa="striked-price"]') ||
                                  document.querySelector('.old-price') ||
                                  document.querySelector('.crossed-price') ||
                                  document.querySelector('[data-qa="product-old-price"]');
                if (strikedElem) {
                    var oldP = strikedElem.innerText.replace(/[^\d.,]/g, '').replace(',', '.').trim();
                    if (oldP && price && parseFloat(oldP) > parseFloat(price)) {
                        promoPrice = price;
                        price = oldP;
                    }
                }

                // E. Zdjęcie z DOM
                if (!imageUrl) {
                    var metaImg = document.querySelector('meta[property="og:image"]') ||
                                  document.querySelector('[data-qa="main-image"] img') ||
                                  document.querySelector('.product-gallery img');
                    if (metaImg) imageUrl = metaImg.getAttribute('content') || metaImg.src || '';
                }

                // F. Ref z URL
                if (!ref) {
                    var mU = window.location.href.match(/-(\d{7,9})\.html/);
                    if (mU) ref = mU[1];
                }

                if (title && title.length > 3) {
                    var payload = JSON.stringify({
                        title: title,
                        price: price,
                        promoPrice: promoPrice,
                        size: size,
                        collection: collection,
                        composition: composition,
                        ref: ref,
                        ean: ean,
                        color: color,
                        pileHeight: pileHeight,
                        weightGsm: weightGsm,
                        imageUrl: imageUrl,
                        url: window.location.href
                    });
                    if (window.AndroidLeroyBridge && window.AndroidLeroyBridge.onDetailedProductFound) {
                        window.AndroidLeroyBridge.onDetailedProductFound(payload);
                    }
                }
            }

            analyze();
            var count = 0;
            var interval = setInterval(function() {
                count++;
                analyze();
                if (count > 8) clearInterval(interval);
            }, 1200);
        })();
    """.trimIndent()

    fun triggerExtraction() {
        webViewInstance?.evaluateJavascript(extractionJs, null)
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.stopLoading()
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = if (isFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.90f)
                    .clip(RoundedCornerShape(22.dp))
            },
            shape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Górny pasek tytułowy z przyciskami Pełny Ekran i Zamknij
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Wyszukiwarka Leroy Merlin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isFullscreen) "Tryb pełnoekranowy (łatwa nawigacja)" else "Pobieranie kompletnych parametrów dywanu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Przycisk Pełny Ekran
                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isFullscreen) "Wyjdź z pełnego ekranu" else "Powiększ na cały ekran",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Zamknij")
                        }
                    }
                }

                // Pasek wyszukiwania i akcji
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("leroy_search_input_field"),
                        placeholder = { Text("EAN, Ref LM lub nazwa dywanu...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Wyczyść", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (searchQuery.isNotBlank()) {
                                val url = if (searchQuery.startsWith("http")) searchQuery else LeroyMerlinParser.buildSearchUrl(searchQuery)
                                activeUrl = url
                                pageStatus = null
                                isLoading = true
                                webViewInstance?.loadUrl(url)
                            }
                        }),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                val url = if (searchQuery.startsWith("http")) searchQuery else LeroyMerlinParser.buildSearchUrl(searchQuery)
                                activeUrl = url
                                pageStatus = null
                                isLoading = true
                                webViewInstance?.loadUrl(url)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Szukaj", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { pasteFromClipboard() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Wklej ze schowka", modifier = Modifier.size(18.dp))
                    }
                }

                // Szybkie chipy (skróty do kategorii dywanów i wyszukiwań)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = activeUrl.contains("/dywany/"),
                        onClick = {
                            val u = "https://www.leroymerlin.pl/produkty/wystroj-wnetrz/dywany-i-chodniki/dywany/"
                            activeUrl = u
                            searchQuery = "Kategoria: Dywany"
                            webViewInstance?.loadUrl(u)
                        },
                        label = { Text("Kategoria Dywany LM", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )

                    if (ean.isNotBlank()) {
                        FilterChip(
                            selected = searchQuery == ean.trim(),
                            onClick = {
                                searchQuery = ean.trim()
                                val u = LeroyMerlinParser.buildSearchUrl(ean.trim())
                                activeUrl = u
                                webViewInstance?.loadUrl(u)
                            },
                            label = { Text("EAN: $ean", fontSize = 11.sp) }
                        )
                    }

                    FilterChip(
                        selected = searchQuery.contains("Dywan Inspire", ignoreCase = true),
                        onClick = {
                            searchQuery = "Dywan Inspire"
                            val u = LeroyMerlinParser.buildSearchUrl("Dywan Inspire")
                            activeUrl = u
                            webViewInstance?.loadUrl(u)
                        },
                        label = { Text("Dywany Inspire", fontSize = 11.sp) }
                    )

                    FilterChip(
                        selected = searchQuery.contains("Dywan Agnella", ignoreCase = true),
                        onClick = {
                            searchQuery = "Dywan Agnella"
                            val u = LeroyMerlinParser.buildSearchUrl("Dywan Agnella")
                            activeUrl = u
                            webViewInstance?.loadUrl(u)
                        },
                        label = { Text("Dywany Agnella", fontSize = 11.sp) }
                    )
                }

                // KARTA ZNALEZIONEGO PRODUKTU Z KOMPLETNYMI PARAMETRAMI
                if (extractedProduct != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .testTag("leroy_found_product_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Wykryto produkt w Leroy Merlin!",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20),
                                        fontSize = 12.sp
                                    )
                                }

                                IconButton(
                                    onClick = { extractedProduct = null },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Usuń podgląd", modifier = Modifier.size(14.dp))
                                }
                            }

                            // Tytuł
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text("Model / Tytuł", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Ceny (regularna i promocyjna) oraz rozmiar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = editPrice,
                                    onValueChange = { editPrice = it },
                                    label = { Text("Cena (zł)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editPromoPrice,
                                    onValueChange = { editPromoPrice = it },
                                    label = { Text("Promocja (zł)", fontSize = 10.sp) },
                                    placeholder = { Text("brak", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editSize,
                                    onValueChange = { editSize = it },
                                    label = { Text("Wymiary", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            // Szczegółowe parametry: Kolekcja, Skład, Kolor, Ref LM
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = editCollection,
                                    onValueChange = { editCollection = it },
                                    label = { Text("Kolekcja", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editComposition,
                                    onValueChange = { editComposition = it },
                                    label = { Text("Skład materiałowy", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1.3f),
                                    singleLine = true
                                )
                            }

                            // Dodatkowe cechy techniczne (Kolor, Ref LM, Runo/Gramatura)
                            val extraSpecs = listOfNotNull(
                                if (editColor.isNotBlank()) "Kolor: $editColor" else null,
                                if (editRefCode.isNotBlank()) "Ref LM: $editRefCode" else null,
                                if (editPileHeight.isNotBlank()) "Runa: $editPileHeight" else null,
                                if (editWeight.isNotBlank()) "Waga: $editWeight" else null
                            ).joinToString(" • ")

                            if (extraSpecs.isNotBlank()) {
                                Text(
                                    text = extraSpecs,
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    val finalProduct = extractedProduct!!.copy(
                                        title = editTitle.trim(),
                                        pricePln = editPrice.toDoubleOrNull(),
                                        promoPricePln = editPromoPrice.toDoubleOrNull(),
                                        size = editSize.trim(),
                                        collection = editCollection.trim(),
                                        composition = editComposition.trim(),
                                        color = editColor.trim(),
                                        refCode = editRefCode.trim(),
                                        pileHeightMm = editPileHeight.trim(),
                                        weightGsm = editWeight.trim(),
                                        patternSuggestion = editPatternType,
                                        barcode = ean.trim()
                                    )
                                    onApplyProduct(finalProduct)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_leroy_product_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Wstaw te dane do dywanu", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Komunikaty stanu (404, Homepage, Captcha)
                when (pageStatus) {
                    "404" -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Brak produktu dla '$searchQuery' w LM (Błąd 404). Wyszukaj dywan po nazwie lub użyj szablonu.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB71C1C)
                                )
                            }
                        }
                    }
                    "HOMEPAGE" -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE3F2FD)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Strona główna sklepu. Kliknij powyżej 'Kategoria Dywany LM' lub wpisz nazwę w polu wyszukiwania.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0D47A1)
                                )
                            }
                        }
                    }
                    "CAPTCHA" -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Weryfikacja anty-botowa. Rozwiąż ją na podglądzie poniżej, a dane natychmiast się odczytają.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }

                // Zakładki (ukrywane w trybie pełnoekranowym dla maksymalnej przestrzeni)
                if (!isFullscreen) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Podgląd sklepu", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Aplikacja LM / Schowek", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Szablony dywanów LM", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ZAWARTOŚĆ GŁÓWNA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = if (isFullscreen) 0.dp else 14.dp)
                ) {
                    if (isFullscreen || selectedTab == 0) {
                        // PRZEGLĄDARKA WEBVIEW Z PŁYWAJĄCYM PASKIEM NAWIGACJI
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Pasek nawigacji przeglądarki
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Dalej", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { webViewInstance?.reload() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Odśwież", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { webViewInstance?.zoomIn() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ZoomIn, contentDescription = "Powiększ", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { webViewInstance?.zoomOut() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ZoomOut, contentDescription = "Pomniejsz", modifier = Modifier.size(16.dp))
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Button(
                                        onClick = { triggerExtraction() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Odczytaj ze strony", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Kontener WebView
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isFullscreen) 0.dp else 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp)
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
                                            settings.loadWithOverviewMode = true
                                            settings.useWideViewPort = true
                                            settings.setSupportZoom(true)
                                            settings.builtInZoomControls = true
                                            settings.displayZoomControls = false
                                            settings.userAgentString =
                                                "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

                                            CookieManager.getInstance().setAcceptCookie(true)
                                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                            addJavascriptInterface(
                                                LeroyWebBridge(
                                                    onDetailedProductExtracted = { product ->
                                                        updateEditableProduct(product)
                                                        isLoading = false
                                                    },
                                                    onPageStatusReported = { status ->
                                                        pageStatus = status
                                                        isLoading = false
                                                    }
                                                ),
                                                "AndroidLeroyBridge"
                                            )

                                            webChromeClient = WebChromeClient()
                                            webViewClient = object : WebViewClient() {
                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    super.onPageFinished(view, url)
                                                    isLoading = false
                                                    view?.evaluateJavascript(extractionJs, null)
                                                }
                                            }

                                            webViewInstance = this
                                            loadUrl(activeUrl)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Pływający przycisk na dole ekranu w trybie pełnoekranowym
                                if (isFullscreen) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 16.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFF1B5E20).copy(alpha = 0.95f),
                                        shadowElevation = 8.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .clickable { triggerExtraction() }
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (extractedProduct != null) "✅ Zastosuj odczytany dywan: ${extractedProduct?.title?.take(22)}..." else "Odczytaj produkt z tej strony",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedTab == 1) {
                        // Zakładka 1: Otwórz w aplikacji LM / Wklej ze schowka
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Wygodne wyszukiwanie przez oficjalną aplikację LM:",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "1. Kliknij poniższy przycisk, aby otworzyć wyszukiwanie w aplikacji Leroy Merlin na telefonie.\n" +
                                               "2. W aplikacji LM znajdź dywan i kliknij Udostępnij / Kopiuj link (albo zaznacz i skopiuj tekst).\n" +
                                               "3. Wróć tutaj i kliknij 'Wklej ze schowka' — aplikacja natychmiast wyciągnie nazwę, wymiary, cenę, kolekcję i kod artykułu!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { openInExternalApp() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Otwórz w aplikacji Leroy Merlin / Chrome", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { pasteFromClipboard() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Wklej link lub tekst ze schowka", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (selectedTab == 2) {
                        // Zakładka 2: Szybkie szablony marek i rozmiarów LM
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Popularne kolekcje dywanów w Leroy Merlin:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Marka / Kolekcja
                            Text(text = "1. Marka / Kolekcja:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Inspire", "Artens", "Agnella", "Lano", "Ragolle", "Balta", "Osta").forEach { col ->
                                    FilterChip(
                                        selected = editCollection == col,
                                        onClick = {
                                            editCollection = col
                                            if (editTitle.isBlank()) editTitle = "Dywan $col"
                                            else if (!editTitle.contains(col)) editTitle += " $col"
                                        },
                                        label = { Text(col, fontSize = 11.sp) }
                                    )
                                }
                            }

                            // Wymiary
                            Text(text = "2. Wymiary dywanu:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("80x150 cm", "120x170 cm", "140x200 cm", "160x230 cm", "200x300 cm", "Chodnik 80x250 cm").forEach { sz ->
                                    FilterChip(
                                        selected = editSize == sz,
                                        onClick = {
                                            editSize = sz
                                            if (!editTitle.contains(sz)) {
                                                editTitle = editTitle.replace(Regex("\\b\\d{2,3}\\s*[xX]\\s*\\d{2,3}(?:\\s*cm)?\\b"), "").trim() + " $sz"
                                            }
                                        },
                                        label = { Text(sz, fontSize = 11.sp) }
                                    )
                                }
                            }

                            // Skład
                            Text(text = "3. Skład surowcowy:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("100% Wełna", "100% Polipropylen Heat-Set", "100% Poliester", "100% Juta").forEach { comp ->
                                    FilterChip(
                                        selected = editComposition == comp,
                                        onClick = { editComposition = comp },
                                        label = { Text(comp, fontSize = 11.sp) }
                                    )
                                }
                            }

                            // Kolor wiodący
                            Text(text = "4. Kolorystyka:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Beżowy", "Szary", "Kremowy", "Antracyt", "Zielony", "Niebieski", "Brązowy").forEach { clr ->
                                    FilterChip(
                                        selected = editColor == clr,
                                        onClick = { editColor = clr },
                                        label = { Text(clr, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val title = editTitle.ifBlank { "Dywan ${editCollection.ifBlank { "Ekspozycja" }} ${editSize.ifBlank { "160x230 cm" }}" }
                                    val templateProduct = LeroyMerlinProduct(
                                        title = title.trim(),
                                        pricePln = editPrice.toDoubleOrNull() ?: 299.0,
                                        promoPricePln = editPromoPrice.toDoubleOrNull(),
                                        size = editSize.ifBlank { "160x230 cm" },
                                        collection = editCollection.ifBlank { "Inspire" },
                                        composition = editComposition.ifBlank { "100% Polipropylen Heat-Set" },
                                        color = editColor.ifBlank { "Beżowy" },
                                        patternSuggestion = editPatternType,
                                        barcode = ean.trim()
                                    )
                                    updateEditableProduct(templateProduct)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Utwórz dywan z powyższego szablonu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Dolny pasek dialogu
                if (!isFullscreen) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Anuluj", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (extractedProduct != null) {
                            Button(
                                onClick = {
                                    val finalProduct = extractedProduct!!.copy(
                                        title = editTitle.trim(),
                                        pricePln = editPrice.toDoubleOrNull(),
                                        promoPricePln = editPromoPrice.toDoubleOrNull(),
                                        size = editSize.trim(),
                                        collection = editCollection.trim(),
                                        composition = editComposition.trim(),
                                        color = editColor.trim(),
                                        refCode = editRefCode.trim(),
                                        pileHeightMm = editPileHeight.trim(),
                                        weightGsm = editWeight.trim(),
                                        patternSuggestion = editPatternType,
                                        barcode = ean.trim()
                                    )
                                    onApplyProduct(finalProduct)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Zastosuj dane", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
