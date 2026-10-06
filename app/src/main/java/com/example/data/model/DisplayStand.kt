package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "display_stands")
data class DisplayStand(
    @PrimaryKey
    val id: String,                 // np. "STAND-A01"
    val code: String,               // np. "A-01"
    val name: String,               // np. "Stanowisko A-01"
    val section: String,            // np. "Aleja 1 - Dywany Duże (200x300)"
    val barcode: String = "",       // Opcjonalny kod kreskowy stanowiska do natychmiastowego skanowania wieszaka
    val slot1CarpetId: String? = null, // ID dywanu w Miejscu 1 (Lewe / Przód)
    val slot2CarpetId: String? = null, // ID dywanu w Miejscu 2 (Prawe / Tył)
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val occupiedCount: Int
        get() = (if (slot1CarpetId != null) 1 else 0) + (if (slot2CarpetId != null) 1 else 0)

    val isFull: Boolean
        get() = occupiedCount >= 2

    val hasFreeSlot: Boolean
        get() = occupiedCount < 2

    val isCompletelyEmpty: Boolean
        get() = occupiedCount == 0
}
