package com.example.data.model

import com.google.mlkit.vision.barcode.common.Barcode

enum class ScannedCodeType {
    EAN_13,
    EAN_8,
    UPC_A,
    UPC_E,
    CODE_128,
    OTHER_BARCODE,
    LEROY_MERLIN_NUMBER, // 8-cyfrowy identyfikator LM wpisany ręcznie lub dedykowany
    RAW_TEXT
}

data class ScannedCodeResult(
    val rawValue: String,
    val format: Int? = null,
    val detectedType: ScannedCodeType = ScannedCodeType.RAW_TEXT
) {
    val displayType: String
        get() = when (detectedType) {
            ScannedCodeType.EAN_13 -> "EAN-13"
            ScannedCodeType.EAN_8 -> "EAN-8"
            ScannedCodeType.UPC_A -> "UPC-A"
            ScannedCodeType.UPC_E -> "UPC-E"
            ScannedCodeType.CODE_128 -> "CODE-128"
            ScannedCodeType.LEROY_MERLIN_NUMBER -> "NUMER LEROY MERLIN"
            ScannedCodeType.OTHER_BARCODE -> "KOD KRESKOWY"
            ScannedCodeType.RAW_TEXT -> "EAN"
        }
}

object BarcodeClassifier {
    /**
     * Klasyfikuje odczyt ze skanera z uwzględnieniem formatu z biblioteki ML Kit.
     * ZAPEWNIA, że pełna wartość (rawValue) pozostaje niezmieniona i nieobcięta!
     */
    fun classifyScannedBarcode(rawValue: String, format: Int): ScannedCodeResult {
        val cleanValue = rawValue.trim()
        val type = when (format) {
            Barcode.FORMAT_EAN_13 -> ScannedCodeType.EAN_13
            Barcode.FORMAT_EAN_8 -> ScannedCodeType.EAN_8
            Barcode.FORMAT_UPC_A -> ScannedCodeType.UPC_A
            Barcode.FORMAT_UPC_E -> ScannedCodeType.UPC_E
            Barcode.FORMAT_CODE_128 -> ScannedCodeType.CODE_128
            else -> {
                if (cleanValue.length == 13 && cleanValue.all { it.isDigit() }) {
                    ScannedCodeType.EAN_13
                } else if (cleanValue.length == 8 && cleanValue.all { it.isDigit() }) {
                    ScannedCodeType.EAN_8
                } else {
                    ScannedCodeType.OTHER_BARCODE
                }
            }
        }
        return ScannedCodeResult(
            rawValue = cleanValue,
            format = format,
            detectedType = type
        )
    }

    /**
     * Klasyfikuje ręcznie wpisany ciąg znaków:
     * - dokładnie 8 cyfr -> NUMER LEROY MERLIN
     * - 13 cyfr -> EAN-13
     * - 8 cyfr nie jako skan EAN-8 -> może być traktowane jako LM
     */
    fun classifyManualInput(input: String): ScannedCodeResult {
        val clean = input.trim()
        val type = when {
            clean.length == 8 && clean.all { it.isDigit() } -> ScannedCodeType.LEROY_MERLIN_NUMBER
            clean.length == 13 && clean.all { it.isDigit() } -> ScannedCodeType.EAN_13
            clean.length == 12 && clean.all { it.isDigit() } -> ScannedCodeType.UPC_A
            else -> ScannedCodeType.RAW_TEXT
        }
        return ScannedCodeResult(
            rawValue = clean,
            format = null,
            detectedType = type
        )
    }
}
