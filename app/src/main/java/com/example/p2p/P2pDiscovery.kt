package com.example.p2p

import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class DiscoveredPeer(
    val name: String,
    val ip: String,
    val port: Int = 8038,
    val lastSeen: Long = System.currentTimeMillis()
)

class P2pDiscovery {

    private val tag = "P2pDiscovery038"
    private val broadcastPort = 8039
    private val scope = CoroutineScope(Dispatchers.IO)
    private var listenJob: Job? = null
    private var broadcastJob: Job? = null
    private var socket: DatagramSocket? = null

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    fun startDiscovery() {
        stopDiscovery()
        val myIp = P2pServer.getLocalIpAddress()
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        // 1. Listen for peer broadcasts
        listenJob = scope.launch {
            try {
                socket = DatagramSocket(broadcastPort).apply {
                    broadcast = true
                    soTimeout = 3000
                }
                val buffer = ByteArray(1024)
                while (isActive) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)
                        val message = String(packet.data, 0, packet.length).trim()
                        val senderIp = packet.address.hostAddress ?: ""

                        if (senderIp != myIp && message.startsWith("DYWANY_038:")) {
                            val parts = message.split(":")
                            val peerName = parts.getOrNull(1) ?: "Pracownik 038"
                            val peerPort = parts.getOrNull(2)?.toIntOrNull() ?: 8038

                            val updatedList = _discoveredPeers.value.toMutableList()
                            val existingIndex = updatedList.indexOfFirst { it.ip == senderIp }
                            if (existingIndex >= 0) {
                                updatedList[existingIndex] = updatedList[existingIndex].copy(
                                    name = peerName,
                                    lastSeen = System.currentTimeMillis()
                                )
                            } else {
                                updatedList.add(DiscoveredPeer(name = peerName, ip = senderIp, port = peerPort))
                            }
                            _discoveredPeers.value = updatedList
                        }
                    } catch (e: Exception) {
                        // socket timeout or error, continue listening
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Listen error", e)
            }
        }

        // 2. Periodically broadcast presence
        broadcastJob = scope.launch {
            while (isActive) {
                try {
                    val msg = "DYWANY_038:$deviceName:8038"
                    val bytes = msg.toByteArray()
                    val broadcastAddress = InetAddress.getByName("255.255.255.255")
                    val packet = DatagramPacket(bytes, bytes.size, broadcastAddress, broadcastPort)
                    socket?.send(packet)
                } catch (e: Exception) {
                    // ignore
                }
                delay(3000)
            }
        }
    }

    fun stopDiscovery() {
        broadcastJob?.cancel()
        listenJob?.cancel()
        try {
            socket?.close()
        } catch (e: Exception) {}
        socket = null
    }
}
