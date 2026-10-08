package com.example.data.service

import android.util.Log
import com.example.data.model.Dywan
import com.example.data.model.IdentifierType
import com.example.data.model.LeroyProductData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Zintegrowany serwis backendowo-kliencki do pobierania i parsowania danych
 * produktów z oficjalnego katalogu internetowego Leroy Merlin Polska.
 *
 * Architektura:
 * LeroyMerlinProductService
 *   └── InMemoryCache (L1)
 *   └── FirestoreCache (L2 - sprawdzany w repozytorium)
 *   └── LeroyMerlinProvider (zapytanie HTTP i parsowanie danych JSON-LD / HTML / OpenGraph)
 */
class LeroyMerlinProductService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    // Pamięć podręczna L1 (w pamięci procesu) zapobiegająca powtarzającym się zapytaniom
    private val memoryCache = ConcurrentHashMap<String, CachedResult>()

    private data class CachedResult(
        val data: LeroyProductData,
        val timestamp: Long
    )

    companion object {
        private const val TAG = "LeroyProductService"
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 godziny

        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }

    /**
     * Wyszukuje produkt po EAN lub numerze referencyjnym KM Leroy Merlin.
     */
    suspend fun resolveProduct(identifier: String): LeroyProductData = withContext(Dispatchers.IO) {
        val cleanInput = identifier.trim()
        if (cleanInput.isBlank()) {
            return@withContext LeroyProductData(status = "ERROR")
        }

        // 1. Sprawdź pamięć podręczną L1
        val cached = memoryCache[cleanInput]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL_MS) {
            Log.d(TAG, "Pobrano z pamięci podręcznej dla $cleanInput")
            return@withContext cached.data
        }

        val type = Dywan.detectIdentifierType(cleanInput)
        Log.i(TAG, "Rozpoczynam pobieranie produktu Leroy Merlin dla: $cleanInput (typ: $type)")

        val result = try {
            when (type) {
                IdentifierType.LEROY_KM -> fetchByKm(cleanInput)
                IdentifierType.EAN -> fetchByEan(cleanInput)
                IdentifierType.UNKNOWN -> fetchByGeneralQuery(cleanInput)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd pobierania danych produktu dla $cleanInput: ${e.message}", e)
            LeroyProductData(
                km = if (cleanInput.length == 8 && cleanInput.all { it.isDigit() }) cleanInput else "",
                ean = if (cleanInput.length in 8..14 && cleanInput.all { it.isDigit() }) cleanInput else "",
                status = "ERROR"
            )
        }

        // Zapisz w pamięci podręcznej jeśli wynik jest poprawny
        if (result.status == "ACTIVE") {
            memoryCache[cleanInput] = CachedResult(result, System.currentTimeMillis())
            if (result.km.isNotBlank()) memoryCache[result.km] = CachedResult(result, System.currentTimeMillis())
            if (result.ean.isNotBlank()) memoryCache[result.ean] = CachedResult(result, System.currentTimeMillis())
        }

        return@withContext result
    }

    private suspend fun fetchByKm(km: String): LeroyProductData {
        return fetchFromLeroySearch(km, expectedKm = km, expectedEan = null)
    }

    private suspend fun fetchByEan(ean: String): LeroyProductData {
        return fetchFromLeroySearch(ean, expectedKm = null, expectedEan = ean)
    }

    private suspend fun fetchByGeneralQuery(query: String): LeroyProductData {
        return fetchFromLeroySearch(query, expectedKm = null, expectedEan = null)
    }

    /**
     * Wykonuje zapytanie do wyszukiwarki sklepu Leroy Merlin Polska
     * i parsuje strukturyzowane metadane Schema.org / JSON-LD / OpenGraph.
     */
    private fun fetchFromLeroySearch(
        query: String,
        expectedKm: String?,
        expectedEan: String?
    ): LeroyProductData {
        val searchUrl = "https://www.leroymerlin.pl/szukaj?q=${java.net.URLEncoder.encode(query, "UTF-8")}"

        val request = Request.Builder()
            .url(searchUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Odpowiedź HTTP ${response.code} dla wyszukiwania $query")
                    return LeroyProductData(
                        km = expectedKm ?: "",
                        ean = expectedEan ?: "",
                        status = if (response.code == 404) "NOT_FOUND" else "ERROR"
                    )
                }

                val html = response.body?.string() ?: ""
                val resolvedUrl = response.request.url.toString()

                val parsed = parseLeroyHtml(html, resolvedUrl, query, expectedKm, expectedEan)
                return parsed
            }
        } catch (e: IOException) {
            Log.e(TAG, "Problem sieciowy podczas łączenia z Leroy Merlin: ${e.message}")
            return LeroyProductData(
                km = expectedKm ?: "",
                ean = expectedEan ?: "",
                status = "ERROR"
            )
        }
    }

    /**
     * Uniwersalny parser metadanych stron produktowych i wyników wyszukiwania Leroy Merlin.
     * Szuka w kolejności:
     * 1. Schema.org JSON-LD (Product)
     * 2. Tagi OpenGraph / Meta
     * 3. Wzorców tekstu i wymiarów w HTML
     */
    fun parseLeroyHtml(
        html: String,
        sourceUrl: String,
        query: String,
        expectedKm: String?,
        expectedEan: String?
    ): LeroyProductData {
        // 1. Sprawdź Schema.org JSON-LD
        val jsonLdMatch = Pattern.compile("<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>", Pattern.DOTALL)
            .matcher(html)

        var foundName = ""
        var foundPrice: Double? = null
        var foundCurrency = "PLN"
        var foundKm = expectedKm ?: ""
        var foundEan = expectedEan ?: ""
        var foundUrl = sourceUrl

        while (jsonLdMatch.find()) {
            val jsonContent = jsonLdMatch.group(1)?.trim() ?: continue
            try {
                if (jsonContent.startsWith("{")) {
                    val jsonObj = JSONObject(jsonContent)
                    if (extractFromJsonLd(jsonObj, { foundName = it }, { foundPrice = it }, { foundCurrency = it }, { foundKm = it }, { foundEan = it }, { foundUrl = it })) {
                        break
                    }
                } else if (jsonContent.startsWith("[")) {
                    val jsonArray = JSONArray(jsonContent)
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.optJSONObject(i) ?: continue
                        if (extractFromJsonLd(item, { foundName = it }, { foundPrice = it }, { foundCurrency = it }, { foundKm = it }, { foundEan = it }, { foundUrl = it })) {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                // Niepoprawny JSON-LD w tagu, kontynuuj przeszukiwanie
            }
        }

        // 2. Jeśli nie znaleziono przez JSON-LD, przeszukaj metatagi OpenGraph
        if (foundName.isBlank()) {
            foundName = extractMetaTag(html, "og:title")
                .replace(" - Leroy Merlin", "")
                .replace(" w sklepach Leroy Merlin", "")
                .trim()
        }

        if (foundPrice == null) {
            val ogPrice = extractMetaTag(html, "product:price:amount")
            if (ogPrice.isNotBlank()) {
                foundPrice = ogPrice.replace(",", ".").toDoubleOrNull()
            }
        }

        if (foundKm.isBlank()) {
            // Spróbuj wyciągnąć 8 cyfr z URL lub tytułu
            val kmFromUrl = Pattern.compile("[,/\\-](\\d{8})(?:\\.html|/|$)").matcher(sourceUrl)
            if (kmFromUrl.find()) {
                foundKm = kmFromUrl.group(1) ?: ""
            }
        }

        // 3. Wykryj wymiary dywanu z nazwy (np. 160x230, 160 x 230 cm, 200x290)
        val extractedSize = extractDimensions(foundName)

        if (foundName.isNotBlank() || foundPrice != null) {
            return LeroyProductData(
                km = foundKm.ifBlank { if (query.matches(Regex("^[0-9]{8}$"))) query else "" },
                ean = foundEan.ifBlank { if (query.matches(Regex("^[0-9]{12,14}$"))) query else "" },
                nazwa = foundName,
                rozmiar = extractedSize,
                cena = foundPrice,
                waluta = foundCurrency,
                productUrl = foundUrl,
                status = "ACTIVE",
                source = "leroy_merlin"
            )
        }

        // Jeżeli strona nie zawierała danych produktu
        return LeroyProductData(
            km = expectedKm ?: if (query.matches(Regex("^[0-9]{8}$"))) query else "",
            ean = expectedEan ?: if (query.matches(Regex("^[0-9]{12,14}$"))) query else "",
            status = "NOT_FOUND"
        )
    }

    private fun extractFromJsonLd(
        obj: JSONObject,
        onName: (String) -> Unit,
        onPrice: (Double) -> Unit,
        onCurrency: (String) -> Unit,
        onKm: (String) -> Unit,
        onEan: (String) -> Unit,
        onUrl: (String) -> Unit
    ): Boolean {
        val type = obj.optString("@type", "")
        if (type.equals("Product", ignoreCase = true)) {
            val name = obj.optString("name", "")
            if (name.isNotBlank()) onName(name)

            val sku = obj.optString("sku", "")
            if (sku.matches(Regex("^[0-9]{8}$"))) onKm(sku)

            val gtin = obj.optString("gtin13", obj.optString("gtin", ""))
            if (gtin.isNotBlank()) onEan(gtin)

            val url = obj.optString("url", "")
            if (url.isNotBlank()) onUrl(url)

            val offers = obj.optJSONObject("offers")
            if (offers != null) {
                val priceVal = offers.optDouble("price", -1.0)
                if (priceVal > 0) onPrice(priceVal)

                val curr = offers.optString("priceCurrency", "")
                if (curr.isNotBlank()) onCurrency(curr)
            }
            return true
        }
        return false
    }

    private fun extractMetaTag(html: String, property: String): String {
        val pattern = Pattern.compile("<meta[^>]+(?:property|name)=[\"']$property[\"'][^>]+content=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: ""
        }
        return ""
    }

    /**
     * Wyszukuje formaty rozmiarów charakterystyczne dla dywanów (np. 160 x 230 cm, 200x300, 80x150 cm).
     */
    fun extractDimensions(text: String): String {
        val regex = Pattern.compile("(\\d{2,3}\\s*(?:x|×|X)\\s*\\d{2,3}(?:\\s*cm)?)")
        val matcher = regex.matcher(text)
        if (matcher.find()) {
            var dim = matcher.group(1)?.trim() ?: ""
            if (!dim.lowercase().contains("cm")) {
                dim += " cm"
            }
            return dim
        }
        return ""
    }
}
