package com.example.ui.dialogs

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import org.json.JSONObject

data class ExtractedProduct(
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val lmNumber: String = "",
    val ean: String = "",
    val dimensions: String = "",
    val composition: String = ""
)

private fun buildLeroyUrl(query: String): String {
    val parsed = com.example.data.util.ProductCodeParser.parse(query)
    val validCode = parsed.getLeroySearchQuery()
    return if (validCode != null) {
        "https://www.leroymerlin.pl/szukaj?q=${Uri.encode(validCode)}"
    } else {
        "https://www.leroymerlin.pl/dywany-i-wykladziny/dywany,a1226.html"
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeroyMerlinLookupDialog(
    initialQuery: String,
    onProductExtracted: (name: String, price: Double, imageUrl: String, lmNumber: String) -> Unit,
    onFullProductExtracted: ((ExtractedProduct) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf(initialQuery.trim()) }
    var currentUrl by remember { mutableStateOf(buildLeroyUrl(initialQuery)) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var detectedProduct by remember { mutableStateOf<ExtractedProduct?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun executeSearch(q: String) {
        val target = buildLeroyUrl(q)
        currentUrl = target
        detectedProduct = null
        statusMessage = null
        webViewInstance?.loadUrl(target)
    }

    fun inspectCurrentPage(wv: WebView, onSuccessResult: ((ExtractedProduct) -> Unit)? = null) {
        extractProductFromWebView(wv) { extracted ->
            if (extracted.name.isNotBlank() || extracted.price > 0.0 || extracted.lmNumber.isNotBlank()) {
                detectedProduct = extracted
                statusMessage = "Rozpoznano dane produktu!"
                onSuccessResult?.invoke(extracted)
            } else {
                statusMessage = "Nie wykryto danych produktu na tej podstronie. Otwórz kartę konkretnego dywanu."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Toolbar
                Surface(
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Zamknij",
                                        tint = Color.White
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Wyszukiwarka Leroy Merlin",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Oficjalny serwis leroymerlin.pl",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Open in external browser
                                IconButton(
                                    onClick = {
                                        try {
                                            val currentOrTarget = webViewInstance?.url ?: currentUrl
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentOrTarget))
                                            context.startActivity(browserIntent)
                                        } catch (e: Exception) {
                                            Log.e("LeroyMerlinLookup", "Cannot open external browser: ${e.message}")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Otwórz w przeglądarce",
                                        tint = Color.White
                                    )
                                }

                                // Quick Extract Button
                                Button(
                                    onClick = {
                                        webViewInstance?.let { wv ->
                                            inspectCurrentPage(wv) { product ->
                                                onFullProductExtracted?.invoke(product)
                                                onProductExtracted(
                                                    product.name,
                                                    product.price,
                                                    product.imageUrl,
                                                    product.lmNumber
                                                )
                                                onDismiss()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Pobierz",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Search Input Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "Wpisz kod LM, EAN lub nazwę dywanu...",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    executeSearch(searchQuery)
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { executeSearch(searchQuery) }) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Szukaj",
                                            tint = Color.White
                                        )
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = {
                                    if (webViewInstance?.canGoBack() == true) {
                                        webViewInstance?.goBack()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Wstecz",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (webViewInstance?.canGoForward() == true) {
                                        webViewInstance?.goForward()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Dalej",
                                    tint = Color.White
                                )
                            }

                            IconButton(onClick = { webViewInstance?.reload() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Odśwież",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // In-App WebView Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    allowContentAccess = true
                                    allowFileAccess = false
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    userAgentString =
                                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                }

                                // Enable cookies for smooth Leroy Merlin anti-bot and session
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        if (url.startsWith("http://") || url.startsWith("https://")) {
                                            view?.loadUrl(url)
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        currentUrl = url ?: currentUrl

                                        // Auto-inspect page in background to assist the user
                                        inspectCurrentPage(this@apply)
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        isLoading = false
                                    }
                                }

                                loadUrl(currentUrl)
                                webViewInstance = this
                            }
                        },
                        update = { view ->
                            webViewInstance = view
                        }
                    )

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF2E7D32))
                        }
                    }
                }

                // Bottom Product Detection & Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val prod = detectedProduct
                        if (prod != null && (prod.name.isNotBlank() || prod.price > 0.0 || prod.lmNumber.isNotBlank())) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (prod.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = prod.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFC8E6C9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingBag,
                                                contentDescription = null,
                                                tint = Color(0xFF2E7D32)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.name.ifBlank { "Wykryty dywan" },
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF1B5E20)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (prod.price > 0.0) {
                                                Text(
                                                    text = "${String.format("%.2f", prod.price)} zł",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            } else {
                                                Text(
                                                    text = "Cena do potwierdzenia",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            if (prod.lmNumber.isNotBlank()) {
                                                Text(
                                                    text = "LM: ${prod.lmNumber}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF388E3C)
                                                )
                                            }
                                        }

                                        if (prod.ean.isNotBlank() || prod.dimensions.isNotBlank()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (prod.ean.isNotBlank()) {
                                                    Text(
                                                        text = "EAN: ${prod.ean}",
                                                        fontSize = 10.sp,
                                                        color = Color.DarkGray
                                                    )
                                                }
                                                if (prod.dimensions.isNotBlank()) {
                                                    Text(
                                                        text = prod.dimensions,
                                                        fontSize = 10.sp,
                                                        color = Color.DarkGray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    onFullProductExtracted?.invoke(prod)
                                    onProductExtracted(
                                        prod.name,
                                        prod.price,
                                        prod.imageUrl,
                                        prod.lmNumber
                                    )
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Zatwierdź i pobierz dane do aplikacji",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            // Instruction & manual retry
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Wskazówka wyszukiwania:",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = statusMessage
                                            ?: "Wyszukaj dywan lub przejdź do karty produktu, a dane pobiorą się automatycznie.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        webViewInstance?.let { wv ->
                                            inspectCurrentPage(wv)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Sprawdź stronę",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extracts product details via JavaScript executing in the live WebView DOM.
 * Works seamlessly across modern single-page apps, JSON-LD Schema.org, Next.js hydration data,
 * OpenGraph metadata, and product card listings without triggering server-side bot mitigations.
 */
private fun extractProductFromWebView(
    webView: WebView,
    onResult: (ExtractedProduct) -> Unit
) {
    val jsCode = """
        (function() {
            var result = {
                name: "",
                price: 0,
                imageUrl: "",
                lmNumber: "",
                ean: "",
                dimensions: "",
                composition: ""
            };

            // 1. Check Schema.org JSON-LD scripts (<script type="application/ld+json">)
            var scripts = document.querySelectorAll('script[type="application/ld+json"]');
            for (var i = 0; i < scripts.length; i++) {
                try {
                    var raw = scripts[i].innerText;
                    if (!raw) continue;
                    var data = JSON.parse(raw);
                    var items = Array.isArray(data) ? data : (data['@graph'] || [data]);
                    for (var j = 0; j < items.length; j++) {
                        var it = items[j];
                        if (it && (it['@type'] === 'Product' || it['@type'] === 'IndividualProduct')) {
                            if (it.name) result.name = it.name;
                            if (it.sku) result.lmNumber = String(it.sku);
                            if (it.productID && !result.lmNumber) result.lmNumber = String(it.productID);
                            if (it.gtin13 || it.gtin || it.gtin8) {
                                result.ean = String(it.gtin13 || it.gtin || it.gtin8);
                            }
                            if (it.image) {
                                if (typeof it.image === 'string') result.imageUrl = it.image;
                                else if (Array.isArray(it.image) && it.image[0]) result.imageUrl = it.image[0];
                                else if (it.image.url) result.imageUrl = it.image.url;
                            }
                            if (it.offers) {
                                var offers = Array.isArray(it.offers) ? it.offers : [it.offers];
                                for (var k = 0; k < offers.length; k++) {
                                    var p = parseFloat(offers[k].price);
                                    if (!isNaN(p) && p > 0) {
                                        result.price = p;
                                        break;
                                    }
                                }
                            }
                            if (it.description) {
                                var desc = it.description;
                                var dimMatch = desc.match(/(\d{2,3}\s*[xX*×]\s*\d{2,3}\s*cm)/i);
                                if (dimMatch) result.dimensions = dimMatch[1];
                            }
                        }
                    }
                } catch(e) {}
            }

            // 2. Check Next.js / framework state
            if (!result.name || result.price === 0) {
                try {
                    if (window.__NEXT_DATA__ && window.__NEXT_DATA__.props) {
                        var pageProps = window.__NEXT_DATA__.props.pageProps || {};
                        var p = pageProps.product || (pageProps.initialState && pageProps.initialState.product);
                        if (p) {
                            if (!result.name && p.name) result.name = p.name;
                            if (!result.lmNumber && (p.sku || p.referenceNumber || p.id)) {
                                result.lmNumber = String(p.sku || p.referenceNumber || p.id);
                            }
                            if (!result.ean && p.ean) result.ean = String(p.ean);
                            if (result.price === 0 && (p.price || p.currentPrice)) {
                                result.price = parseFloat(p.price || p.currentPrice) || 0;
                            }
                            if (!result.imageUrl && (p.mainImage || (p.images && p.images[0]))) {
                                result.imageUrl = p.mainImage || p.images[0];
                            }
                        }
                    }
                } catch(e) {}
            }

            // 3. Fallback to OpenGraph / Meta tags
            if (!result.name) {
                var ogTitle = document.querySelector('meta[property="og:title"]');
                if (ogTitle && ogTitle.content) {
                    result.name = ogTitle.content.replace(/\s*[-|]\s*Leroy Merlin.*/i, '').trim();
                }
            }
            if (!result.imageUrl) {
                var ogImg = document.querySelector('meta[property="og:image"]');
                if (ogImg && ogImg.content) result.imageUrl = ogImg.content;
            }
            if (result.price === 0) {
                var ogPrice = document.querySelector('meta[property="product:price:amount"]');
                if (ogPrice && ogPrice.content) {
                    result.price = parseFloat(ogPrice.content) || 0;
                }
            }

            // 4. Fallback to DOM elements on Leroy Merlin product page
            if (!result.name) {
                var h1 = document.querySelector('h1[data-qa="product-title"], h1[data-testid="product-title"], h1');
                if (h1) {
                    result.name = h1.innerText.replace(/\s*[-|]\s*Leroy Merlin.*/i, '').trim();
                }
                if (!result.name) {
                    result.name = document.title.replace(/\s*[-|]\s*Leroy Merlin.*/i, '').trim();
                }
            }

            if (result.price === 0) {
                var priceSelectors = [
                    '[data-qa="product-price"]',
                    '[data-testid="product-price"]',
                    '.product-price',
                    '.main-price',
                    '[data-qa="price"]',
                    'span[class*="price-value"]',
                    'span[class*="price_main"]',
                    'div[class*="Price_price"]'
                ];
                for (var s = 0; s < priceSelectors.length; s++) {
                    var el = document.querySelector(priceSelectors[s]);
                    if (el) {
                        var clean = el.innerText.replace(/\s/g, '').replace(',', '.').replace(/[^\d.]/g, '');
                        var parsed = parseFloat(clean);
                        if (!isNaN(parsed) && parsed > 0) {
                            result.price = parsed;
                            break;
                        }
                    }
                }
            }

            // 5. Look for LM Reference Number on page / URL
            if (!result.lmNumber) {
                var urlMatch = window.location.href.match(/p?(\d{7,9})(?:\.html|\?|${'$'}|#|\/)/);
                if (urlMatch) {
                    result.lmNumber = urlMatch[1];
                } else {
                    var refMatch = document.body.innerText.match(/(?:Nr ref|Numer referencyjny|Indeks)\s*[:.]?\s*(\d{7,9})/i);
                    if (refMatch) {
                        result.lmNumber = refMatch[1];
                    }
                }
            }

            // 6. Look for EAN if missing
            if (!result.ean) {
                var eanMatch = document.body.innerText.match(/(?:Kod EAN|EAN|GTIN)\s*[:.]?\s*(\d{8,14})/i);
                if (eanMatch) {
                    result.ean = eanMatch[1];
                }
            }

            // 7. Look for Image if missing
            if (!result.imageUrl) {
                var imgEl = document.querySelector('[data-qa="product-image"] img, [data-testid="product-image"] img, img[class*="gallery"], img[class*="product"]');
                if (imgEl && imgEl.src) {
                    result.imageUrl = imgEl.src;
                }
            }

            // 8. If on a search results page with multiple cards and no product extracted yet
            if (!result.name || result.name.toLowerCase().includes('wyniki wyszukiwania') || result.price === 0) {
                var firstCard = document.querySelector('article[data-qa="product-card"], div[data-qa="product-card"], [data-testid="product-card"], div[class*="product-card"]');
                if (firstCard) {
                    var cardTitle = firstCard.querySelector('h3, h2, [data-qa="product-title"], a[class*="title"]');
                    var cardPrice = firstCard.querySelector('[data-qa="product-price"], [class*="price"]');
                    var cardImg = firstCard.querySelector('img');
                    var cardLink = firstCard.querySelector('a[href*="/produkt/"], a[href*="-p"]');

                    if (cardTitle && cardTitle.innerText.trim()) {
                        result.name = cardTitle.innerText.trim();
                    }
                    if (cardPrice) {
                        var pClean = cardPrice.innerText.replace(/\s/g, '').replace(',', '.').replace(/[^\d.]/g, '');
                        var pNum = parseFloat(pClean);
                        if (!isNaN(pNum)) result.price = pNum;
                    }
                    if (cardImg && cardImg.src) {
                        result.imageUrl = cardImg.src;
                    }
                    if (cardLink && cardLink.href) {
                        var m = cardLink.href.match(/p?(\d{7,9})(?:\.html|\?|${'$'}|#|\/)/);
                        if (m) result.lmNumber = m[1];
                    }
                }
            }

            return JSON.stringify(result);
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
        } catch (e: Exception) {
            Log.e("LeroyMerlinLookup", "Error parsing extracted JS JSON: ${e.message}", e)
        }
        onResult(ExtractedProduct())
    }
}
