package com.example.data.util

import android.net.Uri
import com.example.data.model.LeroyMerlinProduct
import java.util.Locale

object LeroyMerlinParser {

    private val INVALID_TITLE_KEYWORDS = listOf(
        "404",
        "nie znaleziono strony",
        "nie znaleziono",
        "brak wyników",
        "sklepy budowlano-dekoracyjne",
        "zapraszamy do naszych sklepów",
        "strona główna",
        "weryfikacja",
        "captcha",
        "datadome",
        "robot",
        "regulamin",
        "polityka prywatności",
        "kupon",
        "błąd",
        "zaloguj",
        "koszyk",
        "dostęp zablokowany",
        "prosimy o cierpliwość",
        "please enable js"
    )

    private val KNOWN_COLLECTIONS = listOf(
        "Inspire",
        "Artens",
        "Agnella",
        "Lano",
        "Ragolle",
        "Balta",
        "Osta",
        "Soudal"
    )

    fun isValidProductTitle(title: String): Boolean {
        val trimmed = title.trim()
        if (trimmed.length < 4) return false
        val lower = trimmed.lowercase(Locale.ROOT)
        for (kw in INVALID_TITLE_KEYWORDS) {
            if (lower.contains(kw)) return false
        }
        return true
    }

    fun cleanProductTitle(rawTitle: String): String {
        return rawTitle
            .replace(Regex("[\\r\\n]+"), " ")
            .replace(Regex("\\s*[-–|]\\s*Leroy Merlin.*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*[-–|]\\s*Sklep.*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*[-–|]\\s*Ceny, Opinie.*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun extractSize(text: String): String {
        val clean = text.replace("-x-", "x", ignoreCase = true)
        val regex = Regex("\\b(\\d{2,3})\\s*[xX*×]\\s*(\\d{2,3})(?:\\s*cm)?\\b")
        val match = regex.find(clean) ?: return ""
        val width = match.groupValues[1]
        val length = match.groupValues[2]
        return "${width}x${length} cm"
    }

    fun extractPrice(text: String): Double? {
        val priceRegex = Regex("(\\d{1,4}[.,]\\d{2}|\\d{1,4})\\s*(?:zł|pln)", RegexOption.IGNORE_CASE)
        val match = priceRegex.find(text)
        if (match != null) {
            val numStr = match.groupValues[1].replace(",", ".").trim()
            return numStr.toDoubleOrNull()
        }
        return null
    }

    fun extractCollection(text: String): String {
        for (col in KNOWN_COLLECTIONS) {
            val regex = Regex("\\b$col\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(text)) {
                return col
            }
        }
        return ""
    }

    fun extractComposition(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("wełna") || lower.contains("welna") || lower.contains("wełniany") || lower.contains("welniany") -> "100% Wełna"
            lower.contains("heat-set") || lower.contains("heat set") -> "100% Polipropylen Heat-Set"
            lower.contains("polipropylen") || lower.contains("bcf") -> "100% Polipropylen"
            lower.contains("poliester") || lower.contains("shaggy") || lower.contains("rabbit") -> "100% Poliester"
            lower.contains("juta") -> "100% Juta"
            lower.contains("bawełna") || lower.contains("bawelna") -> "100% Bawełna"
            else -> ""
        }
    }

    fun extractColor(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        val colors = listOf(
            "beżowy" to "Beżowy",
            "bezowy" to "Beżowy",
            "szary" to "Szary",
            "antracyt" to "Antracyt",
            "kremowy" to "Kremowy",
            "biały" to "Biały",
            "bialy" to "Biały",
            "zielony" to "Zielony",
            "niebieski" to "Niebieski",
            "granatowy" to "Granatowy",
            "brązowy" to "Brązowy",
            "brazowy" to "Brązowy",
            "czarny" to "Czarny",
            "terakota" to "Terakota",
            "różowy" to "Różowy",
            "rozowy" to "Różowy",
            "musztardowy" to "Musztardowy",
            "wielokolorowy" to "Wielokolorowy"
        )
        for ((pattern, name) in colors) {
            val reg = Regex("\\b$pattern\\b", RegexOption.IGNORE_CASE)
            if (reg.containsMatchIn(lower)) return name
        }
        return ""
    }

    fun extractPileHeight(text: String): String {
        val reg = Regex("(?:wysokość runa|grubość)[:\\s]*(\\d{1,2}(?:[.,]\\d)?)\\s*mm", RegexOption.IGNORE_CASE)
        val match = reg.find(text)
        if (match != null) return "${match.groupValues[1]} mm"
        val generalMm = Regex("\\b(\\d{1,2})\\s*mm\\b", RegexOption.IGNORE_CASE).find(text)
        return generalMm?.value ?: ""
    }

    fun extractWeight(text: String): String {
        val reg = Regex("(?:gramatura|waga)[:\\s]*(\\d{3,4})\\s*g/m[²2]?", RegexOption.IGNORE_CASE)
        val match = reg.find(text)
        if (match != null) return "${match.groupValues[1]} g/m²"
        val generalGsm = Regex("\\b(\\d{3,4})\\s*g/m[²2]?\\b", RegexOption.IGNORE_CASE).find(text)
        return generalGsm?.value ?: ""
    }

    fun extractPatternSuggestion(title: String, description: String = ""): Int {
        val text = "$title $description".lowercase(Locale.ROOT)
        return when {
            text.contains("shaggy") || text.contains("rabbit") || text.contains("plusz") -> 4 // Shaggy / Plusz
            text.contains("pers") || text.contains("vintage") || text.contains("orient") || text.contains("tradycyjn") -> 1 // Perski / Vintage
            text.contains("geometr") || text.contains("romb") || text.contains("marokańsk") -> 0 // Geometryczny
            text.contains("juta") || text.contains("sznurkow") || text.contains("płaskotkan") || text.contains("boho") -> 3 // Juta / Płaskotkany
            text.contains("klasycz") || text.contains("ornament") -> 2 // Klasyczny
            else -> 0
        }
    }

    fun extractRefCode(input: String): String {
        // Find 8-digit Leroy Merlin product code (Ref)
        val urlMatch = Regex("-(\\d{7,9})\\.html").find(input)
        if (urlMatch != null) {
            return urlMatch.groupValues[1]
        }
        val refMatch = Regex("(?:ref|nr artykułu|kod lm|indeks)[:\\s]*(\\d{7,9})\\b", RegexOption.IGNORE_CASE).find(input)
        if (refMatch != null) {
            return refMatch.groupValues[1]
        }
        // If string itself is just 7-9 digits
        if (input.trim().matches(Regex("^\\d{7,9}$"))) {
            return input.trim()
        }
        return ""
    }

    fun parsePastedTextOrUrl(input: String, fallbackBarcode: String = ""): LeroyMerlinProduct? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        // 1. Sprawdź, czy to URL do produktu Leroy Merlin
        if (trimmed.contains("leroymerlin.pl/produkty/")) {
            val urlRegex = Regex("/produkty/(?:[^/]+/)*([^/]+)-(\\d{7,9})\\.html")
            val match = urlRegex.find(trimmed)
            if (match != null) {
                val slug = match.groupValues[1]
                val refCode = match.groupValues[2]
                val cleanWords = slug.split("-").filter { it.isNotBlank() }
                val title = cleanWords.joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                }
                val size = extractSize(title)
                val collection = extractCollection(title)
                val composition = extractComposition(title)
                val color = extractColor(title)
                val pile = extractPileHeight(trimmed)
                val weight = extractWeight(trimmed)
                val pattern = extractPatternSuggestion(title)

                return LeroyMerlinProduct(
                    title = title,
                    pricePln = extractPrice(trimmed),
                    size = size,
                    collection = collection,
                    composition = composition,
                    barcode = fallbackBarcode,
                    productUrl = trimmed,
                    refCode = refCode,
                    color = color,
                    pileHeightMm = pile,
                    weightGsm = weight,
                    patternSuggestion = pattern
                )
            }
        }

