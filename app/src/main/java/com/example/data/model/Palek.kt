package com.example.data.model

import com.google.firebase.Timestamp

data class Palek(
    val id: String = "",
    val number: Int = 0,
    val slotA: RugSlot = RugSlot(),
    val slotB: RugSlot = RugSlot(),
    val updatedAt: Timestamp? = null,
    val updatedBy: String? = null
)
