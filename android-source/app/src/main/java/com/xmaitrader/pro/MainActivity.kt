package com.xmaitrader.pro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }
        val title = TextView(this).apply {
            text = "XM AI TRADER PRO\nAsistente flotante"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val info = TextView(this).apply {
            text = "Activa el permiso para mostrar la burbuja sobre XM.\n\nEsta versión inicial no recibe cotizaciones ni genera señales reales."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }
        val button = Button(this).apply {
            text = "ACTIVAR BURBUJA"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    startService(Intent(this@MainActivity, FloatingService::class.java))
                    finish()
                }
            }
        }
        layout.addView(title)
        layout.addView(info)
        layout.addView(button)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            // User returns after granting overlay permission; tap the button to start bubble.
        }
    }
}
