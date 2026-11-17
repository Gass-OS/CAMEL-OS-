package com.example.camel.services

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.io.File
import kotlin.math.roundToInt

data class CamelSystemTelemetry(
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val batteryTempCelsius: Float = 0f,
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
    val ramPercent: Int = 0,
    val storageUsedGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val storagePercent: Int = 0,
    val cpuCores: Int = 0,
    val cpuFreqMhz: Int? = null,
    val networkLabel: String = "Offline",
    val isNetworkOnline: Boolean = false,
    val deviceModel: String = "",
    val androidVersion: String = "",
    val primaryAbi: String = "",
    val uptimeLabel: String = "0m"
)

class CamelTelemetryService(private val context: Context) {

    fun snapshot(): CamelSystemTelemetry {
        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
        } else {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 0
        }

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = rawTemp / 10f

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val totalRamGb = bytesToGb(memInfo.totalMem)
        val availRamGb = bytesToGb(memInfo.availMem)
        val usedRamGb = (totalRamGb - availRamGb).coerceAtLeast(0f)
        val ramPct = if (memInfo.totalMem > 0L) {
            (((memInfo.totalMem - memInfo.availMem).toDouble() / memInfo.totalMem.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)
        } else 0

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalBytes = statFs.totalBytes
        val availBytes = statFs.availableBytes
        val usedBytes = (totalBytes - availBytes).coerceAtLeast(0L)
        val storageTotalGb = bytesToGb(totalBytes)
        val storageUsedGb = bytesToGb(usedBytes)
        val storagePct = if (totalBytes > 0L) {
            ((usedBytes.toDouble() / totalBytes.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        } else 0

        val cpuFreqKhz = readFirstReadableInt(
            listOf(
                "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq",
                "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"
            )
        )
        val cpuFreqMhz = cpuFreqKhz?.let { it / 1000 }

        val (netLabel, isOnline) = readNetworkState()

        val uptimeMinutes = SystemClock.elapsedRealtime() / 60_000L
        val uptimeLabel = if (uptimeMinutes >= 60L) {
            "${uptimeMinutes / 60}h ${uptimeMinutes % 60}m"
        } else {
            "${uptimeMinutes}m"
        }

        val model = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .replaceFirstChar { it.uppercase() }

        return CamelSystemTelemetry(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            batteryTempCelsius = tempC,
            ramUsedGb = usedRamGb,
            ramTotalGb = totalRamGb,
            ramPercent = ramPct,
            storageUsedGb = storageUsedGb,
            storageTotalGb = storageTotalGb,
            storagePercent = storagePct,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            cpuFreqMhz = cpuFreqMhz,
            networkLabel = netLabel,
            isNetworkOnline = isOnline,
            deviceModel = model,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            uptimeLabel = uptimeLabel
        )
    }

    private fun readNetworkState(): Pair<String, Boolean> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "Unavailable" to false
        val network = cm.activeNetwork ?: return "Offline" to false
        val caps = cm.getNetworkCapabilities(network) ?: return "Offline" to false
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi" to hasInternet
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular" to hasInternet
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet" to hasInternet
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN" to hasInternet
            else -> if (hasInternet) "Connected" to true else "Offline" to false
        }
    }

    private fun readFirstReadableInt(paths: List<String>): Int? {
        for (path in paths) {
            val value = runCatching {
                File(path).takeIf { it.exists() && it.canRead() }?.readText()?.trim()?.toIntOrNull()
            }.getOrNull()
            if (value != null && value > 0) return value
        }
        return null
    }

    private fun bytesToGb(bytes: Long): Float =
        (bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)).toFloat()
}
