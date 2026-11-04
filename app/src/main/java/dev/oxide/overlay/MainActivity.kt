package dev.oxide.overlay

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density
        val pad = (16 * dp).toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(pad, pad, pad, pad)
        }
        layout.addView(TextView(this).apply {
            text = "Overlay viewer\nLocal frame input: 127.0.0.1:${FrameSocketServer.DEFAULT_PORT} (one JSON object per line)."
            textSize = 16f
            setTextColor(Color.BLACK)
        })
        status = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFF444444.toInt())
            setPadding(0, pad, 0, pad)
        }
        layout.addView(status)
        layout.addView(button("Grant overlay permission") { requestOverlayPermission() })
        layout.addView(button("Start test overlay") { startOverlayWithTestFrame() })
        layout.addView(button("Stop overlay") {
            stopService(Intent(this, EspOverlayService::class.java))
            toast("Overlay stopped")
        })
        setContentView(layout)

        // targetSdk 35 forces edge-to-edge: keep the buttons out from under the system bars.
        layout.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                pad + insets.systemWindowInsetLeft,
                pad + insets.systemWindowInsetTop,
                pad + insets.systemWindowInsetRight,
                pad + insets.systemWindowInsetBottom
            )
            insets
        }
        layout.requestApplyInsets()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (8 * resources.displayMetrics.density).toInt() }
        gravity = Gravity.CENTER
        setOnClickListener { v: View -> onClick() }
    }

    private fun refreshStatus() {
        status.text = if (Settings.canDrawOverlays(this))
            "Overlay permission: granted" else "Overlay permission: NOT granted"
    }

    private fun requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            toast("Already granted")
        } else {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }
    }

    private fun startOverlayWithTestFrame() {
        if (!Settings.canDrawOverlays(this)) {
            toast("Grant overlay permission first")
            requestOverlayPermission()
            return
        }
        // Deterministic demo frame to validate scaling, transparency and rotation.
        OverlayFrameStore.publish(listOf(
            OverlayBox(0.38f, 0.22f, 0.62f, 0.78f, "TEST", 12f),
            OverlayBox(0.08f, 0.35f, 0.20f, 0.80f, "TEST-2", 31f)
        ))
        val intent = Intent(this, EspOverlayService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
            toast("Overlay started")
        } catch (e: Exception) {
            toast("Start failed: ${e.javaClass.simpleName}")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
