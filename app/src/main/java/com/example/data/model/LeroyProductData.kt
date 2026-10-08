package com.example.data.model

/**
 * Dane produktu pobrane z katalogu Leroy Merlin.
 */
data class LeroyProductData(
    val km: String = "",
    val ean: String = "",
    val nazwa: String = "",
    val rozmiar: String = "",
    val cena: Double? = null,
    val waluta: String = "PLN",
    val productUrl: String = "",
    val status: String = "ACTIVE",
    val source: String = "leroy_merlin"
)
