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
import android.util.DisplayMetrics
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
    private var params: WindowManager.LayoutParams? = null
    private var panelOn = false
    private var busy = false
    private var problem = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }
        box.addView(smallButton("Screenshot") {
            if (busy) {
                say("Wait...")
                return@smallButton
            }
            busy = true
            hidePanel()
            handler.postDelayed({
                shot("single") {
                    showPanel()
                    busy = false
                }
            }, 300)
        })
        box.addView(smallButton("Up") { doSwipe(0f, -1f, "up") })
        box.addView(smallButton("Down") { doSwipe(0f, 1f, "down") })
        box.addView(smallButton("Left") { doSwipe(-1f, 0f, "left") })
        box.addView(smallButton("Right") { doSwipe(1f, 0f, "right") })
        box.addView(smallButton("Capture Board") { captureBoard() })
        val st = TextView(this).apply {
            textSize = 11f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xCC000000.toInt())
            setPadding(12, 8, 12, 8)
            text = "Ready v5"
        }
        box.addView(st)
        status = st
        panel = box
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            x = 8
            y = 40
        }
        showPanel()
    }

    private fun smallButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setOnClickListener { onClick() }
        }
    }

    override fun onDestroy() {
        hidePanel()
        super.onDestroy()
    }

    private fun wm(): WindowManager = getSystemService(WINDOW_SERVICE) as WindowManager

    private fun showPanel() {
        val p = panel ?: return
        val lp = params ?: return
        if (panelOn) return
        try {
            wm().addView(p, lp)
            panelOn = true
        } catch (e: Exception) {
        }
    }

    private fun hidePanel() {
        val p = panel ?: return
        if (!panelOn) return
        try {
            wm().removeView(p)
        } catch (e: Exception) {
        }
        panelOn = false
    }

    private fun say(t: String) {
        handler.post { status?.text = t }
    }

    @Suppress("DEPRECATION")
    private fun screen(): Pair<Float, Float> {
        val metrics = DisplayMetrics()
        wm().defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels.toFloat() to metrics.heightPixels.toFloat()
    }

    private fun shot(name: String, after: () -> Unit) {
        say("Taking " + name)
        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(r: ScreenshotResult) {
                        var bmp: Bitmap? = null
                        try {
                            val hw = Bitmap.wrapHardwareBuffer(r.hardwareBuffer, r.colorSpace)
                            bmp = hw?.copy(Bitmap.Config.ARGB_8888, false)
                            if (bmp == null) {
                                say(name + " empty")
                                return
                            }
                            val cv = ContentValues().apply {
                                put(
                                    MediaStore.Images.Media.DISPLAY_NAME,
                                    "board_" + name + "_" + System.currentTimeMillis() + ".png"
                                )
                                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ArrowBot")
                            }
                            val uri = contentResolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                cv
                            )
                            if (uri == null) {
                                say(name + " file failed")
                                return
                            }
                            val picture = bmp
                            contentResolver.openOutputStream(uri)?.use { out ->
                                picture.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            say("Saved " + name)
                        } catch (e: Exception) {
                            say("Error " + e.javaClass.simpleName)
                        } finally {
                            try {
                                r.hardwareBuffer.close()
                            } catch (e: Exception) {
                            }
                            try {
                                bmp?.recycle()
                            } catch (e: Exception) {
                            }
                            handler.postDelayed(after, 1000)
                        }
                    }

                    override fun onFailure(code: Int) {
                        say("Shot " + name + " failed " + code)
                        handler.postDelayed(after, 1000)
                    }
                }
            )
        } catch (e: Exception) {
            say("Error " + e.javaClass.simpleName)
            handler.postDelayed(after, 500)
        }
    }

    private fun swipe(dirX: Float, dirY: Float, after: (Boolean) -> Unit) {
        val (w, h) = screen()
        val x1: Float
        val y1: Float
        val x2: Float
        val y2: Float
        if (dirX < 0f) {
            x1 = w * 0.78f
            y1 = h * 0.52f
            x2 = w * 0.22f
            y2 = h * 0.52f + 24f
        } else if (dirX > 0f) {
            x1 = w * 0.22f
            y1 = h * 0.52f
            x2 = w * 0.78f
            y2 = h * 0.52f + 24f
        } else if (dirY < 0f) {
            x1 = w * 0.50f
            y1 = h * 0.75f
            x2 = w * 0.50f + 24f
            y2 = h * 0.30f
        } else {
            x1 = w * 0.50f
            y1 = h * 0.30f
            x2 = w * 0.50f + 24f
            y2 = h * 0.75f
        }
        val path = Path()
        val steps = 24
        path.moveTo(x1, y1)
        var i = 1
        while (i <= steps) {
            val t = i.toFloat() / steps.toFloat()
            path.lineTo(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t)
            i = i + 1
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1000))
            .build()
        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    after(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    after(false)
                }
            },
            handler
        )
        if (!accepted) after(false)
    }

    private fun doSwipe(dirX: Float, dirY: Float, label: String) {
        if (busy) {
            say("Wait...")
            return
        }
        busy = true
        hidePanel()
        say("Move " + label)
        swipe(dirX, dirY) { ok ->
            showPanel()
            if (ok) say("Move " + label + " done") else say("Move " + label + " cancelled")
            busy = false
        }
    }

    private fun captureBoard() {
        if (busy) {
            say("Wait...")
            return
        }
        busy = true
        problem = ""
        hidePanel()
        val jobs = ArrayDeque<() -> Unit>()
        fun next() {
            val job = jobs.removeFirstOrNull()
            if (job == null) {
                showPanel()
                busy = false
                if (problem == "") say("Capture done") else say(problem)
                return
            }
            job()
        }
        fun addSwipe(dx: Float, dy: Float, label: String) {
            jobs.add {
                say("Move " + label)
                swipe(dx, dy) { ok ->
                    if (!ok && problem == "") problem = "Move " + label + " cancelled"
                    handler.postDelayed({ next() }, 700)
                }
            }
        }
        fun addShot(name: String) {
            jobs.add {
                shot(name) { next() }
            }
        }
        addShot("center")
        addSwipe(-1f, 0f, "left")
        addShot("left1")
        addSwipe(-1f, 0f, "left")
        addShot("left2")
        addSwipe(1f, 0f, "right")
        addSwipe(1f, 0f, "right")
        addSwipe(1f, 0f, "right")
        addShot("right1")
        addSwipe(1f, 0f, "right")
        addShot("right2")
        addSwipe(-1f, 0f, "left")
        addSwipe(-1f, 0f, "left")
        addSwipe(0f, -1f, "up")
        addShot("up1")
        addSwipe(0f, -1f, "up")
        addShot("up2")
        addSwipe(0f, 1f, "down")
        addSwipe(0f, 1f, "down")
        addSwipe(0f, 1f, "down")
        addShot("down1")
        addSwipe(0f, 1f, "down")
        addShot("down2")
        handler.postDelayed({ next() }, 300)
    }

    fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50)).build()
        dispatchGesture(g, null, null)
    }
}