        // 2. Sprawdź, czy to skopiowany tekst produktu (np. z aplikacji lub strony sklepu)
        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }
        val candidateTitle = lines.firstOrNull { isValidProductTitle(it) } ?: ""
        if (candidateTitle.isNotBlank()) {
            val cleanTitle = cleanProductTitle(candidateTitle)
            val fullText = trimmed
            val size = extractSize(fullText).ifBlank { extractSize(cleanTitle) }
            val price = extractPrice(fullText)
            val collection = extractCollection(fullText).ifBlank { extractCollection(cleanTitle) }
            val composition = extractComposition(fullText).ifBlank { extractComposition(cleanTitle) }
            val ref = extractRefCode(fullText)
            val color = extractColor(fullText).ifBlank { extractColor(cleanTitle) }
            val pile = extractPileHeight(fullText)
            val weight = extractWeight(fullText)
            val pattern = extractPatternSuggestion(cleanTitle, fullText)

            return LeroyMerlinProduct(
                title = cleanTitle,
                pricePln = price,
                size = size,
                collection = collection,
                composition = composition,
                barcode = fallbackBarcode,
                productUrl = if (trimmed.startsWith("http")) trimmed else "",
                refCode = ref,
                color = color,
                pileHeightMm = pile,
                weightGsm = weight,
                patternSuggestion = pattern
            )
        }

        return null
    }

    fun buildSearchUrl(query: String): String {
        return "https://www.leroymerlin.pl/szukaj?q=${Uri.encode(query.trim())}"
    }
}
