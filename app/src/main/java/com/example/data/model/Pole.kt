package com.example.data.model

import com.google.firebase.Timestamp

data class Pole(
    val poleId: String = "",
    val number: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
