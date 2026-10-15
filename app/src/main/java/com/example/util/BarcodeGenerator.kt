package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.EnumMap

object BarcodeGenerator {

    fun generateQrCodeBitmap(text: String, width: Int = 512, height: Int = 512): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.MARGIN, 1)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
            }
            val matrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            )
            val w = matrix.width
            val h = matrix.height
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                val offset = y * w
                for (x in 0 until w) {
                    pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, w, 0, 0, w, h)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun generateEan13BarcodeBitmap(ean: String, width: Int = 600, height: Int = 200): Bitmap? {
        return try {
            val format = if (ean.length == 13 && ean.all { it.isDigit() }) {
                BarcodeFormat.EAN_13
            } else if (ean.length == 8 && ean.all { it.isDigit() }) {
                BarcodeFormat.EAN_8
            } else {
                BarcodeFormat.CODE_128
            }

            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.MARGIN, 2)
            }
            val matrix: BitMatrix = MultiFormatWriter().encode(
                ean,
                format,
                width,
                height,
                hints
            )
            val w = matrix.width
            val h = matrix.height
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                val offset = y * w
                for (x in 0 until w) {
                    pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, w, 0, 0, w, h)
            }
        } catch (e: Exception) {
            null
        }
    }
}
