package com.example.data.model

import com.google.firebase.Timestamp

data class ProductPriceHistory(
    val id: String = "",
    val lmNumber: String = "",
    val price: Double = 0.0,
    val date: Timestamp? = null
)
