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
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.drawable.GradientDrawable
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        showBubble()
        return START_STICKY
    }

    private fun startAsForeground() {
        val channelId = "xm_ai_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "XM AI floating assistant", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId).setContentTitle("XM AI TRADER PRO")
                .setContentText("La burbuja flotante está activa").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this).setContentTitle("XM AI TRADER PRO")
                .setContentText("La burbuja flotante está activa").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        }
        startForeground(1001, notification)
    }

    private fun params(width: Int, height: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
            .apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 220 }

    private fun rounded(color: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = 28f }

    private fun showBubble() {
        if (bubble != null) return
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val b = TextView(this).apply {
            text = "XM\nAI"; textSize = 14f; setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            background = rounded(Color.rgb(21, 101, 192)); setPadding(18, 12, 18, 12)
            setOnClickListener { showPanel() }
        }
        val p = params(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f
        b.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { startX = p.x; startY = p.y; touchX = event.rawX; touchY = event.rawY; false }
                MotionEvent.ACTION_MOVE -> {
                    p.x = startX + (event.rawX - touchX).toInt()
                    p.y = startY + (event.rawY - touchY).toInt()
                    wm.updateViewLayout(b, p); true
                }
                else -> false
            }
        }
        wm.addView(b, p); bubble = b
    }

    private fun showPanel() {
        if (panel != null) {
            try { wm.removeView(panel) } catch (_: Exception) {}
            panel = null
            return
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(28, 24, 28, 24)
            background = rounded(Color.rgb(15, 23, 42))
        }
        val title = TextView(this).apply {
            text = "XM AI TRADER PRO"; textSize = 18f
            typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
        }
        val subtitle = TextView(this).apply {
            text = "Listo para consultar EUR/USD"; textSize = 13f
            setTextColor(Color.LTGRAY); setPadding(0, 8, 0, 16)
        }
        val analyze = Button(this).apply {
            text = "ANALIZAR"
            setOnClickListener {
                this.isEnabled = false
                subtitle.text = "Consultando cotización EUR/USD..."
                Thread {
                    val result: String = try {
                        val conn = URL("https://biquote.io/api/EURUSD?allowStale=false").openConnection() as HttpURLConnection
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        conn.requestMethod = "GET"
                        val status = conn.responseCode
                        if (status != 200) throw Exception("HTTP " + status)
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        conn.disconnect()
                        val data = JSONObject(body)
                        val stale = data.optBoolean("stale", true)
                        val age = data.optDouble("quoteAgeSeconds", 999999.0)
                        val state = data.optString("marketState", "unknown")
                        if (stale || age > 300.0 || state != "open") {
                            "WAIT — cotización antigua o mercado cerrado. No se genera señal."
                        } else {
                            "EUR/USD\nPrecio medio: " + data.optDouble("mid") +
                                "\nBid: " + data.optDouble("bid") +
                                "\nAsk: " + data.optDouble("ask") +
                                "\nDato actualizado hace " + age.toInt() +
                                " segundos.\nAún no hay señal BUY/SELL: primero validamos los datos."
                        }
                    } catch (e: Exception) {
                        "WAIT — no se pudo obtener un dato fiable: " + (e.message ?: "error de conexión")
                    }
                    mainHandler.post {
                        subtitle.text = result
                        this.isEnabled = true
                    }
                }.start()
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
        wm.addView(box, p); panel = box
    }

    override fun onDestroy() {
        try { bubble?.let { wm.removeView(it) } } catch (_: Exception) {}
        try { panel?.let { wm.removeView(it) } } catch (_: Exception) {}
        bubble = null; panel = null
        super.onDestroy()
    }
}