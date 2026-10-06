package com.example.data.model

data class LeroyMerlinProduct(
    val title: String,
    val pricePln: Double? = null,
    val size: String = "",
    val collection: String = "",
    val composition: String = "",
    val barcode: String = "",
    val productUrl: String = "",
    val imageUrl: String = "",
    val refCode: String = ""
)
