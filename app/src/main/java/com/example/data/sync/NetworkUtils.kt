package com.example.data.sync

import android.content.Context
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {
    const val DEFAULT_PORT = 8899

    fun getLocalIpAddress(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiIp = wifiManager?.connectionInfo?.ipAddress
            if (wifiIp != null && wifiIp != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    wifiIp and 0xff,
                    wifiIp shr 8 and 0xff,
                    wifiIp shr 16 and 0xff,
                    wifiIp shr 24 and 0xff
                )
            }
        } catch (e: Exception) {
            // Ignoruj i przejdź do NetworkInterface
        }

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.isLoopback || !intf.isUp) continue
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            // Ignoruj
        }

        return "127.0.0.1"
    }

    fun buildServerUrl(ip: String, port: Int = DEFAULT_PORT): String {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://").substringBefore("/")
        return "http://$cleanIp:$port"
    }
}
