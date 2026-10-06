package com.example.data.sync

import com.example.data.model.SyncPayload
import com.example.data.repository.ExpoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class WifiSyncServer(
    private val repository: ExpoRepository,
    private val deviceName: String,
    private val deviceId: String
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _connectionsCount = MutableStateFlow(0)
    val connectionsCount: StateFlow<Int> = _connectionsCount.asStateFlow()

    private val _lastSyncInfo = MutableStateFlow<String?>(null)
    val lastSyncInfo: StateFlow<String?> = _lastSyncInfo.asStateFlow()

    fun start(port: Int = NetworkUtils.DEFAULT_PORT) {
        if (_isRunning.value) return

        serverJob = coroutineScope.launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                _isRunning.value = true
                repository.logCustomAction(
                    type = "SERWER_START",
                    message = "Uruchomiono serwer synchronizacji ekspozycji na porcie $port",
                    deviceName = deviceName
                )

                while (isActive && serverSocket?.isClosed == false) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch(Dispatchers.IO) {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isActive || serverSocket?.isClosed == true) break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun stop() {
        try {
            _isRunning.value = false
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            serverJob = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            _connectionsCount.value += 1
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val output: OutputStream = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0].uppercase()
            val path = parts[1].substringBefore("?")

            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) break
                if (line!!.lowercase().startsWith("content-length:")) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            when {
                method == "OPTIONS" -> {
                    sendHttpResponse(output, 200, "OK", "text/plain", "")
                }
                method == "GET" && path == "/api/status" -> {
                    val statusObj = JSONObject().apply {
                        put("status", "ONLINE")
                        put("serverName", deviceName)
                        put("deviceId", deviceId)
                        put("serverTime", System.currentTimeMillis())
                    }
                    sendHttpResponse(output, 200, "OK", "application/json", statusObj.toString())
                }
                method == "GET" && path == "/api/sync" -> {
                    val payload = repository.createSyncPayload(deviceId, deviceName)
                    sendHttpResponse(output, 200, "OK", "application/json", payload.toJsonString())
                    _lastSyncInfo.value = "Pobrano stan przez klienta (${System.currentTimeMillis()})"
                }
                method == "POST" && path == "/api/sync" -> {
                    val bodyChars = CharArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val read = reader.read(bodyChars, readTotal, contentLength - readTotal)
                        if (read == -1) break
                        readTotal += read
                    }
                    val bodyString = String(bodyChars, 0, readTotal)

                    val incomingPayload = SyncPayload.fromJsonString(bodyString)
                    val mergeResult = repository.mergeSyncPayload(incomingPayload, incomingPayload.deviceName)

                    // Zwróć najświeższy scalony stan do klienta
                    val responsePayload = repository.createSyncPayload(deviceId, deviceName)
                    sendHttpResponse(output, 200, "OK", "application/json", responsePayload.toJsonString())

                    _lastSyncInfo.value = "Zsynchronizowano z: ${incomingPayload.deviceName} (+${mergeResult.carpetsUpdated} dywanów)"
                }
                else -> {
                    sendHttpResponse(output, 404, "Not Found", "text/plain", "Endpoint not found")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                socket.close()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun sendHttpResponse(
        output: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: String
    ) {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: $contentType; charset=utf-8\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type\r\n" +
                "Connection: close\r\n\r\n"

        output.write(header.toByteArray(Charsets.UTF_8))
        if (bodyBytes.isNotEmpty()) {
            output.write(bodyBytes)
        }
        output.flush()
    }
}
