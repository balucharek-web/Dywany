package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Rejestr audytowy zmian cen produktów na ekspozycji.
 */
@IgnoreExtraProperties
data class ProductPriceHistory(
    val id: String = "",
    val km: String = "",
    val oldPrice: Double? = null,
    val newPrice: Double = 0.0,
    val changedAt: Any? = null,
    val source: String = "leroy_merlin",
    val changedBy: String = ""
)
