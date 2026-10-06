package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "take_down_orders")
data class TakeDownOrder(
    @PrimaryKey
    val id: String,
    val carpetId: String,
    val carpetName: String,
    val carpetBarcode: String,
    val carpetSize: String,
    val standId: String,
    val standName: String,
    val standCode: String,
    val slot: Int,                  // 1 (Lewe) lub 2 (Prawe)
    val requestedBy: String,        // Imię/urządzenie sprzedawcy
    val notes: String = "",         // np. "Klient czeka przy kasie", "Zapakować na wynos"
    val isCompleted: Boolean = false, // Czy magazynier zdjął dywan
    val completedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val slotName: String
        get() = if (slot == 1) "Miejsce 1 (Lewe / Przód)" else "Miejsce 2 (Prawe / Tył)"
}
