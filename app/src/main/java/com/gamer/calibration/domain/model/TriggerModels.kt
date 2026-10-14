package com.gamer.calibration.domain.model

data class TriggerState(
    val keyCode: Int,
    val name: String,
    val isPressed: Boolean = false,
    val pressDurationMs: Long = 0L,
    val lastActuationMs: Long = 0L,
    val totalClicks: Int = 0,
    val clicksPerSecond: Double = 0.0
)

data class HardwareTriggersSummary(
    val l1Trigger: TriggerState = TriggerState(android.view.KeyEvent.KEYCODE_BUTTON_L1, "L1 / LEFT SHOULDER"),
    val r1Trigger: TriggerState = TriggerState(android.view.KeyEvent.KEYCODE_BUTTON_R1, "R1 / RIGHT SHOULDER"),
    val l2Trigger: TriggerState = TriggerState(android.view.KeyEvent.KEYCODE_BUTTON_L2, "L2 / LEFT TRIGGER"),
    val r2Trigger: TriggerState = TriggerState(android.view.KeyEvent.KEYCODE_BUTTON_R2, "R2 / RIGHT TRIGGER"),
    val volumeUpTrigger: TriggerState = TriggerState(android.view.KeyEvent.KEYCODE_VOLUME_UP, "VOL+ (MAPPED TRIGGER)"),
    val volumeDownTrigger: TriggerState = TriggerState(android.view.KeyEvent.KEYCODE_VOLUME_DOWN, "VOL- (MAPPED TRIGGER)"),
    val lastTriggerLatencyMs: Long = 0L,
    val peakClicksPerSecond: Double = 0.0,
    val isOverlayActive: Boolean = false
)
