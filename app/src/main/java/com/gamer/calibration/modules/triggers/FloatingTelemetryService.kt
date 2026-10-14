package com.gamer.calibration.modules.triggers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.gamer.calibration.MainActivity
import com.gamer.calibration.R
import java.util.Locale

class FloatingTelemetryService : Service(), Choreographer.FrameCallback {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var lastFrameTimeNanos: Long = 0L
    private var frameCount = 0
    private var fpsStartTime = System.currentTimeMillis()
    private var currentFps = 60.0

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var fpsTextView: TextView? = null
    private var touchHzTextView: TextView? = null
    private var padHzTextView: TextView? = null

    companion object {
        const val CHANNEL_ID = "apex_overlay_channel"
        const val NOTIFICATION_ID = 1001
        var isRunning = false
        var sharedTouchHz: Double = 0.0
        var sharedControllerHz: Double = 0.0
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        initOverlayWindow()
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Apex Gaming Telemetry Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Real-time floating gaming stats over apps"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Telemetry Overlay Active")
            .setContentText("Drag to bottom of screen or tap ✕ to close")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun initOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 60
            y = 120
        }

        // Clean frosted container
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(20, 12, 16, 12)
            val borderDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#E60F172A")) // Translucent deep slate
                setStroke(2, android.graphics.Color.parseColor("#334155"))
                cornerRadius = 24f
            }
            background = borderDrawable
        }

        fun createStatPill(label: String, initialVal: String, colorHex: String): TextView {
            val col = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(10, 0, 10, 0)
            }
            val lbl = TextView(this).apply {
                text = label
                textSize = 9f
                setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val valText = TextView(this).apply {
                text = initialVal
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor(colorHex))
                typeface = android.graphics.Typeface.MONOSPACE
            }
            col.addView(lbl)
            col.addView(valText)
            container.addView(col)
            return valText
        }

        fpsTextView = createStatPill("FPS", "60.0", "#10B981")
        touchHzTextView = createStatPill("TOUCH", "0 Hz", "#38BDF8")
        padHzTextView = createStatPill("PAD", "0 Hz", "#F59E0B")

        // Close 'X' button
        val closeBtn = TextView(this).apply {
            text = " ✕ "
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#94A3B8"))
            setPadding(8, 0, 4, 0)
            setOnClickListener {
                stopSelf()
            }
        }
        container.addView(closeBtn)

        // Draggable touch handler with Drag-to-Bottom-to-Kill
        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.x = initialX + (event.rawX - initialTouchX).toInt()
                    params?.y = initialY + (event.rawY - initialTouchY).toInt()

                    // If dragged to bottom 15% of screen, highlight red to signal release to dismiss
                    if (event.rawY > (screenHeight * 0.85f)) {
                        (container.background as? android.graphics.drawable.GradientDrawable)?.apply {
                            setColor(android.graphics.Color.parseColor("#E6EF4444")) // Red alert
                            setStroke(2, android.graphics.Color.WHITE)
                        }
                    } else {
                        (container.background as? android.graphics.drawable.GradientDrawable)?.apply {
                            setColor(android.graphics.Color.parseColor("#E60F172A"))
                            setStroke(2, android.graphics.Color.parseColor("#334155"))
                        }
                    }

                    params?.let { windowManager?.updateViewLayout(container, it) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Check if dropped near the bottom to kill
                    if (event.rawY > (screenHeight * 0.85f)) {
                        stopSelf()
                    }
                    true
                }
                else -> false
            }
        }

        overlayView = container
        windowManager?.addView(overlayView, params)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRunning) return

        frameCount++
        val now = System.currentTimeMillis()
        val elapsed = now - fpsStartTime
        if (elapsed >= 500) {
            currentFps = (frameCount * 1000.0) / elapsed
            frameCount = 0
            fpsStartTime = now

            fpsTextView?.text = String.format(Locale.US, "%.1f", currentFps)
            touchHzTextView?.text = String.format(Locale.US, "%.0f Hz", sharedTouchHz)
            padHzTextView?.text = String.format(Locale.US, "%.0f Hz", sharedControllerHz)
        }

        lastFrameTimeNanos = frameTimeNanos
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
    }
}
