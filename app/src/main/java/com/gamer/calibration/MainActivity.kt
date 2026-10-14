package com.gamer.calibration

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.gamer.calibration.domain.model.DiagnosticReport
import com.gamer.calibration.modules.audio.AudioLatencyEngine
import com.gamer.calibration.modules.audio.BareMetalAudioLabScreen
import com.gamer.calibration.modules.controller.ControllerPrecisionLabScreen
import com.gamer.calibration.modules.controller.ControllerTelemetryEngine
import com.gamer.calibration.modules.motion.MotionSensorLabScreen
import com.gamer.calibration.modules.motion.MotionTelemetryEngine
import com.gamer.calibration.modules.touch.TouchDigitizerLabScreen
import com.gamer.calibration.modules.touch.TouchTelemetryEngine
import com.gamer.calibration.modules.triggers.HardwareTriggerLabScreen
import com.gamer.calibration.modules.triggers.TriggerTelemetryEngine
import com.gamer.calibration.modules.sensitivity.SensitivityVaultScreen
import com.gamer.calibration.modules.tuner.SystemTunerHubScreen
import com.gamer.calibration.modules.reflex.ReflexSandboxScreen
import com.gamer.calibration.modules.avsync.AvSyncCalibrationScreen
import com.gamer.calibration.ui.components.CyberButton
import com.gamer.calibration.ui.dashboard.BentoDashboardScreen
import com.gamer.calibration.ui.dashboard.CalibrationModule
import com.gamer.calibration.ui.dashboard.DiagnosticReportScreen
import com.gamer.calibration.ui.dashboard.TestDetailConfig
import com.gamer.calibration.ui.dashboard.TestDetailScreen
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography
import com.gamer.calibration.ui.theme.MobileGamerCalibrationTheme

class MainActivity : ComponentActivity() {

    private lateinit var touchEngine: TouchTelemetryEngine
    private lateinit var motionEngine: MotionTelemetryEngine
    private lateinit var triggerEngine: TriggerTelemetryEngine
    private lateinit var controllerEngine: ControllerTelemetryEngine
    private lateinit var audioEngine: AudioLatencyEngine

