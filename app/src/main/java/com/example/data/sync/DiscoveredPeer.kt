package com.example.data.sync

data class DiscoveredPeer(
    val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
    val port: Int = NetworkUtils.DEFAULT_PORT,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val lastSyncStatus: String = "Połączono"
) {
    val httpUrl: String
        get() = "http://$ipAddress:$port"
}
