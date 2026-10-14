package com.gamer.calibration.domain.model

data class GamepadDeviceInfo(
    val id: Int = -1,
    val name: String = "No Gamepad Connected",
    val vendorId: Int = 0,
    val productId: Int = 0,
    val isConnected: Boolean = false,
    val hasVibrator: Boolean = false,
    val hasBattery: Boolean = false,
    val batteryPercent: Float = -1f,
    val guessedBrand: String = "Unknown / Generic Gamepad",
    val connectionType: String = "Bluetooth / USB-C",
    val axesSupported: List<String> = emptyList()
)

object GamepadBrandGuesser {
    fun guess(name: String, vendorId: Int, productId: Int): String {
        val lowerName = name.lowercase()
        return when {
            vendorId == 0x045E || lowerName.contains("xbox") ->
                "Microsoft Xbox Controller (Series X|S / One)"
            vendorId == 0x054C || lowerName.contains("dualsense") || lowerName.contains("wireless controller") ->
                "Sony PlayStation DualSense / DualShock 4"
            vendorId == 0x057E || lowerName.contains("nintendo") || lowerName.contains("pro controller") ->
                "Nintendo Switch Pro Controller"
            vendorId == 0x1532 || lowerName.contains("razer") || lowerName.contains("kishi") ->
                "Razer Kishi / Mobile Gaming Controller"
            vendorId == 0x2DC8 || lowerName.contains("8bitdo") ->
                "8BitDo Controller (Hall Effect Compatible)"
            vendorId == 0x3537 || lowerName.contains("gamesir") ->
                "GameSir Mobile Controller (G8 Galileo / X2 Pro)"
            lowerName.contains("backbone") ->
                "Backbone One Gaming Controller"
            lowerName.contains("flydigi") || vendorId == 0x20BC ->
                "Flydigi Apex / Vader (Hall Effect)"
            lowerName.contains("steelseries") || vendorId == 0x1038 ->
                "SteelSeries Stratus+ Gamepad"
            vendorId != 0 ->
                "Generic Gamepad (VID: 0x${vendorId.toString(16).uppercase().padStart(4, '0')})"
            else ->
                if (name.isNotEmpty() && name != "No Gamepad Connected") name else "Generic Android HID Gamepad"
        }
    }
}

data class StickTelemetry(
    val rawX: Float = 0f,
    val rawY: Float = 0f,
    val radius: Float = 0f,
    val angleDeg: Float = 0f,
    val innerDeadzonePercent: Float = 0f,
    val outerCircularityErrorPercent: Double = 0.0,
    val totalSamplesCaptured: Long = 0L,
    val pollingRateHz: Double = 0.0
)

data class ControllerPrecisionSummary(
    val device: GamepadDeviceInfo = GamepadDeviceInfo(),
    val leftStick: StickTelemetry = StickTelemetry(),
    val rightStick: StickTelemetry = StickTelemetry(),
    val isLeftActive: Boolean = true,
    val rumbleLeftMotor: Float = 0f,
    val rumbleRightMotor: Float = 0f,
    val controllerGyroX: Float = 0f,
    val controllerGyroY: Float = 0f,
    val controllerGyroZ: Float = 0f
)