    private var diagnosticReport by mutableStateOf(DiagnosticReport())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (::audioEngine.isInitialized) {
            audioEngine.detectAudioRouting()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize telemetry & hardware engines eagerly
        touchEngine = TouchTelemetryEngine(lifecycleScope)
        motionEngine = MotionTelemetryEngine(this, lifecycleScope)
        triggerEngine = TriggerTelemetryEngine(lifecycleScope)
        controllerEngine = ControllerTelemetryEngine(this, lifecycleScope)
        audioEngine = AudioLatencyEngine(this, lifecycleScope)

        // Initial launch in Portrait
        setPortraitOrientation()

        // Edge-to-edge layout with safe insets
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = getSharedPreferences("apex_calibrate_prefs", Context.MODE_PRIVATE)
        MinimalGlassColors.isDarkTheme = prefs.getBoolean("dark_mode_enabled", false)

        checkRequiredPermissions()

        // Asynchronously initialize Google Mobile Ads SDK without blocking UI startup
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.google.android.gms.ads.MobileAds.initialize(this@MainActivity) {}
            } catch (_: Exception) {}
        }

        setContent {
            var currentModule by remember { mutableStateOf(CalibrationModule.DASHBOARD) }
            var selectedTestConfig by remember { mutableStateOf<TestDetailConfig?>(null) }
            var showAutoRotateDialog by remember { mutableStateOf(false) }
            var pendingTargetModule by remember { mutableStateOf<CalibrationModule?>(null) }

            MobileGamerCalibrationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MinimalGlassColors.Background
                ) {
                    // Friendly Auto-Rotate Dialog
                    if (showAutoRotateDialog) {
                        AlertDialog(
                            onDismissRequest = { showAutoRotateDialog = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MinimalGlassColors.CardSurface,
                            title = {
                                Text(
                                    text = "Auto-Rotate Recommended",
                                    style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MinimalGlassColors.TextPrimary
                                )
                            },
                            text = {
                                Text(
                                    text = "Auto-Rotate is currently turned off in your phone's Quick Settings. Enable it so your phone naturally rotates into landscape when held horizontally, or force landscape directly.",
                                    style = MinimalTypography.bodyMedium,
                                    color = MinimalGlassColors.TextSecondary
                                )
                            },
                            confirmButton = {
                                CyberButton(
                                    text = "Force Landscape",
                                    onClick = {
                                        showAutoRotateDialog = false
                                        forceLandscapeOrientation()
                                        pendingTargetModule?.let { currentModule = it }
                                    },
                                    accentColor = MinimalGlassColors.PrimaryBlue,
                                    filled = true
                                )
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showAutoRotateDialog = false
                                        try {
                                            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                                        } catch (e: Exception) {
                                            try {
                                                startActivity(Intent(Settings.ACTION_SETTINGS))
                                            } catch (ignored: Exception) {}
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "Open Settings",
                                        style = MinimalTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MinimalGlassColors.TextSecondary
                                    )
                                }
                            }
                        )
                    }

                    LaunchedEffect(currentModule) {
                        when (currentModule) {
                            CalibrationModule.DASHBOARD,
                            CalibrationModule.TEST_DETAIL,
                            CalibrationModule.DIAGNOSTIC_REPORT,
                            CalibrationModule.SENSITIVITY_VAULT,
                            CalibrationModule.SYSTEM_TUNER_HUB,
                            CalibrationModule.REFLEX_SANDBOX,
                            CalibrationModule.AV_SYNC_LAB -> {
                                setPortraitOrientation()
                                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            }
                            else -> {
                                val keepAwake = prefs.getBoolean("keep_screen_awake", true)
                                if (keepAwake) {
                                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                } else {
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                }
                                if (isSystemAutoRotateEnabled(this@MainActivity)) {
                                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = currentModule,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "module_transition"
                    ) { module ->
                        when (module) {
                            CalibrationModule.DASHBOARD -> {
                                BentoDashboardScreen(
                                    report = diagnosticReport,
                                    controllerEngine = controllerEngine,
                                    audioEngine = audioEngine,
                                    onSelectModuleForDetail = { targetLab ->
                                        when (targetLab) {
                                            CalibrationModule.SENSITIVITY_VAULT,
                                            CalibrationModule.SYSTEM_TUNER_HUB,
                                            CalibrationModule.REFLEX_SANDBOX,
                                            CalibrationModule.AV_SYNC_LAB -> {
                                                currentModule = targetLab
                                            }
                                            else -> {
                                                selectedTestConfig = getTestConfigFor(targetLab)
                                                currentModule = CalibrationModule.TEST_DETAIL
                                            }
                                        }
                                    },
                                    onOpenReport = {
                                        currentModule = CalibrationModule.DIAGNOSTIC_REPORT
                                    },
                                    onThemeChanged = { isDark ->
                                        prefs.edit().putBoolean("dark_mode_enabled", isDark).apply()
                                    }
                                )
                            }

                            CalibrationModule.TEST_DETAIL -> {
                                selectedTestConfig?.let { cfg ->
                                    TestDetailScreen(
                                        config = cfg,
                                        onBack = { currentModule = CalibrationModule.DASHBOARD },
                                        onStartTest = {
                                            if (!isSystemAutoRotateEnabled(this@MainActivity)) {
                                                pendingTargetModule = cfg.module
                                                showAutoRotateDialog = true
                                            } else {
                                                currentModule = cfg.module
                                            }
                                        }
                                    )
                                }
                            }

                            CalibrationModule.DIAGNOSTIC_REPORT -> {
                                DiagnosticReportScreen(
                                    report = diagnosticReport,
                                    onBack = { currentModule = CalibrationModule.DASHBOARD }
                                )
                            }

                            CalibrationModule.TOUCHSCREEN_LAB -> {
                                TouchDigitizerLabScreen(
                                    touchEngine = touchEngine,
                                    onBack = { currentModule = CalibrationModule.DASHBOARD },
                                    onToggleOrientation = { toggleLabOrientation() },
                                    onFinish = { hz, pointers ->
                                        diagnosticReport = diagnosticReport.copy(
                                            touchCompleted = true,
                                            touchSamplingHz = hz,
                                            touchMaxPointers = pointers
                                        )
                                        currentModule = CalibrationModule.DIAGNOSTIC_REPORT
                                    }
                                )
                            }

                            CalibrationModule.MOTION_SENSOR_LAB -> {
                                MotionSensorLabScreen(
                                    motionEngine = motionEngine,
                                    onBack = { currentModule = CalibrationModule.DASHBOARD },
                                    onToggleOrientation = { toggleLabOrientation() },
                                    onFinish = { drift, calibrated ->
                                        diagnosticReport = diagnosticReport.copy(
                                            motionCompleted = true,
                                            motionDriftDegs = drift,
                                            isMotionCalibrated = calibrated
                                        )
                                        currentModule = CalibrationModule.DIAGNOSTIC_REPORT
                                    }
                                )
                            }

                            CalibrationModule.HARDWARE_TRIGGERS_LAB -> {
                                HardwareTriggerLabScreen(
                                    triggerEngine = triggerEngine,
                                    onBack = { currentModule = CalibrationModule.DASHBOARD },
                                    onToggleOrientation = { toggleLabOrientation() },
                                    onFinish = { latency, cps ->
                                        diagnosticReport = diagnosticReport.copy(
                                            triggerCompleted = true,
                                            triggerLatencyMs = latency,
                                            triggerPeakCps = cps
                                        )
                                        currentModule = CalibrationModule.DIAGNOSTIC_REPORT
                                    }
                                )
                            }

                            CalibrationModule.CONTROLLER_LAB -> {
                                ControllerPrecisionLabScreen(
                                    controllerEngine = controllerEngine,
                                    onBack = { currentModule = CalibrationModule.DASHBOARD },
                                    onToggleOrientation = { toggleLabOrientation() },
                                    onFinish = { error, connected ->
                                        diagnosticReport = diagnosticReport.copy(
                                            controllerCompleted = true,
                                            controllerCircularityErrorPercent = error,
                                            controllerConnected = connected
                                        )
                                        currentModule = CalibrationModule.DIAGNOSTIC_REPORT
                                    }
                                )
                            }

                            CalibrationModule.AUDIO_LATENCY_LAB -> {
                                BareMetalAudioLabScreen(
                                    audioEngine = audioEngine,
                                    onBack = { currentModule = CalibrationModule.DASHBOARD },
                                    onToggleOrientation = { toggleLabOrientation() },
                                    onFinish = { latency, deviceName ->
                                        diagnosticReport = diagnosticReport.copy(
                                            audioCompleted = true,
                                            audioLatencyMs = latency,
                                            audioDevice = deviceName
                                        )
                                        currentModule = CalibrationModule.DIAGNOSTIC_REPORT
                                    }
                                )
                            }

                            CalibrationModule.SENSITIVITY_VAULT -> {
                                SensitivityVaultScreen(
                                    onBack = { currentModule = CalibrationModule.DASHBOARD }
                                )
                            }

                            CalibrationModule.SYSTEM_TUNER_HUB -> {
                                SystemTunerHubScreen(
                                    onBack = { currentModule = CalibrationModule.DASHBOARD }
                                )
                            }

                            CalibrationModule.REFLEX_SANDBOX -> {
                                ReflexSandboxScreen(
                                    onBack = { currentModule = CalibrationModule.DASHBOARD }
                                )
                            }

                            CalibrationModule.AV_SYNC_LAB -> {
                                AvSyncCalibrationScreen(
                                    onBack = { currentModule = CalibrationModule.DASHBOARD }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isSystemAutoRotateEnabled(context: Context): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        } catch (e: Exception) {
            true
        }
    }

    private fun setPortraitOrientation() {
        if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun setLandscapeOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    private fun forceLandscapeOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    private fun toggleLabOrientation() {
        requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    private fun getTestConfigFor(module: CalibrationModule): TestDetailConfig {
        return when (module) {
            CalibrationModule.TOUCHSCREEN_LAB -> TestDetailConfig(
                module = CalibrationModule.TOUCHSCREEN_LAB,
                title = "Touch Screen & Digitizer",
                category = "Display Hardware",
                icon = Icons.Default.TouchApp,
                iconTint = MinimalGlassColors.PrimaryBlue,
                iconBg = MinimalGlassColors.PrimaryBlueLight,
                whatItIs = "Checks the real hardware frequency at which your touchscreen registers finger movements.",
                whyItMatters = "Higher touch sampling rates reduce input lag, giving you snappier flick-shots and micro-aim adjustments in FPS titles.",
                instructions = listOf(
                    "Swipe rapidly back and forth across the screen with 1 finger to benchmark maximum polling rate.",
                    "Place 4 or more fingers simultaneously on screen to verify multi-touch claw grip support."
                ),
                benchmarks = listOf(
                    "Optimal Esports" to "240Hz+",
                    "Standard Gaming" to "120Hz - 180Hz",
                    "Casual Phone" to "60Hz"
                )
            )

            CalibrationModule.MOTION_SENSOR_LAB -> TestDetailConfig(
                module = CalibrationModule.MOTION_SENSOR_LAB,
                title = "Motion & Gyroscope",
                category = "6-Axis IMU Sensors",
                icon = Icons.Default.Sensors,
                iconTint = MinimalGlassColors.SuccessGreen,
                iconBg = MinimalGlassColors.SuccessGreenLight,
                whatItIs = "Measures resting sensor drift and calibrates the hardware zero-point bias offset.",
                whyItMatters = "Eliminates unwanted crosshair wandering or creeping when using gyro aiming in PUBG Mobile and CoD Mobile.",
                instructions = listOf(
                    "Place your phone completely flat on a table and do not touch it for 3 seconds.",
                    "Rotate the phone smoothly 90° horizontally to confirm dynamic axis response."
                ),
                benchmarks = listOf(
                    "Esports Tolerance" to "< 0.5°/s drift",
                    "Noticeable Drift" to "0.5° - 1.0°/s",
                    "Severe Drift" to "> 1.0°/s (Requires Tare)"
                )
            )

            CalibrationModule.AUDIO_LATENCY_LAB -> TestDetailConfig(
                module = CalibrationModule.AUDIO_LATENCY_LAB,
                title = "Audio Latency",
                category = "Sound Hardware",
                icon = Icons.Default.GraphicEq,
                iconTint = MinimalGlassColors.Purple,
                iconBg = MinimalGlassColors.PurpleLight,
                whatItIs = "Measures the physical sound delay from your device speakers or wireless earbuds to your ears.",
                whyItMatters = "High audio latency causes gunshot sounds and footstep cues to play hundreds of milliseconds late.",
                instructions = listOf(
                    "Ensure phone volume is audible and environment is relatively quiet.",
                    "Tap 'Test Acoustic Delay' to fire a quick sound pulse and calculate round-trip arrival time."
                ),
                benchmarks = listOf(
                    "Wired 3.5mm / Type-C" to "< 30 ms",
                    "Earbuds Gaming Mode" to "40 - 70 ms",
                    "Standard Bluetooth" to "150 - 250 ms"
                )
            )

            CalibrationModule.HARDWARE_TRIGGERS_LAB -> TestDetailConfig(
                module = CalibrationModule.HARDWARE_TRIGGERS_LAB,
                title = "Gaming Triggers & HUD",
                category = "Hardware Controls",
                icon = Icons.Default.Layers,
                iconTint = MinimalGlassColors.WarningAmber,
                iconBg = MinimalGlassColors.WarningAmberLight,
                whatItIs = "Tests gaming phone shoulder buttons (air triggers) and controls the in-game floating FPS overlay.",
                whyItMatters = "Measures actuation click latency and lets you monitor live FPS over Genshin Impact, CoD, and PUBG.",
                instructions = listOf(
                    "Tap your phone's physical triggers or volume keys 5 times rapidly.",
                    "Toggle the floating HUD overlay to monitor live FPS over other games. Drag it to the bottom to close anytime."
                ),
                benchmarks = listOf(
                    "Fast Actuation" to "< 40 ms",
                    "Rapid-Fire Speed" to "8 - 12+ CPS"
                )
            )

            CalibrationModule.CONTROLLER_LAB -> TestDetailConfig(
                module = CalibrationModule.CONTROLLER_LAB,
                title = "Controller & Joysticks",
                category = "External Gamepads",
                icon = Icons.Default.SportsEsports,
                iconTint = MinimalGlassColors.PrimaryBlue,
                iconBg = MinimalGlassColors.PrimaryBlueLight,
                whatItIs = "Interrogates connected gamepads and computes analog stick outer boundary circularity error.",
                whyItMatters = "Exposes whether your thumbsticks have true circular gating (Hall Effect / TMR) or square corner clipping (potentiometers).",
                instructions = listOf(
                    "Connect your Bluetooth or USB-C gamepad to your phone.",
                    "Rotate the analog thumbstick in 3 full 360° circles to benchmark circularity error.",
                    "Use the dual rumble sliders to test left and right vibration motors."
                ),
                benchmarks = listOf(
                    "Hall Effect / TMR" to "< 5% error (True Circle)",
                    "Standard Controller" to "8 - 14% error",
                    "Square Clipping" to "> 15% error"
                )
            )

            else -> TestDetailConfig(
                module = CalibrationModule.TOUCHSCREEN_LAB,
                title = "Touch Screen",
                category = "Display",
                icon = Icons.Default.TouchApp,
                iconTint = MinimalGlassColors.PrimaryBlue,
                iconBg = MinimalGlassColors.PrimaryBlueLight,
                whatItIs = "",
                whyItMatters = "",
                instructions = emptyList(),
                benchmarks = emptyList()
            )
        }
    }

    private fun checkRequiredPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::triggerEngine.isInitialized && triggerEngine.handleKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (::controllerEngine.isInitialized && controllerEngine.handleGenericMotionEvent(event)) {
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onResume() {
        super.onResume()
        if (::controllerEngine.isInitialized) {
            controllerEngine.scanConnectedGamepads()
        }
        if (::audioEngine.isInitialized) {
            audioEngine.detectAudioRouting()
        }
    }
}
