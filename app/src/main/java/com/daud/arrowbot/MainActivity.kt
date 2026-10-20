package com.daud.arrowbot

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        box.addView(TextView(this).apply {
            textSize = 18f
            text = "1. نیچے بٹن دبائیں\n2. ArrowBot تلاش کر کے چالو کریں\n3. گیم کھولیں، اسکرین پر دو بٹن نظر آئیں گے"
        })
        box.addView(Button(this).apply {
            text = "سیٹنگز کھولیں"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        setContentView(box)
    }
}
