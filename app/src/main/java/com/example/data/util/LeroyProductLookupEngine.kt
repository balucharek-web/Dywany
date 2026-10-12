package com.example.data.util

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
 * Rzetelny silnik wyszukiwania i pobierania danych produktów.
 * Nigdy nie wymyśla fałszywych nazw dywanów dla nieznanych kodów (np. masek przeciwpyłowych, narzędzi).
 * 1. Sprawdza produkty zapisane w bazie sklepu (Firestore / lokalne).
 * 2. Sprawdza zweryfikowany katalog wzorcowy Leroy Merlin po ścisłym dopasowaniu kodu.
 * 3. Sprawdza otwarty międzynarodowy rejestr kodów EAN i zwraca prawdziwą nazwę artykułu.
 * 4. W razie braku w bazach uczciwie prosi użytkownika o uzupełnienie danych z etykiety.
 */
object LeroyProductLookupEngine {
    private const val TAG = "LeroyLookupEngine"

    suspend fun lookupProduct(
        rawCode: String,
        existingProducts: List<Product> = emptyList()
    ): ProductLookupResult = withContext(Dispatchers.IO) {
        val clean = rawCode.trim()
        if (clean.isBlank()) {
            return@withContext ProductLookupResult.Error("Wprowadź kod EAN lub numer LM do wyszukania.")
        }

        val parsed = ProductCodeParser.parse(clean)
        val eanToQuery = parsed.ean
        val lmToQuery = parsed.lmNumber

        Log.d(TAG, "Wyszukiwanie produktu dla: '$clean' (EAN: '$eanToQuery', LM: '$lmToQuery')")

        // 1. Sprawdź w dotychczasowych produktach w bazie sklepu
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

        // 3. Sprawdź w otwartym rejestrze kodów kreskowych (Open Products Facts)
        if (eanToQuery.isNotBlank() || clean.length in 8..14) {
            val queryCode = eanToQuery.ifBlank { clean }
            try {
                val eanMatch = BarcodeLookupService.lookupEan(queryCode)
                if (eanMatch != null && eanMatch.name.isNotBlank()) {
                    val realName = eanMatch.name
                    val isRug = realName.contains("dywan", ignoreCase = true) ||
                            realName.contains("chodnik", ignoreCase = true) ||
                            realName.contains("wykładzina", ignoreCase = true) ||
                            realName.contains("rug", ignoreCase = true) ||
                            realName.contains("carpet", ignoreCase = true)

                    val statusMsg = if (isRug) {
                        "Pobrano z rejestru EAN: $realName"
                    } else {
                        "Wykryto artykuł: $realName (Uwaga: to nie jest dywan)"
                    }

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
                        message = statusMsg
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Błąd rejestru EAN: ${e.message}")
            }
        }

        // 4. Jeśli produktu nie znaleziono w żadnej bazie - brak zmyślania!
        return@withContext ProductLookupResult.NotFound(
            "Nie odnaleziono artykułu dla kodu \"$clean\". Uzupełnij nazwę i cenę ręcznie z etykiety."
        )
    }
}
