package com.example.data.sync

import com.example.data.model.SyncPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WifiSyncClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(hostUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = if (hostUrl.endsWith("/")) "${hostUrl}api/status" else "$hostUrl/api/status"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Błąd serwera: HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val obj = JSONObject(body)
                val serverName = obj.optString("serverName", "Serwer ekspozycji")
                Result.success(serverName)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncWithHost(hostUrl: String, payload: SyncPayload): Result<SyncPayload> = withContext(Dispatchers.IO) {
        try {
            val url = if (hostUrl.endsWith("/")) "${hostUrl}api/sync" else "$hostUrl/api/sync"
            val requestBody = payload.toJsonString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Błąd synchronizacji: HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val responsePayload = SyncPayload.fromJsonString(body)
                Result.success(responsePayload)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
