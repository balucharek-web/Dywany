package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CarpetStatus {
    ON_DISPLAY,   // Na ekspozycji
    IN_STORAGE,   // W magazynie
    RESERVED      // Zarezerwowany dla klienta
}

@Entity(tableName = "carpets")
data class Carpet(
    @PrimaryKey
    val id: String,
    val barcode: String,            // Kod kreskowy etykiety elektronicznej (ESL / EAN)
    val name: String,               // Nazwa / Model dywanu
    val size: String,               // Wymiary np. "160x230 cm", "200x300 cm"
    val collection: String,         // Kolekcja np. "Klasyczna Persja", "Shaggy", "Modern"
    val composition: String,        // Skład np. "100% Wełna", "Polipropylen"
    val pricePln: Double,           // Cena regularna w PLN
    val promoPricePln: Double? = null, // Cena promocyjna z etykiety ESL
    val currentStandId: String? = null, // ID stanowiska ekspozycyjnego (jeśli wisi)
    val currentSlot: Int? = null,   // Slot 1 (Lewe) lub Slot 2 (Prawe)
    val status: CarpetStatus = CarpetStatus.IN_STORAGE,
    val patternType: Int = 0,       // Indeks wzoru wizualnego (0-5)
    val notes: String = "",         // Dodatkowe uwagi
    val displaySinceTimestamp: Long? = null, // Czas powieszenia na ekspozycji (do rotacji)
    val reservedForName: String? = null,    // Imię/nazwisko klienta rezerwującego
    val reservedPhone: String? = null,      // Telefon klienta
    val reservedUntilTime: Long? = null,    // Do kiedy obowiązuje rezerwacja
    val updatedAt: Long = System.currentTimeMillis() // Timestamp modyfikacji (do synchronizacji P2P)
) {
    val isReserved: Boolean
        get() = status == CarpetStatus.RESERVED || !reservedForName.isNullOrBlank()

    val daysOnDisplay: Int
        get() {
            val since = displaySinceTimestamp ?: return 0
            val diffMs = System.currentTimeMillis() - since
            return (diffMs / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        }
}
