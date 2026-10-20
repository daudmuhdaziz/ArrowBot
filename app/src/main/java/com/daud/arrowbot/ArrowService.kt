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
            text = "Scroll test"
            textSize = 12f
            setOnClickListener { swipeTest() }
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
            WindowManager.Lay
