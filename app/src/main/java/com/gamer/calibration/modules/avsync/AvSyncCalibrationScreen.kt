package com.gamer.calibration.modules.avsync

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AvSyncCalibrationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    KeepScreenOn()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRunning by remember { mutableStateOf(false) }
    var beatPeriodMs by remember { mutableLongStateOf(1000L) } // 60 BPM
    var lastBeatTimestamp by remember { mutableLongStateOf(0L) }

    val tapOffsets = remember { mutableStateListOf<Long>() }
    var lastOffsetMs by remember { mutableLongStateOf(0L) }

    // Synthesized Sound Blip
    val audioTrack = remember {
        val sampleRate = 44100
        val durationMs = 35
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        val freq = 1200.0 // 1200 Hz high frequency click
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i * freq / sampleRate
            buffer[i] = (sin(angle) * Short.MAX_VALUE * 0.8).toInt().toShort()
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

    // Metronome Loop
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isActive && isRunning) {
                lastBeatTimestamp = SystemClock.uptimeMillis()
                playClick()
                delay(beatPeriodMs)
            }
        }
    }

    fun handleTap() {
        if (!isRunning) {
            isRunning = true
            return
        }

        val now = SystemClock.uptimeMillis()
        val elapsedSinceBeat = now - lastBeatTimestamp
        // Nearest beat offset (-500ms to +500ms)
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MinimalGlassColors.Background)
    ) {
        // Top Bar
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
                    text = "Measure audio delay offset for rhythm games and emulators",
                    style = MinimalTypography.labelSmall.copy(fontSize = 11.sp),
                    color = MinimalGlassColors.TextMuted
                )
            }

            IconButton(onClick = {
                isRunning = !isRunning
            }) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Stop" else "Start",
                    tint = if (isRunning) MinimalGlassColors.DangerRed else MinimalGlassColors.PrimaryBlue
                )
            }
        }

        // Tap Zone & Visual Metronome
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { handleTap() }
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Visual Beat Flash Target
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRunning) MinimalGlassColors.PurpleLight else MinimalGlassColors.BackgroundSubtle
                        )
                        .border(
                            2.dp,
                            if (isRunning) MinimalGlassColors.Purple else MinimalGlassColors.GlassBorder,
                            CircleShape
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
                        text = if (!isRunning) "TAP TO START" else if (tapOffsets.isEmpty()) "TAP ON THE BEAT" else "${if (averageOffsetMs >= 0) "+$averageOffsetMs" else "$averageOffsetMs"} MS",
                        style = TelemetryFonts.Hero.copy(
                            fontSize = if (tapOffsets.isNotEmpty()) 36.sp else 18.sp,
                            color = if (tapOffsets.isNotEmpty()) MinimalGlassColors.Purple else MinimalGlassColors.TextPrimary
                        )
                    )

                    Text(
                        text = if (!isRunning) {
                            "Tap when you hear the click pulse"
                        } else if (tapOffsets.size < 4) {
                            "Keep tapping along with the beat (${tapOffsets.size}/8 taps)"
                        } else {
                            "Audio delay offset calculated from your taps"
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

        // Bottom Stats Footer
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
                    value = if (lastOffsetMs != 0L) "${if (lastOffsetMs > 0) "+$lastOffsetMs" else "$lastOffsetMs"}" else "--",
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
