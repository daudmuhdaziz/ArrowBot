package com.daud.arrowbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class ArrowService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var panel: LinearLayout? = null
    private var status: TextView? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val p = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        p.addView(Button(this).apply {
            text = "Screenshot"
            textSize = 12f
            setOnClickListener { shot() }
        })
        p.addView(Button(this).apply {
            text = "Up"
            textSize = 12f
            setOnClickListener { doSwipe(0f, -1f) }
        })
        p.addView(Button(this).apply {
            text = "Down"
            textSize = 12f
            setOnClickListener { doSwipe(0f, 1f) }
        })
        p.addView(Button(this).apply {
            text = "Left"
            textSize = 12f
            setOnClickListener { doSwipe(-1f, 0f) }
        })
        p.addView(Button(this).apply {
            text = "Right"
            textSize = 12f
            setOnClickListener { doSwipe(1f, 0f) }
        })

        val st = TextView(this).apply {
            textSize = 11f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xCC000000.toInt())
            setPadding(12, 8, 12, 8)
            text = "Ready"
        }
        p.addView(st)
        status = st

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.END
        lp.y = 200
        wm.addView(p, lp)
        panel = p
    }

    override fun onDestroy() {
        panel?.let { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) }
        super.onDestroy()
    }

    private fun say(t: String) {
        handler.post { status?.text = t }
    }

    private fun shot() {
        say("Taking screenshot...")
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(r: ScreenshotResult) {
                        try {
                            val hw = Bitmap.wrapHardwareBuffer(r.hardwareBuffer, r.colorSpace)
                            val bmp = hw?.copy(Bitmap.Config.ARGB_8888, false)
                            r.hardwareBuffer.close()
                            if (bmp == null) {
                                say("Screenshot was empty")
                                return
                            }
                            val cv = ContentValues().apply {
                                put(MediaStore.Images.Media.DISPLAY_NAME, "arrow_${System.currentTimeMillis()}.png")
                                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ArrowBot")
                            }
                            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                            if (uri == null) {
                                say("Could not create file")
                                return
                            }
                            contentResolver.openOutputStream(uri)?.use {
                                bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                            }
                            say("Saved: ${bmp.width} x ${bmp.height}")
                        } catch (e: Exception) {
                            say("Error: ${e.javaClass.simpleName}")
                        }
                    }
                    override fun onFailure(code: Int) {
                        say("Failed, code: $code")
                    }
                })
        } catch (e: Exception) {
            say("Error: ${e.javaClass.simpleName}")
        }
    }

    // مضبوط سوائپ: تقریباً پوری اسکرین کی دوری، 900ms
    private fun doSwipe(dirX: Float, dirY: Float) {
        val m = resources.displayMetrics
        val w = m.widthPixels.toFloat()
        val h = m.heightPixels.toFloat()

        // افقی کے لیے 80% چوڑائی، عمودی کے لیے 60% اونچائی
        val distX = w * 0.80f * dirX
        val distY = h * 0.60f * dirY

        val cx = w / 2f
        val cy = h / 2f

        val path = Path().apply {
            moveTo(cx - distX / 2f, cy - distY / 2f)
            lineTo(cx + distX / 2f, cy + distY / 2f)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 900))
            .build()

        val dirName = when {
            dirY < 0 -> "Up"
            dirY > 0 -> "Down"
            dirX < 0 -> "Left"
            else -> "Right"
        }
        say("Scrolling: $dirName")
        dispatchGesture(gesture, null, null)

        handler.postDelayed({ say("Ready") }, 1100)
    }

    fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50)).build()
        dispatchGesture(g, null, null)
    }
}
