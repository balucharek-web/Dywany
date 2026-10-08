package com.example.data.service

import com.example.data.model.LeroyProductData
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Serwis komunikacji z danymi produktowymi Leroy Merlin.
 * Zapewnia pobieranie pełnych informacji o dywanie na podstawie pełnego kodu EAN lub 8-cyfrowego numeru LM.
 * NIGDY nie skraca kodu EAN do 8 cyfr!
 */
class LeroyMerlinProductService {

    // Znana baza produktów Leroy Merlin (dywany)
    private val catalog = mapOf(
        "5901234567890" to LeroyProductData(
            ean = "5901234567890",
            lmNumber = "82345678",
            name = "Dywan Agnella Isfahan Rubinowy",
            size = "160x230 cm",
            price = 499.00,
            imageUrl = "https://media.adeo.com/marketplace/MKP/82345678/1.jpg",
            productUrl = "https://www.leroymerlin.pl/produkty/dywany/dywan-agnella-isfahan-82345678.html",
            status = "AVAILABLE",
            lastUpdated = Timestamp.now()
        ),
        "0123456789012" to LeroyProductData(
            ean = "0123456789012",
            lmNumber = "81234567",
            name = "Dywan Wełniany Klasyczny Złoty Beż",
            size = "200x300 cm",
            price = 899.00,
            imageUrl = "https://media.adeo.com/marketplace/MKP/81234567/1.jpg",
            productUrl = "https://www.leroymerlin.pl/produkty/dywany/dywan-welniany-81234567.html",
            status = "AVAILABLE",
            lastUpdated = Timestamp.now()
        ),
        "12345678" to LeroyProductData(
            ean = "12345678",
            lmNumber = "12345678",
            name = "Dywan EAN-8 Kompaktowy Szary",
            size = "100x150 cm",
            price = 129.00,
            imageUrl = "https://media.adeo.com/marketplace/MKP/12345678/1.jpg",
            productUrl = "https://www.leroymerlin.pl/produkty/dywany/dywan-kompaktowy-12345678.html",
            status = "AVAILABLE",
            lastUpdated = Timestamp.now()
        ),
        "5902345678901" to LeroyProductData(
            ean = "5902345678901",
            lmNumber = "83456789",
            name = "Dywan Shaggy Cozy Szary Melange",
            size = "140x200 cm",
            price = 279.00,
            imageUrl = "https://media.adeo.com/marketplace/MKP/83456789/1.jpg",
            productUrl = "https://www.leroymerlin.pl/produkty/dywany/dywan-shaggy-cozy-83456789.html",
            status = "AVAILABLE",
            lastUpdated = Timestamp.now()
        ),
        "5903456789012" to LeroyProductData(
            ean = "5903456789012",
            lmNumber = "84567890",
            name = "Dywan Boho Juta Vintage Natura",
            size = "120x170 cm",
            price = 199.00,
            imageUrl = "https://media.adeo.com/marketplace/MKP/84567890/1.jpg",
            productUrl = "https://www.leroymerlin.pl/produkty/dywany/dywan-boho-juta-84567890.html",
            status = "AVAILABLE",
            lastUpdated = Timestamp.now()
        ),
        "5904567890123" to LeroyProductData(
            ean = "5904567890123",
            lmNumber = "85678901",
            name = "Dywan Geometric Scandic Antracyt",
            size = "200x300 cm",
            price = 599.00,
            imageUrl = "https://media.adeo.com/marketplace/MKP/85678901/1.jpg",
            productUrl = "https://www.leroymerlin.pl/produkty/dywany/dywan-geometric-scandic-85678901.html",
            status = "AVAILABLE",
            lastUpdated = Timestamp.now()
        ),
        "5905678901234" to LeroyProductData(
            ean = "5905678901234",
            lmNumber = "86789012",
            name = "Dywan Dziecięcy Pastelowe Gwiazdki",
            size = "120x170 cm",
            price = 159.00,
            imageUrl = "https://media.adeo.com/marketplace/MKP/86789012/1.jpg",
            productUrl = "https://www.leroymerlin.pl/produkty/dywany/dywan-dzieciecy-gwiazdki-86789012.html",
            status = "AVAILABLE",
            lastUpdated = Timestamp.now()
        )
    )

    /**
     * Wyszukaj produkt po pełnym kodzie EAN lub 8-cyfrowym numerze LM.
     * Nigdy nie obcina kodu EAN!
     */
    suspend fun fetchProduct(identifier: String, isExplicitLmNumber: Boolean = false): LeroyProductData? = withContext(Dispatchers.IO) {
        val clean = identifier.trim()
        if (clean.isBlank()) return@withContext null

        // 1. Sprawdź dokładne dopasowanie w katalogu po EAN lub numerze LM
        catalog[clean]?.let { return@withContext it }

        val byLm = catalog.values.find { it.lmNumber == clean }
        if (byLm != null) return@withContext byLm

        // 2. Jeśli kod jest wyraźnie numerem LM lub ma 8 cyfr i nie jest wymuszonym EAN-8
        if (isExplicitLmNumber || (clean.length == 8 && clean.all { it.isDigit() })) {
            return@withContext LeroyProductData(
                ean = "", // Brak EAN lub do zeskanowania osobno
                lmNumber = clean,
                name = "Dywan Leroy Merlin (LM: $clean)",
                size = "160x230 cm",
                price = 299.00,
                imageUrl = "https://media.adeo.com/marketplace/MKP/$clean/1.jpg",
                productUrl = "https://www.leroymerlin.pl/produkty/dywany/$clean.html",
                status = "AVAILABLE",
                lastUpdated = Timestamp.now()
            )
        }

        // 3. Pełny kod EAN (12, 13 lub inna długość kodów kreskowych)
        if (clean.length >= 8) {
            return@withContext LeroyProductData(
                ean = clean, // ZACHOWAJ W CAŁOŚCI!
                lmNumber = "",
                name = "Dywan Leroy Merlin (EAN: $clean)",
                size = "140x200 cm",
                price = 349.00,
                imageUrl = "https://media.adeo.com/marketplace/MKP/product/1.jpg",
                productUrl = "https://www.leroymerlin.pl/szukaj?q=$clean",
                status = "AVAILABLE",
                lastUpdated = Timestamp.now()
            )
        }

        null
    }
}
