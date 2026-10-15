package com.example.p2p

import android.os.Build
import android.util.Log
import com.example.data.model.Product
import com.example.data.repository.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

class P2pServer(private val repository: ProductRepository) {

    private val tag = "P2pServer038"
    val port = 8038

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _lastSyncMessage = MutableStateFlow("Serwer gotowy do synchronizacji")
    val lastSyncMessage: StateFlow<String> = _lastSyncMessage.asStateFlow()

    fun start() {
        if (_isRunning.value) return
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                _isRunning.value = true
                Log.d(tag, "P2P Server started on port $port")
                while (_isRunning.value) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        handleClient(client)
                    } catch (e: Exception) {
                        if (!_isRunning.value) break
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Server error", e)
                _isRunning.value = false
            }
        }
    }

    fun stop() {
        _isRunning.value = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
        serverJob?.cancel()
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                socket.soTimeout = 10000
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), true)

                // Read request line
                val requestLine = reader.readLine() ?: return@launch
                val parts = requestLine.split(" ")
                val method = parts.getOrNull(0) ?: "GET"
                val path = parts.getOrNull(1) ?: "/"

                // Read headers
                var contentLength = 0
                var line = reader.readLine()
                while (!line.isNullOrEmpty()) {
                    if (line.lowercase().startsWith("content-length:")) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine()
                }

                when {
                    method == "GET" && path.startsWith("/api/status") -> {
                        val json = JSONObject().apply {
                            put("status", "ok")
                            put("sklep", "038")
                            put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                            put("timestamp", System.currentTimeMillis())
                        }
                        sendJsonResponse(writer, 200, json.toString())
                    }

                    method == "GET" && path.startsWith("/api/products") -> {
                        val products = repository.getAllProductsForSync()
                        val array = productsToJson(products)
                        sendJsonResponse(writer, 200, array.toString())
                    }

                    method == "POST" && path.startsWith("/api/sync") -> {
                        // Read body
                        val bodyChars = CharArray(contentLength)
                        var readTotal = 0
                        while (readTotal < contentLength) {
                            val r = reader.read(bodyChars, readTotal, contentLength - readTotal)
                            if (r == -1) break
                            readTotal += r
                        }
                        val body = String(bodyChars, 0, readTotal).trim()
                        val incomingProducts: List<Product>
                        val incomingAdmins = mutableListOf<com.example.data.model.AdminUser>()

                        if (body.startsWith("{")) {
                            val jsonRoot = JSONObject(body)
                            val pArray = jsonRoot.optJSONArray("products") ?: JSONArray()
                            incomingProducts = jsonToProducts(pArray)
                            val aArray = jsonRoot.optJSONArray("admins")
                            if (aArray != null) {
                                for (i in 0 until aArray.length()) {
                                    val aObj = aArray.getJSONObject(i)
                                    val email = aObj.optString("email")
                                    if (email.isNotBlank()) {
                                        incomingAdmins.add(
                                            com.example.data.model.AdminUser(
                                                email = email,
                                                isSuperAdmin = aObj.optBoolean("isSuperAdmin", false),
                                                addedBy = aObj.optString("addedBy", ""),
                                                addedAt = aObj.optLong("addedAt", System.currentTimeMillis())
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            incomingProducts = jsonToProducts(JSONArray(body))
                        }

                        // Merge incoming products & admins
                        val updatedCount = repository.mergeSyncProducts(incomingProducts)
                        if (incomingAdmins.isNotEmpty()) {
                            repository.mergeSyncAdmins(incomingAdmins)
                        }

                        // Respond with our complete state
                        val myAll = repository.getAllProductsForSync()
                        val myAdmins = repository.getAllAdminsForSync()
                        val adminJsonArray = JSONArray()
                        for (a in myAdmins) {
                            adminJsonArray.put(JSONObject().apply {
                                put("email", a.email)
                                put("isSuperAdmin", a.isSuperAdmin)
                                put("addedBy", a.addedBy)
                                put("addedAt", a.addedAt)
                            })
                        }

                        val responseJson = JSONObject().apply {
                            put("status", "synced")
                            put("sklep", "038")
                            put("updatedLocally", updatedCount)
                            put("products", productsToJson(myAll))
                            put("admins", adminJsonArray)
                        }
                        _lastSyncMessage.value = "Zsynchronizowano z ${socket.inetAddress.hostAddress}: zaktualizowano $updatedCount pozycji"
                        sendJsonResponse(writer, 200, responseJson.toString())
                    }

                    else -> {
                        sendJsonResponse(writer, 404, "{\"error\":\"Not Found\"}")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Client processing failed", e)
            } finally {
                try { socket.close() } catch (e: Exception) {}
            }
        }
    }

    private fun sendJsonResponse(writer: PrintWriter, code: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        writer.print("HTTP/1.1 $code OK\r\n")
        writer.print("Content-Type: application/json; charset=utf-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print(body)
        writer.flush()
    }

    companion object {
        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback || !iface.isUp) continue
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val ip = addr.hostAddress ?: ""
                            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                                return ip
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
            return "127.0.0.1"
        }

        fun productsToJson(products: List<Product>): JSONArray {
            val array = JSONArray()
            for (p in products) {
                val obj = JSONObject().apply {
                    put("uuid", p.uuid)
                    put("kodLm", p.kodLm)
                    put("ean", p.ean)
                    put("nazwa", p.nazwa)
                    put("kolekcja", p.kolekcja)
                    put("rozmiar", p.rozmiar)
                    put("cena", p.cena)
                    put("cenaM2", p.cenaM2)
                    put("ilosc", p.ilosc)
                    put("lokalizacja", p.lokalizacja)
                    put("sektor", p.sektor)
                    put("sklad", p.sklad)
                    put("kolor", p.kolor)
                    put("uwagi", p.uwagi)
                    put("sklepId", p.sklepId)
                    put("updatedAt", p.updatedAt)
                    put("isDeleted", p.isDeleted)
                }
                array.put(obj)
            }
            return array
        }

        fun jsonToProducts(array: JSONArray): List<Product> {
            val list = mutableListOf<Product>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Product(
                        uuid = obj.optString("uuid"),
                        kodLm = obj.optString("kodLm"),
                        ean = obj.optString("ean"),
                        nazwa = obj.optString("nazwa"),
                        kolekcja = obj.optString("kolekcja"),
                        rozmiar = obj.optString("rozmiar"),
                        cena = obj.optDouble("cena", 0.0),
                        cenaM2 = obj.optDouble("cenaM2", 0.0),
                        ilosc = obj.optInt("ilosc", 0),
                        lokalizacja = obj.optString("lokalizacja", "Brak lokacji"),
                        sektor = obj.optString("sektor", "Nierozmieszczone"),
                        sklad = obj.optString("sklad"),
                        kolor = obj.optString("kolor"),
                        uwagi = obj.optString("uwagi"),
                        sklepId = obj.optString("sklepId", "038"),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isDeleted = obj.optBoolean("isDeleted", false)
                    )
                )
            }
            return list
        }
    }
}
