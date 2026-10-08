package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Reprezentuje pojedyncze miejsce (slot A lub B) na pałąku ekspozycyjnym.
 */
@IgnoreExtraProperties
data class RugSlot(
    val km: String = "",
    val ean: String = "",
    val nazwa: String = "",
    val rozmiar: String = "",
    val cena: Double? = null,
    val waluta: String = "PLN",
    val productUrl: String = "",
    val productDataStatus: String = "ACTIVE",
    val productDataUpdatedAt: Any? = null,
    val updatedAt: Any? = null,
    val updatedByEmail: String = ""
) {
    val isOccupied: Boolean
        get() = km.isNotBlank()

    val formattedPrice: String
        get() = if (cena != null) String.format("%.2f %s", cena, waluta) else ""
}
