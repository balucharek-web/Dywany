package com.example.data.util

object ProductCodeParser {
    // Detect rack places like 1A, 23B, 100A
    private val PLACE_REGEX = Regex("^[0-9]{1,3}[A-Ba-b]$")

    // Exact 8-digit LM system number (referencyjny numer LM ze zdjęcia etykiety)
    private val EXACT_LM_8DIGIT_REGEX = Regex("^[0-9]{8}$")

    // Exact EAN barcode (12 to 14 digits)
    private val EXACT_EAN_REGEX = Regex("^[0-9]{12,14}$")

    // Substring extractors for scanner raw outputs (np. 3276000719250APS lub spacje)
    private val EXTRACT_EAN_13 = Regex("[0-9]{13}")
    private val EXTRACT_LM_8 = Regex("(?<![0-9])[0-9]{8}(?![0-9])")

    /**
     * Checks if the query represents an in-store rack place (e.g. 1A, 23B).
     */
    fun isDisplaySpot(query: String): Boolean {
        return PLACE_REGEX.matches(query.trim())
    }

    /**
     * Parses scanned barcode or entered text and determines whether it is an EAN,
     * an 8-digit LM number, or a rack display place.
     */
    fun parse(input: String): ParsedProductCode {
        val raw = input.trim()
        if (raw.isBlank()) {
            return ParsedProductCode(rawInput = raw)
        }

        if (isDisplaySpot(raw)) {
            return ParsedProductCode(
                isPlace = true,
                rawInput = raw
            )
        }

        // 1. Direct match on 8-digit LM number (np. 82325238 z etykiety)
        if (EXACT_LM_8DIGIT_REGEX.matches(raw)) {
            return ParsedProductCode(
                lmNumber = raw,
                rawInput = raw
            )
        }

        // 2. Direct match on EAN (np. 3276000719250 z etykiety)
        if (EXACT_EAN_REGEX.matches(raw)) {
            return ParsedProductCode(
                ean = raw,
                rawInput = raw
            )
        }

        // 3. Extract 13-digit EAN if barcode scanner appends suffix (np. 3276000719250APS)
        val eanMatch = EXTRACT_EAN_13.find(raw)
        if (eanMatch != null) {
            return ParsedProductCode(
                ean = eanMatch.value,
                rawInput = raw
            )
        }

        // 4. Extract standalone 8-digit LM number
        val lmMatch = EXTRACT_LM_8.find(raw)
        if (lmMatch != null) {
            return ParsedProductCode(
                lmNumber = lmMatch.value,
                rawInput = raw
            )
        }

        // Fallback: general query
        return ParsedProductCode(rawInput = raw)
    }
}

data class ParsedProductCode(
    val lmNumber: String = "",
    val ean: String = "",
    val isPlace: Boolean = false,
    val rawInput: String = ""
) {
    val isLmNumber: Boolean get() = lmNumber.isNotBlank()
    val isEan: Boolean get() = ean.isNotBlank()
    val hasValidCatalogCode: Boolean get() = isLmNumber || isEan

    /**
     * Zwraca kod do wyszukania w serwisie Leroy Merlin (EAN lub 8-cyfrowy nr LM).
     * Zwraca null, jeżeli wpisano miejsce (np. 23A) lub brak kodu.
     */
    fun getLeroySearchQuery(): String? {
        if (isPlace) return null
        return lmNumber.ifBlank { ean }.ifBlank { null }
    }
}
