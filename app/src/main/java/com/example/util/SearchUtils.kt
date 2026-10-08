package com.example.util

import com.example.model.DisplayAssignment
import com.example.model.Product

object SearchUtils {

    private val spotRegex = Regex("""^(\d{1,4})\s*([a-bA-B])$""")

    /**
     * Parses a query string into a (poleNumber, spotLetter) pair if it represents a spot format (e.g. "23A", "1B").
     * Returns null if query does not match the spot pattern.
     */
    fun parseSpotQuery(query: String): Pair<Int, String>? {
        val trimmed = query.trim()
        val match = spotRegex.matchEntire(trimmed) ?: return null
        val poleNumber = match.groupValues[1].toIntOrNull() ?: return null
        val spotLetter = match.groupValues[2].uppercase()
        if (spotLetter != "A" && spotLetter != "B") return null
        return Pair(poleNumber, spotLetter)
    }

    /**
     * Normalizes an EAN code read from a scanner or input.
     * CRITICAL: NEVER TRUNCATES the barcode. Retains full string length (e.g. 13 digits 5901234567890).
     */
    fun sanitizeBarcode(raw: String): String {
        return raw.trim()
    }

    /**
     * Normalizes a Leroy Merlin system number (usually 8 digits, e.g. "12345678").
     * Kept strictly distinct from EAN.
     */
    fun sanitizeLmSystemNumber(raw: String): String {
        return raw.trim()
    }

    /**
     * Checks if a product matches a given free-form search query.
     */
    fun matchesProduct(product: Product, query: String): Boolean {
        val clean = query.trim()
        if (clean.isBlank()) return false

        // 1. Direct or partial EAN match (preserving all digits)
        if (product.ean.contains(clean, ignoreCase = true)) return true

        // 2. Direct or partial LM system number match
        if (product.lmSystemNumber.contains(clean, ignoreCase = true)) return true

        // 3. Name match
        if (product.name.contains(clean, ignoreCase = true)) return true

        // 4. Description match
        if (product.description.contains(clean, ignoreCase = true)) return true

        return false
    }

    /**
     * Standard spotId builder: e.g. (23, "A") -> "23A"
     */
    fun formatSpotId(poleNumber: Int, spotLetter: String): String {
        return "${poleNumber}${spotLetter.uppercase()}"
    }
}
