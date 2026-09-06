package com.example.blackbox.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager

data class WifiSnapshot(
    val connectedSsid: String?,
    val bssid: String?,
    val signalLevelDbm: Int,
    val nearbyAccessPointCount: Int,
    val timestampMs: Long
)

class WifiSnapshotCollector(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @SuppressLint("MissingPermission")
    fun captureSnapshot(): WifiSnapshot {
        val info = wifiManager.connectionInfo
        val ssid = info?.ssid?.replace("\"", "")
        val bssid = info?.bssid
        val rssi = info?.rssi ?: -100

        val scanResultsCount = try {
            wifiManager.scanResults.size
        } catch (e: SecurityException) {
            0
        }

        return WifiSnapshot(
            connectedSsid = if (ssid != "<unknown ssid>") ssid else null,
            bssid = bssid,
            signalLevelDbm = rssi,
            nearbyAccessPointCount = scanResultsCount,
            timestampMs = System.currentTimeMillis()
        )
    }
}
