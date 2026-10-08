package com.example.data.model

import com.google.firebase.Timestamp

data class LeroyProductData(
    val ean: String = "",
    val lmNumber: String = "",
    val name: String = "",
    val size: String = "",
    val price: Double = 0.0,
    val promoPrice: Double? = null,
    val imageUrl: String? = null,
    val productUrl: String? = null,
    val status: String = "AVAILABLE", // AVAILABLE, OUT_OF_STOCK, DISCONTINUED
    val lastUpdated: Timestamp? = null
)
