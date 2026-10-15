package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["ean"]),
        Index(value = ["kodLm"]),
        Index(value = ["lokalizacja"])
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val kodLm: String,
    val ean: String,
    val nazwa: String,
    val kolekcja: String = "",
    val rozmiar: String,
    val cena: Double,
    val cenaM2: Double = 0.0,
    val ilosc: Int = 1,
    val lokalizacja: String = "1a", // Miejsce na ekspozycji, np. "1a", "1b", "21a"
    val sektor: String = "Ekspozycja",
    val sklad: String = "",
    val kolor: String = "",
    val uwagi: String = "",
    val sklepId: String = "038",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    fun formattedPrice(): String = String.format("%.2f zł", cena)

    fun formattedPriceM2(): String {
        return if (cenaM2 > 0) {
            String.format("%.2f zł/m²", cenaM2)
        } else {
            val dimensions = parseDimensions(rozmiar)
            if (dimensions != null) {
                val areaM2 = (dimensions.first * dimensions.second) / 10000.0
                if (areaM2 > 0) {
                    String.format("%.2f zł/m²", cena / areaM2)
                } else ""
            } else ""
        }
    }

    /**
     * Extracts pałąk number, e.g. "21a" -> 21, "5b" -> 5
     */
    fun getPalakNumber(): Int? {
        val digits = lokalizacja.filter { it.isDigit() }
        return digits.toIntOrNull()
    }

    /**
     * Extracts side, e.g. "21a" -> "a", "5b" -> "b"
     */
    fun getPalakSide(): String {
        val lastChar = lokalizacja.trim().lowercase().lastOrNull()
        return if (lastChar == 'a' || lastChar == 'b') lastChar.toString() else "a"
    }

    /**
     * Formatted display e.g. "Pałąk 21a" or "Miejsce 21a"
     */
    fun getPalakDisplay(): String {
        val num = getPalakNumber()
        val side = getPalakSide().uppercase()
        return if (num != null) "Pałąk $num$side" else lokalizacja
    }

    companion object {
        fun parseDimensions(rozmiar: String): Pair<Double, Double>? {
            return try {
                val clean = rozmiar.lowercase().replace("cm", "").replace("m", "").trim()
                val parts = clean.split("x")
                if (parts.size >= 2) {
                    val w = parts[0].trim().toDoubleOrNull() ?: return null
                    val h = parts[1].trim().toDoubleOrNull() ?: return null
                    Pair(w, h)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
