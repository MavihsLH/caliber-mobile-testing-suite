package com.gamer.calibration.modules.reflex

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamer.calibration.ui.components.KeepScreenOn
import com.gamer.calibration.ui.components.TelemetryValue
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography
import com.gamer.calibration.ui.theme.TelemetryFonts
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random

enum class ReflexState {
    IDLE,
    PREPARING,
    TRIGGERED,
    EARLY_FAULT,
    RESULT
}

@Composable
fun ReflexSandboxScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    KeepScreenOn()
    val context = LocalContext.current
    val vibrator = remember { androidx.core.content.ContextCompat.getSystemService(context, Vibrator::class.java) }
    val appSettingsPrefs = remember { context.getSharedPreferences("apex_calibrate_prefs", Context.MODE_PRIVATE) }

    val prefs = remember {
        val oldPrefs = context.getSharedPreferences("gamersense_reflex_pb", Context.MODE_PRIVATE)
        val newPrefs = context.getSharedPreferences("caliber_reflex_pb", Context.MODE_PRIVATE)
        if (!newPrefs.contains("personal_best_ms") && oldPrefs.contains("personal_best_ms")) {
            newPrefs.edit().putLong("personal_best_ms", oldPrefs.getLong("personal_best_ms", 0L)).apply()
        }
        // Sanitize any corrupted < 80ms record caused by previous coroutine race condition
        val existingPb = newPrefs.getLong("personal_best_ms", 0L)
        if (existingPb in 1L..79L) {
            newPrefs.edit().remove("personal_best_ms").apply()
        }
        newPrefs
    }
    var personalBestMs by remember { mutableLongStateOf(prefs.getLong("personal_best_ms", 0L)) }

    var reflexState by remember { mutableStateOf(ReflexState.IDLE) }
    var triggerUptime by remember { mutableLongStateOf(0L) }
    var lastReactionMs by remember { mutableLongStateOf(0L) }
    val trialHistory = remember { mutableStateListOf<Long>() }

    val coroutineScope = rememberCoroutineScope()
    var activeTrialJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            activeTrialJob?.cancel()
        }
    }

    fun triggerHapticPulse() {
        if (!appSettingsPrefs.getBoolean("haptics_enabled", true)) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (ignored: Exception) {}
    }

    fun startTrial() {
        activeTrialJob?.cancel()
        reflexState = ReflexState.PREPARING
        activeTrialJob = coroutineScope.launch {
            val randomDelayMs = Random.nextLong(1500, 3800)
            delay(randomDelayMs)
            if (reflexState == ReflexState.PREPARING) {
                triggerUptime = SystemClock.uptimeMillis()
                reflexState = ReflexState.TRIGGERED
                triggerHapticPulse()
            }
        }
    }

    fun handleTapDown(downTimeMs: Long) {
        when (reflexState) {
            ReflexState.IDLE -> {
                startTrial()
            }
            ReflexState.PREPARING -> {
                // Cancel pending trigger coroutine immediately so it cannot fire on retry
                activeTrialJob?.cancel()
                activeTrialJob = null
                reflexState = ReflexState.EARLY_FAULT
            }
            ReflexState.TRIGGERED -> {
                val now = if (downTimeMs >= triggerUptime) downTimeMs else SystemClock.uptimeMillis()
                val delta = (now - triggerUptime).coerceAtLeast(1L)

                // Sub-80ms indicates a pre-fire anticipation tap started prior to visual recognition
                if (delta < 80L) {
                    activeTrialJob?.cancel()
                    reflexState = ReflexState.EARLY_FAULT
                    return
                }

                lastReactionMs = delta
                trialHistory.add(delta)

                if (personalBestMs == 0L || delta < personalBestMs) {
                    personalBestMs = delta
                    prefs.edit().putLong("personal_best_ms", delta).apply()
                }

                reflexState = ReflexState.RESULT
            }
            ReflexState.EARLY_FAULT, ReflexState.RESULT -> {
                startTrial()
            }
        }
    }

    val averageMs = if (trialHistory.isNotEmpty()) trialHistory.average().toLong() else 0L
    val bestInSession = if (trialHistory.isNotEmpty()) trialHistory.minOrNull() ?: 0L else 0L
    val jitterMs = if (trialHistory.size > 1) {
        val avg = trialHistory.average()
        val sumSquaredDiff = trialHistory.sumOf { (it - avg) * (it - avg) }
        sqrt(sumSquaredDiff / trialHistory.size).toLong()
    } else 0L

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val targetColor by animateColorAsState(
        targetValue = when (reflexState) {
            ReflexState.IDLE -> MinimalGlassColors.PrimaryBlue
            ReflexState.PREPARING -> MinimalGlassColors.WarningAmber
            ReflexState.TRIGGERED -> Color(0xFF00F5D4)
            ReflexState.EARLY_FAULT -> MinimalGlassColors.DangerRed
            ReflexState.RESULT -> MinimalGlassColors.SuccessGreen
        },
        label = "target_color"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MinimalGlassColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MinimalGlassColors.CardSurface)
                .border(1.dp, MinimalGlassColors.GlassBorder)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MinimalGlassColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reaction Time Test",
                    style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MinimalGlassColors.TextPrimary
                )
                Text(
                    text = "Zero-lag touch-down latency & visual reflex test",
                    style = MinimalTypography.labelSmall.copy(fontSize = 11.sp),
                    color = MinimalGlassColors.TextMuted
                )
            }

            if (trialHistory.isNotEmpty() || personalBestMs > 0L) {
                IconButton(onClick = {
                    activeTrialJob?.cancel()
                    trialHistory.clear()
                    reflexState = ReflexState.IDLE
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset session",
                        tint = MinimalGlassColors.TextSecondary
                    )
                }
            }
        }

        // Main Reflex Tap Arena using instantaneous ACTION_DOWN detection
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        handleTapDown(down.uptimeMillis)
                    }
                }
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(if (reflexState == ReflexState.PREPARING) pulseScale else 1.0f)
                        .clip(CircleShape)
                        .background(targetColor.copy(alpha = 0.15f))
                        .border(3.dp, targetColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(targetColor.copy(alpha = 0.25f))
                            .border(2.dp, targetColor.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (reflexState) {
                                ReflexState.TRIGGERED -> Icons.Default.ElectricBolt
                                ReflexState.EARLY_FAULT -> Icons.Default.Speed
                                else -> Icons.Default.TrackChanges
                            },
                            contentDescription = null,
                            tint = targetColor,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when (reflexState) {
                            ReflexState.IDLE -> "TAP TO START"
                            ReflexState.PREPARING -> "WAIT FOR CYAN..."
                            ReflexState.TRIGGERED -> "TAP NOW!"
                            ReflexState.EARLY_FAULT -> "TOO EARLY"
                            ReflexState.RESULT -> "$lastReactionMs MS"
                        },
                        style = TelemetryFonts.Hero.copy(
                            fontSize = if (reflexState == ReflexState.RESULT) 38.sp else 20.sp,
                            color = targetColor
                        )
                    )

                    Text(
                        text = when (reflexState) {
                            ReflexState.IDLE -> "Captures hardware touch-down event on contact"
                            ReflexState.PREPARING -> "Keep finger poised above display"
                            ReflexState.TRIGGERED -> "Touch screen immediately!"
                            ReflexState.EARLY_FAULT -> "Pre-fired before signal. Tap to retry."
                            ReflexState.RESULT -> getEsportsTier(lastReactionMs)
                        },
                        style = MinimalTypography.bodyMedium.copy(fontSize = 12.sp),
                        color = MinimalGlassColors.TextSecondary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MinimalGlassColors.CardSurface)
                .border(1.dp, MinimalGlassColors.GlassBorder)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryValue(
                    label = "AVERAGE",
                    value = if (averageMs > 0) "$averageMs" else "--",
                    unit = "ms"
                )
                TelemetryValue(
                    label = "BEST",
                    value = if (bestInSession > 0) "$bestInSession" else "--",
                    unit = "ms"
                )
                TelemetryValue(
                    label = "CONSISTENCY",
                    value = if (jitterMs > 0) "±$jitterMs" else "--",
                    unit = "ms"
                )
                TelemetryValue(
                    label = "RECORD",
                    value = if (personalBestMs > 0) "$personalBestMs" else "--",
                    unit = "ms"
                )
            }
        }
    }
}

private fun getEsportsTier(ms: Long): String {
    return when {
        ms < 175 -> "Pro Esports Tier (< 175ms)"
        ms in 175..215 -> "Competitive Fast (175-215ms)"
        ms in 216..265 -> "Average (216-265ms)"
        else -> "Relaxed (> 265ms)"
    }
}
