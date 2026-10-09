package com.example.data.model

import com.google.firebase.Timestamp

data class Product(
    val productId: String = "",
    val name: String = "",
    val ean: String = "",
    val lmSystemNumber: String = "",
    val onlinePrice: Double = 0.0,
    val localPrice: Double = 0.0,
    val localPriceOverride: Boolean = false,
    val imageUrl: String = "",
    val productUrl: String = "",
    val dimensions: String = "",
    val composition: String = "",
    val lastUpdated: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedBy: String = ""
) {
    /**
     * Zwraca cenę, która powinna być aktualnie prezentowana na ekspozycji.
     * Jeżeli cena lokalna jest aktywna (localPriceOverride == true), zwraca localPrice.
     * W przeciwnym razie cenę ze strony onlinePrice.
     */
    fun effectivePrice(): Double {
        return if (localPriceOverride && localPrice > 0.0) localPrice else onlinePrice
    }

    fun hasLocalOverride(): Boolean = localPriceOverride && localPrice > 0.0
}
