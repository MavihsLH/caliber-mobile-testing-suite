package com.gamer.calibration.modules.avsync

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamer.calibration.ui.components.CyberButton
import com.gamer.calibration.ui.components.KeepScreenOn
import com.gamer.calibration.ui.components.TelemetryValue
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography
import com.gamer.calibration.ui.theme.TelemetryFonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sin

@Composable
fun AvSyncCalibrationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    KeepScreenOn()
    val context = LocalContext.current

    var isRunning by remember { mutableStateOf(false) }
    var isBeatFlashing by remember { mutableStateOf(false) }
    val beatPeriodMs = 800L // 75 BPM rhythm cadence
    var lastBeatTimestamp by remember { mutableLongStateOf(0L) }

    val tapOffsets = remember { mutableStateListOf<Long>() }
    var lastOffsetMs by remember { mutableLongStateOf(0L) }

    val audioTrack = remember {
        val sampleRate = 44100
        val durationMs = 30
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        val freq = 1400.0
        for (i in 0 until numSamples) {
            val env = 1.0 - (i.toDouble() / numSamples)
            val angle = 2.0 * Math.PI * i * freq / sampleRate
            buffer[i] = (env * sin(angle) * Short.MAX_VALUE * 0.85).toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buffer, 0, buffer.size)
        track
    }

    DisposableEffect(Unit) {
        onDispose {
            isRunning = false
            try {
                audioTrack.release()
            } catch (ignored: Exception) {}
        }
    }

    fun playClick() {
        try {
            audioTrack.stop()
            audioTrack.reloadStaticData()
            audioTrack.play()
        } catch (ignored: Exception) {}
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            delay(400L) // Brief lead-in after starting so start tap doesn't collide with Beat #1
            while (isActive && isRunning) {
                lastBeatTimestamp = SystemClock.uptimeMillis()
                isBeatFlashing = true
                playClick()
                delay(90L)
                isBeatFlashing = false
                delay(beatPeriodMs - 90L)
            }
        } else {
            isBeatFlashing = false
        }
    }

    fun handleTapDown(downUptimeMs: Long) {
        if (!isRunning) {
            isRunning = true
            return
        }
        if (lastBeatTimestamp == 0L) return

        val elapsedSinceBeat = downUptimeMs - lastBeatTimestamp
        val offset = if (elapsedSinceBeat > beatPeriodMs / 2) {
            elapsedSinceBeat - beatPeriodMs
        } else {
            elapsedSinceBeat
        }

        lastOffsetMs = offset
        tapOffsets.add(offset)
        if (tapOffsets.size > 12) {
            tapOffsets.removeAt(0)
        }
    }

    val averageOffsetMs = if (tapOffsets.isNotEmpty()) {
        tapOffsets.average().toLong()
    } else 0L

    val flashScale by animateFloatAsState(
        targetValue = if (isBeatFlashing) 1.12f else 1.0f,
        animationSpec = tween(durationMillis = if (isBeatFlashing) 40 else 180),
        label = "beat_flash_scale"
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
                    text = "Audio Delay Sync",
                    style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MinimalGlassColors.TextPrimary
                )
                Text(
                    text = "Measure audio-visual offset for rhythm games & emulators",
                    style = MinimalTypography.labelSmall.copy(fontSize = 11.sp),
                    color = MinimalGlassColors.TextMuted
                )
            }

            if (tapOffsets.isNotEmpty()) {
                IconButton(onClick = {
                    tapOffsets.clear()
                    lastOffsetMs = 0L
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset samples",
                        tint = MinimalGlassColors.TextSecondary
                    )
                }
            }

            IconButton(onClick = { isRunning = !isRunning }) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Stop" else "Start",
                    tint = if (isRunning) MinimalGlassColors.DangerRed else MinimalGlassColors.PrimaryBlue
                )
            }
        }

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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(flashScale)
                        .clip(CircleShape)
                        .background(
                            when {
                                isBeatFlashing -> MinimalGlassColors.Purple.copy(alpha = 0.40f)
                                isRunning -> MinimalGlassColors.PurpleLight
                                else -> MinimalGlassColors.BackgroundSubtle
                            }
                        )
                        .border(
                            width = if (isBeatFlashing) 4.dp else 2.dp,
                            color = if (isRunning) MinimalGlassColors.Purple else MinimalGlassColors.GlassBorder,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = if (isRunning) MinimalGlassColors.Purple else MinimalGlassColors.TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (!isRunning) {
                            "TAP TO START"
                        } else if (tapOffsets.isEmpty()) {
                            "TAP ON THE BEAT"
                        } else {
                            "${if (averageOffsetMs >= 0) "+$averageOffsetMs" else "$averageOffsetMs"} MS"
                        },
                        style = TelemetryFonts.Hero.copy(
                            fontSize = if (tapOffsets.isNotEmpty()) 36.sp else 18.sp,
                            color = if (tapOffsets.isNotEmpty()) MinimalGlassColors.Purple else MinimalGlassColors.TextPrimary
                        )
                    )

                    Text(
                        text = if (!isRunning) {
                            "Tap in rhythm with the audio click & purple flash"
                        } else if (tapOffsets.size < 4) {
                            "Keep tapping along with the beat (${tapOffsets.size}/8 taps)"
                        } else {
                            "Recommended audio offset for rhythm games & emulators"
                        },
                        style = MinimalTypography.bodyMedium.copy(fontSize = 12.sp),
                        color = MinimalGlassColors.TextSecondary
                    )
                }

                if (tapOffsets.size >= 4) {
                    CyberButton(
                        text = "Copy Offset (${averageOffsetMs}ms)",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Caliber Audio Offset", "${averageOffsetMs}ms")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Offset copied: ${averageOffsetMs}ms", Toast.LENGTH_SHORT).show()
                        },
                        accentColor = MinimalGlassColors.Purple,
                        filled = false
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
                    label = "LAST OFFSET",
                    value = if (tapOffsets.isNotEmpty()) "${if (lastOffsetMs > 0) "+$lastOffsetMs" else "$lastOffsetMs"}" else "--",
                    unit = "ms"
                )
                TelemetryValue(
                    label = "AVERAGE LAG",
                    value = if (tapOffsets.isNotEmpty()) "${if (averageOffsetMs > 0) "+$averageOffsetMs" else "$averageOffsetMs"}" else "--",
                    unit = "ms"
                )
                TelemetryValue(
                    label = "SAMPLES",
                    value = "${tapOffsets.size}",
                    unit = "taps"
                )
            }
        }
    }
}
