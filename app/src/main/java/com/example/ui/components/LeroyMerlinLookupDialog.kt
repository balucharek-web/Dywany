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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

class LeroyWebBridge(
    private val onProductExtracted: (LeroyMerlinProduct) -> Unit,
    private val onPageStatusReported: (String) -> Unit
) {
    @JavascriptInterface
    fun onProductFound(
        title: String,
        priceStr: String,
        size: String,
        collection: String,
        composition: String,
        refCode: String,
        url: String
    ) {
        Handler(Looper.getMainLooper()).post {
            val price = priceStr.toDoubleOrNull()
            if (LeroyMerlinParser.isValidProductTitle(title)) {
                onProductExtracted(
                    LeroyMerlinProduct(
                        title = LeroyMerlinParser.cleanProductTitle(title),
                        pricePln = price,
                        size = size.ifBlank { LeroyMerlinParser.extractSize(title) },
                        collection = collection.ifBlank { LeroyMerlinParser.extractCollection(title) },
                        composition = composition.ifBlank { LeroyMerlinParser.extractComposition(title) },
                        refCode = refCode.ifBlank { LeroyMerlinParser.extractRefCode(url) },
                        productUrl = url
                    )
                )
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Przeglądarka wbudowana, 1: Aplikacja LM / Schowek, 2: Szablony LM

    // Pola edycyjne dla znalezionego produktu (użytkownik może je poprawić przed wstawieniem)
    var editTitle by remember { mutableStateOf("") }
    var editPrice by remember { mutableStateOf("") }
    var editSize by remember { mutableStateOf("") }
    var editCollection by remember { mutableStateOf("") }
    var editComposition by remember { mutableStateOf("") }

    fun updateEditableProduct(product: LeroyMerlinProduct) {
        extractedProduct = product
        editTitle = product.title
        editPrice = product.pricePln?.toInt()?.toString() ?: ""
        editSize = product.size
        editCollection = product.collection
        editComposition = product.composition
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
                Toast.makeText(context, "Wklejono tekst do wyszukania", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Nie można otworzyć przeglądarki", Toast.LENGTH_SHORT).show()
        }
    }

    val extractionJs = """
        (function() {
            function analyze() {
                var bodyText = (document.body ? document.body.innerText : '') || '';
                var lowerBody = bodyText.toLowerCase();

                // 1. Sprawdź czy to 404
                var h1 = (document.querySelector('h1')?.innerText || '').trim();
                var docTitle = (document.title || '').trim();
                if (h1.includes('404') || h1.toLowerCase().includes('nie znaleziono strony') || docTitle.includes('404')) {
                    if (window.AndroidLeroyBridge && window.AndroidLeroyBridge.onPageStatus) {
                        window.AndroidLeroyBridge.onPageStatus('404');
                    }
                    return;
                }

                // 2. Sprawdź czy to Captcha / DataDome
                if (lowerBody.includes('datadome') || lowerBody.includes('geo.captcha') || lowerBody.includes('please enable js') || lowerBody.includes('weryfikacja anty-bot')) {
                    if (window.AndroidLeroyBridge && window.AndroidLeroyBridge.onPageStatus) {
                        window.AndroidLeroyBridge.onPageStatus('CAPTCHA');
                    }
                    return;
                }

                // 3. Sprawdź czy to strona główna bez wyników
                if (docTitle.toLowerCase().includes('sklepy budowlano-dekoracyjne') || h1.toLowerCase().includes('sklepy budowlano-dekoracyjne')) {
                    if (window.AndroidLeroyBridge && window.AndroidLeroyBridge.onPageStatus) {
                        window.AndroidLeroyBridge.onPageStatus('HOMEPAGE');
                    }
                    return;
                }

                // 4. Szukanie tytułu produktu
                var productTitle = '';
                var titleElem = document.querySelector('[data-qa="product-title"]') ||
                                document.querySelector('h1[data-qa]') ||
                                document.querySelector('.product-detail__title') ||
                                document.querySelector('.product-title');

                if (titleElem && titleElem.innerText && titleElem.innerText.trim().length > 3) {
                    productTitle = titleElem.innerText.trim();
                } else if (h1.length > 5 && !h1.toLowerCase().includes('leroy merlin') && !h1.toLowerCase().includes('wyniki wyszukiwania')) {
                    productTitle = h1;
                }

                if (!productTitle) {
                    var firstCard = document.querySelector('article h3 a') ||
                                    document.querySelector('[data-qa="product-card"] h3 a') ||
                                    document.querySelector('.product-card h3 a') ||
                                    document.querySelector('[data-qa="product-card"] a[data-qa="product-title"]');
                    if (firstCard && firstCard.innerText && firstCard.innerText.trim().length > 3) {
                        productTitle = firstCard.innerText.trim();
                    }
                }

                if (productTitle) {
                    productTitle = productTitle.replace(/[\r\n]+/g, ' ').replace(/\s*[-–|]\s*Leroy Merlin.*$/i, '').trim();

                    var lowerP = productTitle.toLowerCase();
                    if (lowerP.includes('404') || lowerP.includes('nie znaleziono') || lowerP.includes('brak wyników') || lowerP.includes('sklepy budowlano-dekoracyjne')) {
                        return;
                    }

                    var price = '';
                    var priceElem = document.querySelector('[data-qa="main-price"]') ||
                                    document.querySelector('[data-qa="product-price"]') ||
                                    document.querySelector('.price-value') ||
                                    document.querySelector('.price') ||
                                    document.querySelector('meta[property="product:price:amount"]');
                    if (priceElem) {
                        var rawPrice = priceElem.innerText || priceElem.getAttribute('content') || '';
                        rawPrice = rawPrice.replace(/[^\d.,]/g, '').replace(',', '.').trim();
                        price = rawPrice;
                    }

                    var size = '';
                    var sizeMatch = productTitle.match(/\b\d{2,3}\s*[xX*×]\s*\d{2,3}(?:\s*cm)?\b/);
                    if (sizeMatch) {
                        size = sizeMatch[0].replace(/\s+/g, ' ');
                        if (!size.toLowerCase().includes('cm')) size += ' cm';
                    }

                    var collection = '';
                    var known = ['Inspire', 'Artens', 'Agnella', 'Ragolle', 'Balta', 'Lano', 'Osta', 'Soudal'];
                    for (var i = 0; i < known.length; i++) {
                        var reg = new RegExp('\\b' + known[i] + '\\b', 'i');
                        if (reg.test(productTitle)) {
                            collection = known[i];
                            break;
                        }
                    }

                    var composition = '';
                    if (productTitle.toLowerCase().includes('wełn') || lowerBody.includes('100% wełna')) {
                        composition = '100% Wełna';
                    } else if (lowerBody.includes('polipropylen') || lowerBody.includes('heat-set')) {
                        composition = '100% Polipropylen';
                    } else if (lowerBody.includes('poliester') || lowerBody.includes('rabbit') || lowerBody.includes('shaggy')) {
                        composition = '100% Poliester';
                    } else if (lowerBody.includes('juta')) {
                        composition = '100% Juta';
                    } else if (lowerBody.includes('bawełna')) {
                        composition = '100% Bawełna';
                    }

                    var ref = '';
                    var urlMatch = window.location.href.match(/-(\d{7,9})\.html/);
                    if (urlMatch) {
                        ref = urlMatch[1];
                    }

                    if (window.AndroidLeroyBridge && window.AndroidLeroyBridge.onProductFound) {
                        window.AndroidLeroyBridge.onProductFound(productTitle, price, size, collection, composition, ref, window.location.href);
                    }
                }
            }

            analyze();
            var count = 0;
            var t = setInterval(function() {
                count++;
                analyze();
                if (count > 6) clearInterval(t);
            }, 1500);
        })();
    """.trimIndent()

    fun triggerDomExtraction() {
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
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(640.dp)
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Nagłówek
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Wyszukiwarka Leroy Merlin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Szybkie pobieranie danych i etykiet dywanów",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Zamknij")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Górny pasek wyszukiwania z możliwością edycji i wklejenia
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        Icon(Icons.Default.ContentPaste, contentDescription = "Wklej ze schowka", modifier = Modifier.size(16.dp))
                    }
                }

                // Szybkie chipy zapytań
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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

                // KARTA ROZPOZNANEGO PRODUKTU (Gdy znaleziono lub wklejono dane)
                if (extractedProduct != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
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
                                        text = "Rozpoznano produkt w Leroy Merlin!",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20),
                                        fontSize = 12.sp
                                    )
                                }

                                IconButton(
                                    onClick = { extractedProduct = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Usuń podgląd", modifier = Modifier.size(14.dp))
                                }
                            }

                            // Edytowalny tytuł
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text("Model / Tytuł", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = editPrice,
                                    onValueChange = { editPrice = it },
                                    label = { Text("Cena (zł)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editSize,
                                    onValueChange = { editSize = it },
                                    label = { Text("Wymiary", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editCollection,
                                    onValueChange = { editCollection = it },
                                    label = { Text("Kolekcja", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Button(
                                onClick = {
                                    val finalProduct = extractedProduct!!.copy(
                                        title = editTitle.trim(),
                                        pricePln = editPrice.toDoubleOrNull(),
                                        size = editSize.trim(),
                                        collection = editCollection.trim(),
                                        composition = editComposition.trim(),
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
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Brak produktu dla kodu '$searchQuery' w Leroy Merlin (Błąd 404). Użyj szablonu lub wyszukaj po nazwie.",
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
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE3F2FD)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Strona główna sklepu. Wpisz nazwę w polu wyszukiwarki lub otwórz w aplikacji Leroy Merlin.",
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
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Weryfikacja anty-botowa. Rozwiąż ją na podglądzie poniżej, a dane natychmiast się odczytają.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }

                // Zakładki trybów pracy
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
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

                Spacer(modifier = Modifier.height(8.dp))

                // ZAWARTOŚĆ ZAKŁADEK
                when (selectedTab) {
                    0 -> {
                        // Zakładka 0: Podgląd sklepu WebView
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            // Pasek narzędzi przeglądarki
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
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
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Button(
                                        onClick = { triggerDomExtraction() },
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
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
                                                    onProductExtracted = { product ->
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
                            }
                        }
                    }

                    1 -> {
                        // Zakładka 1: Otwórz w aplikacji Leroy Merlin / Wklej ze schowka
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Najwygodniejsza metoda dla pracowników sklepu:",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "1. Kliknij poniższy przycisk, aby otworzyć wyszukiwanie bezpośrednio w aplikacji Leroy Merlin lub Chrome na telefonie.\n" +
                                               "2. W aplikacji LM znajdź produkt i kliknij Udostępnij / Kopiuj link (albo skopiuj nazwę i cenę).\n" +
                                               "3. Wróć tutaj i kliknij 'Wklej ze schowka' — dane zostaną od razu uzupełnione!",
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
                    }

                    2 -> {
                        // Zakładka 2: Szybkie szablony ekspozycji Leroy Merlin
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Szybki wybór popularnych kolekcji w Leroy Merlin:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Marki własne i popularne
                            Text(text = "1. Kolekcja / Marka:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Inspire", "Artens", "Agnella", "Lano", "Ragolle", "Balta").forEach { col ->
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

                            // Rozmiary dywanów w LM
                            Text(text = "2. Typowy wymiar dywanu:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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

                            // Skład surowcowy
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

                            Button(
                                onClick = {
                                    val title = editTitle.ifBlank { "Dywan ${editCollection.ifBlank { "Ekspozycja" }} ${editSize.ifBlank { "160x230 cm" }}" }
                                    val templateProduct = LeroyMerlinProduct(
                                        title = title.trim(),
                                        pricePln = editPrice.toDoubleOrNull() ?: 299.0,
                                        size = editSize.ifBlank { "160x230 cm" },
                                        collection = editCollection.ifBlank { "Inspire" },
                                        composition = editComposition.ifBlank { "100% Polipropylen Heat-Set" },
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

                Spacer(modifier = Modifier.height(10.dp))

                // Przyciski dolne
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                    size = editSize.trim(),
                                    collection = editCollection.trim(),
                                    composition = editComposition.trim(),
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
