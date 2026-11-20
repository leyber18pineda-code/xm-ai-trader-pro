package com.xmaitrader.pro

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.drawable.GradientDrawable

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        showBubble()
        return START_STICKY
    }


    private fun startAsForeground() {
        val channelId = "xm_ai_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "XM AI floating assistant", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("XM AI TRADER PRO")
                .setContentText("La burbuja flotante está activa")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("XM AI TRADER PRO")
                .setContentText("La burbuja flotante está activa")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        }
        startForeground(1001, notification)
    }

    private fun params(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width, height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 220 }
    }

    private fun rounded(color: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = 28f }

    private fun showBubble() {
        if (bubble != null) return
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val b = TextView(this).apply {
            text = "XM\nAI"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(21, 101, 192))
            setPadding(18, 12, 18, 12)
            setOnClickListener { showPanel() }
        }
        val p = params(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        b.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = p.x; startY = p.y
                    touchX = event.rawX; touchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = startX + (event.rawX - touchX).toInt()
                    p.y = startY + (event.rawY - touchY).toInt()
                    wm.updateViewLayout(b, p)
                    true
                }
                else -> false
            }
        }
        wm.addView(b, p)
        bubble = b
    }

    private fun showPanel() {
        if (panel != null) {
            try { wm.removeView(panel) } catch (_: Exception) {}
            panel = null
            return
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            background = rounded(Color.rgb(15, 23, 42))
        }
        val title = TextView(this).apply {
            text = "XM AI TRADER PRO"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        val subtitle = TextView(this).apply {
            text = "Asistente de análisis Forex"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 16)
        }
        val analyze = Button(this).apply {
            text = "ANALIZAR"
            setOnClickListener {
                subtitle.text = "Sin conexión a datos de mercado. No se emitirá una señal inventada."
            }
        }
        val close = Button(this).apply {
            text = "CERRAR"
            setOnClickListener {
                try { wm.removeView(panel) } catch (_: Exception) {}
                panel = null
            }
        }
        box.addView(title); box.addView(subtitle); box.addView(analyze); box.addView(close)
        val p = params(680, WindowManager.LayoutParams.WRAP_CONTENT).apply {
            width = (resources.displayMetrics.widthPixels * 0.88f).toInt()
            x = (resources.displayMetrics.widthPixels * 0.06f).toInt()
            y = (resources.displayMetrics.heightPixels * 0.25f).toInt()
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }
        wm.addView(box, p)
        panel = box
    }

    override fun onDestroy() {
        try { bubble?.let { wm.removeView(it) } } catch (_: Exception) {}
        try { panel?.let { wm.removeView(it) } } catch (_: Exception) {}
        bubble = null; panel = null
        super.onDestroy()
    }
}
