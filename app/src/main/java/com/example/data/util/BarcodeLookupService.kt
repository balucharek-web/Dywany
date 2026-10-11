package com.example.data.util

import android.util.Log
import com.example.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object BarcodeLookupService {
    private const val TAG = "BarcodeLookupService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * Looks up product details from the legal, open barcode registry (Open Products Facts).
     * Works without bot blockers, returning product title, images, and brand.
     */
    suspend fun lookupEan(ean: String): Product? = withContext(Dispatchers.IO) {
        val cleanEan = ean.trim()
        if (cleanEan.length < 8) return@withContext null

        try {
            val url = "https://world.openproductsfacts.org/api/v0/product/$cleanEan.json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LeroyMerlinRugDisplay-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)

                if (json.optInt("status") == 1 && json.has("product")) {
                    val p = json.getJSONObject("product")
                    val name = p.optString("product_name_pl").ifBlank {
                        p.optString("product_name").ifBlank { "Dywan EAN $cleanEan" }
                    }
                    val img = p.optString("image_url").ifBlank {
                        p.optString("image_front_url")
                    }

                    return@withContext Product(
                        productId = "ean_$cleanEan",
                        name = name,
                        ean = cleanEan,
                        lmSystemNumber = "",
                        onlinePrice = 0.0,
                        localPrice = 0.0,
                        localPriceOverride = false,
                        imageUrl = img,
                        dimensions = "",
                        composition = ""
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Open barcode registry query failed for EAN $cleanEan: ${e.message}")
        }
        return@withContext null
    }
}
