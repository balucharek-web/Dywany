package com.example.data.model

import com.google.firebase.Timestamp

data class Dywan(
    val id: String = "",
    val ean: String = "",              // Pełny kod EAN (np. 13 cyfr, nieobcinany)
    val lmNumber: String = "",         // 8-cyfrowy numer produktu Leroy Merlin
    val name: String = "",
    val size: String = "",
    val price: Double = 0.0,
    val palekNumber: Int? = null,
    val slot: String? = null,          // "A" lub "B"
    val imageUrl: String? = null,
    val productUrl: String? = null,
    val lastUpdated: Timestamp? = null
)
