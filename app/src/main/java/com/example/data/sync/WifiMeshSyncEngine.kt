package com.example.data.sync

import android.content.Context
import android.net.wifi.WifiManager
import com.example.data.repository.ExpoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

class WifiMeshSyncEngine(
    private val context: Context,
    private val repository: ExpoRepository,
    val deviceName: String,
    val deviceId: String
) {
    companion object {
        const val BROADCAST_PORT = 8898
        const val HTTP_PORT = NetworkUtils.DEFAULT_PORT
        private const val BEACON_PREFIX = "DYWANEXPO_BEACON_V1"
    }

    val server = WifiSyncServer(repository, deviceName, deviceId)
    val client = WifiSyncClient()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var broadcastJob: Job? = null
    private var listenerJob: Job? = null
    private var autoSyncJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _peers = MutableStateFlow<Map<String, DiscoveredPeer>>(emptyMap())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = MutableStateFlow<List<DiscoveredPeer>>(emptyMap<String, DiscoveredPeer>().values.toList())
    private val _peersList = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val peersList: StateFlow<List<DiscoveredPeer>> = _peersList.asStateFlow()

    private val _isMeshActive = MutableStateFlow(false)
    val isMeshActive: StateFlow<Boolean> = _isMeshActive.asStateFlow()

    private val _lastMeshEvent = MutableStateFlow("Inicjalizacja sieci P2P...")
    val lastMeshEvent: StateFlow<String> = _lastMeshEvent.asStateFlow()

    private val _isScanningSubnet = MutableStateFlow(false)
    val isScanningSubnet: StateFlow<Boolean> = _isScanningSubnet.asStateFlow()

    fun start() {
        if (_isMeshActive.value) return
        _isMeshActive.value = true

        // 1. Uruchomienie lokalnego serwera HTTP (każde urządzenie jest Hostem)
        server.start(HTTP_PORT)

        // 2. Multicast lock dla Wi-Fi
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("DywanExpoMulticast")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Rozgłaszanie obecności w sieci (UDP Broadcast)
        startBroadcasting()

        // 4. Odsłuchiwanie innych urządzeń w sieci
        startListening()

        // 5. Okresowa synchronizacja i czyszczenie nieaktywnych urządzeń
        startPeriodicSyncAndPrune()

        _lastMeshEvent.value = "Sieć P2P aktywna. Urządzenie nasłuchuje na porcie $HTTP_PORT."
    }

    fun stop() {
        _isMeshActive.value = false
        broadcastJob?.cancel()
        listenerJob?.cancel()
        autoSyncJob?.cancel()
        server.stop()
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (ignored: Exception) {
        }
    }

    private fun startBroadcasting() {
        broadcastJob = scope.launch {
            while (isActive && _isMeshActive.value) {
                try {
                    val myIp = NetworkUtils.getLocalIpAddress(context)
                    if (myIp != "127.0.0.1") {
                        val payload = repository.createSyncPayload(deviceId, deviceName)
                        val message = "$BEACON_PREFIX|$deviceId|$deviceName|$myIp|$HTTP_PORT|${payload.timestamp}"
                        val bytes = message.toByteArray(Charsets.UTF_8)

                        DatagramSocket().use { socket ->
                            socket.broadcast = true
                            // 1. Global broadcast
                            val broadcastAddr = InetAddress.getByName("255.255.255.255")
                            val packet = DatagramPacket(bytes, bytes.size, broadcastAddr, BROADCAST_PORT)
                            socket.send(packet)

                            // 2. Subnet broadcast (np. 192.168.1.255)
                            val subnetBroadcast = getSubnetBroadcastAddress(myIp)
                            if (subnetBroadcast != null && subnetBroadcast != "255.255.255.255") {
                                val subnetPacket = DatagramPacket(bytes, bytes.size, InetAddress.getByName(subnetBroadcast), BROADCAST_PORT)
                                socket.send(subnetPacket)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignoruj przejściowe błędy sieciowe
                }
                delay(4000) // Rozgłaszaj co 4 sekundy
            }
        }
    }

    private fun startListening() {
        listenerJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(BROADCAST_PORT))
                }
                val buffer = ByteArray(2048)

                while (isActive && _isMeshActive.value) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val senderIp = packet.address.hostAddress ?: ""

                        if (text.startsWith(BEACON_PREFIX)) {
                            val parts = text.split("|")
                            if (parts.size >= 6) {
                                val peerDeviceId = parts[1]
                                val peerDeviceName = parts[2]
                                val advertisedIp = parts[3]
                                val peerPort = parts[4].toIntOrNull() ?: HTTP_PORT
                                val peerTimestamp = parts[5].toLongOrNull() ?: 0L

                                val actualIp = if (senderIp.isNotBlank()) senderIp else advertisedIp

                                if (peerDeviceId != deviceId) {
                                    handleDiscoveredPeer(
                                        peerDeviceId,
                                        peerDeviceName,
                                        actualIp,
                                        peerPort,
                                        peerTimestamp
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                socket?.close()
            }
        }
    }

    private fun handleDiscoveredPeer(
        peerDeviceId: String,
        peerDeviceName: String,
        ip: String,
        port: Int,
        peerTimestamp: Long
    ) {
        val currentMap = _peers.value.toMutableMap()
        val existing = currentMap[peerDeviceId]
        val isNew = existing == null

        val updatedPeer = DiscoveredPeer(
            deviceId = peerDeviceId,
            deviceName = peerDeviceName,
            ipAddress = ip,
            port = port,
            lastSeenTimestamp = System.currentTimeMillis(),
            lastSyncStatus = existing?.lastSyncStatus ?: "Wykryto urządzenie"
        )
        currentMap[peerDeviceId] = updatedPeer
        _peers.value = currentMap
        _peersList.value = currentMap.values.toList()

        if (isNew) {
            _lastMeshEvent.value = "Wykryto nowe urządzenie w salonie: $peerDeviceName ($ip)"
            // Natychmiastowa pierwsza synchronizacja
            scope.launch {
                syncWithPeer(updatedPeer)
            }
        }
    }

    private fun startPeriodicSyncAndPrune() {
        autoSyncJob = scope.launch {
            while (isActive && _isMeshActive.value) {
                delay(12000) // Co 12 sekund synchronizuj z każdym znanym urządzeniem
                val now = System.currentTimeMillis()
                val currentPeers = _peers.value

                // 1. Prune nieaktywnych urządzeń (brak sygnału przez 35 sekund)
                val activePeers = currentPeers.filter { (_, peer) ->
                    now - peer.lastSeenTimestamp < 35000
                }
                if (activePeers.size != currentPeers.size) {
                    _peers.value = activePeers
                    _peersList.value = activePeers.values.toList()
                }

                // 2. Synchronizacja z aktywnymi urządzeniami
                for ((_, peer) in activePeers) {
                    launch {
                        syncWithPeer(peer)
                    }
                }
            }
        }
    }

    suspend fun syncWithPeer(peer: DiscoveredPeer) = withContext(Dispatchers.IO) {
        try {
            val localPayload = repository.createSyncPayload(deviceId, deviceName)
            val result = client.syncWithHost(peer.httpUrl, localPayload)

            if (result.isSuccess) {
                val remotePayload = result.getOrThrow()
                val merge = repository.mergeSyncPayload(remotePayload, peer.deviceName)

                val statusText = if (merge.carpetsUpdated > 0 || merge.standsUpdated > 0) {
                    "Zaktualizowano: +${merge.carpetsUpdated} dywanów"
                } else {
                    "Zsynchronizowano (stan zgodny)"
                }

                updatePeerStatus(peer.deviceId, statusText)
                _lastMeshEvent.value = "Pomyślna wymiana z ${peer.deviceName}: $statusText"
            } else {
                updatePeerStatus(peer.deviceId, "Błąd: ${result.exceptionOrNull()?.message?.take(25)}")
            }
        } catch (e: Exception) {
            updatePeerStatus(peer.deviceId, "Rozłączono")
        }
    }

    private fun updatePeerStatus(peerDeviceId: String, status: String) {
        val current = _peers.value.toMutableMap()
        current[peerDeviceId]?.let {
            current[peerDeviceId] = it.copy(lastSyncStatus = status)
            _peers.value = current
            _peersList.value = current.values.toList()
        }
    }

    /**
     * Błyskawiczny broadcast synchronizacji przy dokonaniu lokalnej zmiany
     * (gdy pracownik przestawi dywan na stojaku)
     */
    fun notifyLocalDataChanged() {
        scope.launch {
            val peers = _peers.value.values.toList()
            for (peer in peers) {
                launch {
                    syncWithPeer(peer)
                }
            }
        }
    }

    /**
     * Ręczne skanowanie podsieci (dla sieci Wi-Fi blokujących broadcast UDP)
     */
    fun scanLocalSubnet() {
        if (_isScanningSubnet.value) return
        _isScanningSubnet.value = true
        _lastMeshEvent.value = "Przeszukiwanie sieci Wi-Fi w poszukiwaniu innych telefonów..."

        scope.launch {
            val myIp = NetworkUtils.getLocalIpAddress(context)
            if (myIp == "127.0.0.1") {
                _isScanningSubnet.value = false
                _lastMeshEvent.value = "Brak połączenia z siecią Wi-Fi"
                return@launch
            }

            val prefix = myIp.substringBeforeLast(".")
            val myLastOctet = myIp.substringAfterLast(".").toIntOrNull() ?: -1

            // Przeszukaj w wiązkach po 20 adresów w podsieci
            for (batchStart in 1..254 step 25) {
                val batchEnd = minOf(batchStart + 24, 254)
                for (i in batchStart..batchEnd) {
                    if (i == myLastOctet) continue
                    val testIp = "$prefix.$i"
                    launch {
                        val testUrl = "http://$testIp:$HTTP_PORT"
                        val testRes = client.testConnection(testUrl)
                        if (testRes.isSuccess) {
                            val serverName = testRes.getOrThrow()
                            handleDiscoveredPeer(
                                peerDeviceId = "SUBNET-$testIp",
                                peerDeviceName = serverName,
                                ip = testIp,
                                port = HTTP_PORT,
                                peerTimestamp = System.currentTimeMillis()
                            )
                        }
                    }
                }
                delay(300)
            }

            delay(1500)
            _isScanningSubnet.value = false
            _lastMeshEvent.value = "Zakończono przeszukiwanie. Aktywnych urządzeń: ${_peers.value.size}"
        }
    }

    private fun getSubnetBroadcastAddress(ip: String): String? {
        val parts = ip.split(".")
        if (parts.size == 4) {
            return "${parts[0]}.${parts[1]}.${parts[2]}.255"
        }
        return null
    }
}
