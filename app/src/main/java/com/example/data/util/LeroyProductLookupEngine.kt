package com.example.data.util

import android.content.Context
import android.util.Log
import com.example.data.model.LeroyMerlinCatalogData
import com.example.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExtractedProduct(
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val lmNumber: String = "",
    val ean: String = "",
    val dimensions: String = "",
    val composition: String = ""
)

sealed class ProductLookupResult {
    data class Success(
        val product: ExtractedProduct,
        val source: String,
        val message: String
    ) : ProductLookupResult()

    data class NotFound(val message: String) : ProductLookupResult()
    data class Error(val message: String) : ProductLookupResult()
}

/**
 * Główny silnik wyszukiwania i pobierania danych produktów.
 * Jeśli produktu nie ma w lokalnej bazie sklepu, pobiera rzeczywiste dane z oficjalnej strony leroymerlin.pl
 * i wkleja je bezpośrednio do pól aplikacji.
 */
object LeroyProductLookupEngine {
    private const val TAG = "LeroyLookupEngine"

    suspend fun lookupProduct(
        rawCode: String,
        existingProducts: List<Product> = emptyList(),
        context: Context? = null
    ): ProductLookupResult = withContext(Dispatchers.IO) {
        val clean = rawCode.trim()
        if (clean.isBlank()) {
            return@withContext ProductLookupResult.Error("Wprowadź kod EAN lub numer LM do wyszukania.")
        }

        val parsed = ProductCodeParser.parse(clean)
        val eanToQuery = parsed.ean
        val lmToQuery = parsed.lmNumber

        Log.d(TAG, "Wyszukiwanie produktu dla: '$clean' (EAN: '$eanToQuery', LM: '$lmToQuery')")

        // 1. Sprawdź w dotychczasowych produktach w bazie sklepu (Firestore / pamięć podręczna)
        val inStore = existingProducts.firstOrNull { p ->
            (lmToQuery.isNotBlank() && p.lmSystemNumber.equals(lmToQuery, ignoreCase = true)) ||
            (eanToQuery.isNotBlank() && p.ean.equals(eanToQuery, ignoreCase = true)) ||
            (clean.length >= 8 && (p.ean == clean || p.lmSystemNumber == clean))
        }
        if (inStore != null) {
            val price = if (inStore.localPriceOverride && inStore.localPrice > 0.0) inStore.localPrice else inStore.onlinePrice
            return@withContext ProductLookupResult.Success(
                product = ExtractedProduct(
                    name = inStore.name,
                    price = price,
                    imageUrl = inStore.imageUrl,
                    lmNumber = inStore.lmSystemNumber,
                    ean = inStore.ean,
                    dimensions = inStore.dimensions,
                    composition = inStore.composition
                ),
                source = "Baza produktów sklepu",
                message = "Znaleziono w bazie sklepu: ${inStore.name}"
            )
        }

        // 2. Sprawdź w oficjalnym katalogu wzorcowym Leroy Merlin
        val inCatalog = LeroyMerlinCatalogData.DEFAULT_RUGS.firstOrNull { p ->
            (lmToQuery.isNotBlank() && p.lmSystemNumber.equals(lmToQuery, ignoreCase = true)) ||
            (eanToQuery.isNotBlank() && p.ean.equals(eanToQuery, ignoreCase = true)) ||
            (clean.length >= 8 && (p.ean == clean || p.lmSystemNumber == clean))
        }
        if (inCatalog != null) {
            return@withContext ProductLookupResult.Success(
                product = ExtractedProduct(
                    name = inCatalog.name,
                    price = inCatalog.onlinePrice,
                    imageUrl = inCatalog.imageUrl,
                    lmNumber = inCatalog.lmSystemNumber,
                    ean = if (eanToQuery.isNotBlank()) eanToQuery else inCatalog.ean,
                    dimensions = inCatalog.dimensions,
                    composition = inCatalog.composition
                ),
                source = "Katalog Leroy Merlin",
                message = "Pobrano z katalogu Leroy Merlin: ${inCatalog.name}"
            )
        }

        // 3. POBRANIE BEZPOŚREDNIO Z OFICJALNEJ STRONY LEROYMERLIN.PL (LIVE HEADLESS SCRAPER)
        if (context != null) {
            // Spróbuj najpierw po 8-cyfrowym numerze referencyjnym LM, jeśli jest, a w drugiej kolejności po EAN / query
            val searchCandidates = mutableListOf<String>()
            if (lmToQuery.isNotBlank()) searchCandidates.add(lmToQuery)
            if (eanToQuery.isNotBlank() && eanToQuery != lmToQuery) searchCandidates.add(eanToQuery)
            if (searchCandidates.isEmpty()) searchCandidates.add(clean)

            for (queryCand in searchCandidates) {
                try {
                    Log.d(TAG, "Próba pobrania ze strony leroymerlin.pl dla: $queryCand")
                    val webExtracted = LeroyMerlinLiveScraper.searchAndExtract(context, queryCand)
                    if (webExtracted != null && webExtracted.name.isNotBlank()) {
                        val finalLm = webExtracted.lmNumber.ifBlank { lmToQuery }
                        val finalEan = webExtracted.ean.ifBlank { eanToQuery }
                        return@withContext ProductLookupResult.Success(
                            product = webExtracted.copy(
                                lmNumber = finalLm,
                                ean = finalEan
                            ),
                            source = "Oficjalna strona leroymerlin.pl",
                            message = "Pobrano dane ze strony leroymerlin.pl: ${webExtracted.name}"
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Błąd pobierania z leroymerlin.pl dla $queryCand: ${e.message}")
                }
            }
        }

        // 4. Sprawdź w otwartym rejestrze kodów kreskowych (Open Products Facts) jako uzupełnienie
        if (eanToQuery.isNotBlank() || clean.length in 8..14) {
            val queryCode = eanToQuery.ifBlank { clean }
            try {
                val eanMatch = BarcodeLookupService.lookupEan(queryCode)
                if (eanMatch != null && eanMatch.name.isNotBlank()) {
                    val realName = eanMatch.name
                    return@withContext ProductLookupResult.Success(
                        product = ExtractedProduct(
                            name = realName,
                            price = eanMatch.onlinePrice,
                            imageUrl = eanMatch.imageUrl,
                            lmNumber = lmToQuery,
                            ean = queryCode,
                            dimensions = eanMatch.dimensions,
                            composition = eanMatch.composition
                        ),
                        source = "Rejestr EAN",
                        message = "Pobrano z rejestru kodów: $realName"
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Błąd rejestru EAN: ${e.message}")
            }
        }

        // 5. Jeśli produkt nie został odnaleziony na leroymerlin.pl ani w bazach
        return@withContext ProductLookupResult.NotFound(
            "Nie odnaleziono artykułu na stronie leroymerlin.pl dla zapytania \"$clean\". Sprawdź 8-cyfrowy numer referencyjny z żółtej etykiety."
        )
    }
}
