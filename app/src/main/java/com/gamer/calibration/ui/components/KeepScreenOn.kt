package com.gamer.calibration.ui.components

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Keeps the device screen awake and prevents dimming or display sleep while active.
 * Applies both View.keepScreenOn and Activity window FLAG_KEEP_SCREEN_ON for guaranteed
 * compatibility without requiring battery-draining background wake locks.
 */
@Composable
fun KeepScreenOn() {
    val currentView = LocalView.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("apex_calibrate_prefs", Context.MODE_PRIVATE) }
    val enabled = prefs.getBoolean("keep_screen_awake", true)

    if (enabled) {
        DisposableEffect(Unit) {
            val window = (context as? Activity)?.window
            currentView.keepScreenOn = true
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            onDispose {
                currentView.keepScreenOn = false
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}
