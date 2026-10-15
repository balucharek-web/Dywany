package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader()
    private var lastScannedCode: String? = null
    private var lastScannedTimestamp: Long = 0
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        } catch (e: Exception) {
            // ignore if audio cannot be initialized
        }
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            try {
                val buffer = mediaImage.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                val width = imageProxy.width
                val height = imageProxy.height

                val source = PlanarYUVLuminanceSource(
                    bytes,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                    false
                )
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                try {
                    val result = reader.decodeWithState(binaryBitmap)
                    val text = result.text?.trim()
                    if (!text.isNullOrEmpty()) {
                        val now = System.currentTimeMillis()
                        // Prevent instant duplicate triggers within 1.5 seconds for same code
                        if (text != lastScannedCode || (now - lastScannedTimestamp) > 1500) {
                            lastScannedCode = text
                            lastScannedTimestamp = now
                            playBeep()
                            onBarcodeDetected(text)
                        }
                    }
                } catch (e: Exception) {
                    // Normal when no barcode in current frame
                } finally {
                    reader.reset()
                }
            } catch (e: Exception) {
                // error reading frame
            }
        }
        imageProxy.close()
    }

    private fun playBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {}
    }
}
