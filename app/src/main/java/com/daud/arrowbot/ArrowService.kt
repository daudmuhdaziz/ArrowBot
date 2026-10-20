package com.daud.arrowbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class ArrowService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var panel: LinearLayout? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val p = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        p.addView(Button(this).apply {
            text = "تصویر"
            setOnClickListener { shot() }
        })
        p.addView(Button(this).apply {
            text = "سرکاؤ"
            setOnClickListener { swipeTest() }
        })
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.END
        lp.y = 300
        wm.addView(p, lp)
        panel = p
    }

    override fun onDestroy() {
        panel?.let { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) }
        super.onDestroy()
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()

    private fun shot() {
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(r: ScreenshotResult) {
                    val hw = Bitmap.wrapHardwareBuffer(r.hardwareBuffer, r.colorSpace)
                    val bmp = hw?.copy(Bitmap.Config.ARGB_8888, false)
                    r.hardwareBuffer.close()
                    if (bmp == null) { toast("تصویر نہیں بنی"); return }
                    val f = File(getExternalFilesDir(null), "shot_${System.currentTimeMillis()}.png")
                    FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    toast("تصویر محفوظ: ${bmp.width}x${bmp.height}")
                }
                override fun onFailure(code: Int) { toast("ناکامی: $code") }
            })
    }

    private fun swipe(dx: Float, dy: Float) {
        val m = resources.displayMetrics
        val cx = m.widthPixels / 2f
        val cy = m.heightPixels / 2f
        val path = Path().apply { moveTo(cx, cy); lineTo(cx + dx, cy + dy) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 400)).build()
        dispatchGesture(g, null, null)
    }

    private fun swipeTest() {
        val d = 300f
        val moves = listOf(0f to -d, 0f to d, -d to 0f, d to 0f)
        moves.forEachIndexed { i, (dx, dy) ->
            handler.postDelayed({ swipe(dx, dy) }, i * 1200L)
        }
        toast("چار سمتوں میں سرکاؤں گا")
    }

    fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50)).build()
        dispatchGesture(g, null, null)
    }
}
