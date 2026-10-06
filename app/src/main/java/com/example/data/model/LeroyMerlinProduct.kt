package com.example.data.model

data class LeroyMerlinProduct(
    val title: String,
    val pricePln: Double? = null,
    val promoPricePln: Double? = null,
    val size: String = "",
    val collection: String = "",
    val composition: String = "",
    val barcode: String = "",
    val productUrl: String = "",
    val imageUrl: String = "",
    val refCode: String = "",
    val color: String = "",
    val pileHeightMm: String = "",
    val weightGsm: String = "",
    val description: String = "",
    val patternSuggestion: Int = 0
)
