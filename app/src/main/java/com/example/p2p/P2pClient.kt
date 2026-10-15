package com.example.p2p

import com.example.data.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class P2pClient(private val repository: ProductRepository) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    sealed class SyncResult {
        data class Success(val updatedCount: Int, val message: String) : SyncResult()
        data class Error(val error: String) : SyncResult()
    }

    suspend fun checkPeerStatus(host: String, port: Int = 8038): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").substringBefore(":")
            val url = "http://$cleanHost:$port/api/status"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val sklep = json.optString("sklep", "")
                val device = json.optString("device", "Inny telefon")
                if (sklep == "038") {
                    Result.success("Sklep 038 ($device)")
                } else {
                    Result.failure(Exception("Urządzenie nie należy do Sklepu 038 (zgłasza: $sklep)"))
                }
            } else {
                Result.failure(Exception("Błąd HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncWithPeer(host: String, port: Int = 8038): SyncResult = withContext(Dispatchers.IO) {
        try {
            val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").substringBefore(":")
            val myProducts = repository.getAllProductsForSync()
            val myAdmins = repository.getAllAdminsForSync()

            val adminJsonArray = org.json.JSONArray()
            for (a in myAdmins) {
                adminJsonArray.put(JSONObject().apply {
                    put("email", a.email)
                    put("isSuperAdmin", a.isSuperAdmin)
                    put("addedBy", a.addedBy)
                    put("addedAt", a.addedAt)
                })
            }

            val syncPayload = JSONObject().apply {
                put("products", P2pServer.productsToJson(myProducts))
                put("admins", adminJsonArray)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = syncPayload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("http://$cleanHost:$port/api/sync")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext SyncResult.Error("Błąd serwera peer: HTTP ${response.code}")
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val remoteProductsJson = json.optJSONArray("products") ?: return@withContext SyncResult.Error("Błędna odpowiedź peer")
            val remoteProducts = P2pServer.jsonToProducts(remoteProductsJson)

            // Merge remote products locally
            val localUpdated = repository.mergeSyncProducts(remoteProducts)
            val peerUpdated = json.optInt("updatedLocally", 0)

            // Merge remote admins locally
            val remoteAdminsJson = json.optJSONArray("admins")
            if (remoteAdminsJson != null) {
                val remoteAdmins = mutableListOf<com.example.data.model.AdminUser>()
                for (i in 0 until remoteAdminsJson.length()) {
                    val aObj = remoteAdminsJson.getJSONObject(i)
                    val email = aObj.optString("email")
                    if (email.isNotBlank()) {
                        remoteAdmins.add(
                            com.example.data.model.AdminUser(
                                email = email,
                                isSuperAdmin = aObj.optBoolean("isSuperAdmin", false),
                                addedBy = aObj.optString("addedBy", ""),
                                addedAt = aObj.optLong("addedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
                repository.mergeSyncAdmins(remoteAdmins)
            }

            SyncResult.Success(
                updatedCount = localUpdated,
                message = "Zsynchronizowano pomyślnie! Pobrano $localUpdated nowszych pozycji, przesłano do partnera $peerUpdated."
            )
        } catch (e: Exception) {
            SyncResult.Error("Nie udało się połączyć z urządzeniem: ${e.localizedMessage ?: "Błąd sieci"}")
        }
    }
}
