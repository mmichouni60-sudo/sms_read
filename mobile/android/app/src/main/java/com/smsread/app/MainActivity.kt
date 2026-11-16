package com.smsread.app

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(TextView(this).apply {
            text = "SMS Read — آزمایشی\nبرای دریافت پیامک، اجازه SMS لازم است."
            textSize = 18f
        })
        layout.addView(Button(this).apply {
            text = "اجازه دریافت پیامک"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= 23)
                    requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), 100)
            }
        })
        setContentView(layout)
    }
}
