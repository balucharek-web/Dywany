package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

enum class IdentifierType {
    LEROY_KM, // 8 cyfr
    EAN,      // 8, 12, 13 lub 14 cyfr
    UNKNOWN
}

/**
 * Wpis dywanu w indeksie produktów KM z pełnymi danymi produktowymi Leroy Merlin.
 */
@IgnoreExtraProperties
data class Dywan(
    val km: String = "",
    val ean: String = "",
    val nazwa: String = "",
    val rozmiar: String = "",
    val cena: Double? = null,
    val waluta: String = "PLN",
    val productUrl: String = "",
    val palekNumer: Int = 0,
    val miejsce: String = "",
    val slot: String = "A",
    val productDataUpdatedAt: Any? = null,
    val productDataSource: String = "leroy_merlin",
    val productDataStatus: String = "ACTIVE",
    val updatedAt: Any? = null,
    val updatedByEmail: String = ""
) {
    val formattedPrice: String
        get() = if (cena != null) String.format("%.2f %s", cena, waluta) else ""

    companion object {
        /**
         * Walidacja 8-cyfrowego kodu KM Leroy Merlin.
         */
        fun isValidKm(km: String): Boolean {
            return km.trim().matches(Regex("^[0-9]{8}$"))
        }

        /**
         * Walidacja kodu kreskowego EAN (obsługa EAN-8, EAN-12 (UPC), EAN-13, EAN-14).
         */
        fun isValidEan(ean: String): Boolean {
            val trimmed = ean.trim()
            return trimmed.matches(Regex("^[0-9]{8}$")) ||
                   trimmed.matches(Regex("^[0-9]{12,14}$"))
        }

        /**
         * Automatyczne rozpoznanie typu identyfikatora.
         */
        fun detectIdentifierType(input: String): IdentifierType {
            val trimmed = input.trim()
            return when {
                trimmed.matches(Regex("^[0-9]{12,14}$")) -> IdentifierType.EAN
                trimmed.matches(Regex("^[0-9]{8}$")) -> IdentifierType.LEROY_KM
                else -> IdentifierType.UNKNOWN
            }
        }

        /**
         * Walidacja formatu miejsca (np. 1A, 23B).
         */
        fun isValidMiejsce(miejsce: String): Boolean {
            return miejsce.uppercase().matches(Regex("^[1-9][0-9]*[AB]$"))
        }
    }
}
