package com.gamer.calibration.domain.model

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.WindowManager
import java.util.Locale

data class DeviceSpecs(
    val deviceName: String = "Android Device",
    val manufacturer: String = "Generic",
    val model: String = "Mobile",
    val androidVersion: String = "14",
    val apiLevel: Int = 34,
    val refreshRateHz: Float = 60f,
    val screenResolution: String = "1080 x 2400",
    val screenDensityDpi: Int = 420,
    val totalRamGb: String = "8 GB",
    val availableRamGb: String = "4 GB",
    val chipset: String = "Snapdragon / Dimensity",
    val cpuCores: Int = 8,
    val supportedAbis: String = "arm64-v8a",
    val supportedRefreshRates: String = "60Hz, 120Hz",
    val glEsVersion: String = "OpenGL ES 3.2",
    val board: String = "Universal"
)

object DeviceInfoHelper {
    fun extract(context: Context): DeviceSpecs {
        return try {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
            }
            val model = Build.MODEL
            val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) {
                model
            } else {
                "$manufacturer $model"
            }

            // Display Info
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                windowManager?.defaultDisplay
            }

            val refreshRate = display?.mode?.refreshRate ?: 60f
            val width = display?.mode?.physicalWidth ?: context.resources.displayMetrics.widthPixels
            val height = display?.mode?.physicalHeight ?: context.resources.displayMetrics.heightPixels
            val densityDpi = context.resources.displayMetrics.densityDpi

            val supportedModesList = display?.supportedModes
                ?.map { it.refreshRate.toInt() }
                ?.distinct()
                ?.sorted()
                ?.joinToString(" / ") { "${it}Hz" }
                ?: "${refreshRate.toInt()}Hz"

            // RAM Info
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val ramGb = String.format(Locale.US, "%.1f GB", memInfo.totalMem / (1024.0 * 1024.0 * 1024.0))
            val availGb = String.format(Locale.US, "%.1f GB", memInfo.availMem / (1024.0 * 1024.0 * 1024.0))

            // Chipset / Hardware
            val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Build.SOC_MODEL
            } else {
                Build.HARDWARE
            }
            val chipsetDisplay = if (!soc.isNullOrEmpty() && soc != "unknown") soc.uppercase() else Build.BOARD.uppercase()

            val cpuCores = Runtime.getRuntime().availableProcessors()
            val abis = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
            val glEs = actManager?.deviceConfigurationInfo?.glEsVersion ?: "3.2"

            DeviceSpecs(
                deviceName = deviceName,
                manufacturer = manufacturer,
                model = model,
                androidVersion = Build.VERSION.RELEASE,
                apiLevel = Build.VERSION.SDK_INT,
                refreshRateHz = refreshRate,
                screenResolution = "${width} × ${height}",
                screenDensityDpi = densityDpi,
                totalRamGb = ramGb,
                availableRamGb = availGb,
                chipset = chipsetDisplay,
                cpuCores = cpuCores,
                supportedAbis = abis,
                supportedRefreshRates = supportedModesList,
                glEsVersion = "OpenGL ES $glEs",
                board = Build.BOARD.uppercase()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            DeviceSpecs()
        }
    }
}
