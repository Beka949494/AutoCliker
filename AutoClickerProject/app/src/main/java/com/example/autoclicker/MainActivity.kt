package com.example.autoclicker

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32,32,32,32)
        }
        val title = TextView(this).apply { text = "Қарапайым автокликер"; textSize = 22f; gravity = Gravity.CENTER }
        val note = TextView(this).apply {
            text = "Бұл құрал экрандағы бекітілген нүктені қайталап басады.\nҚолдану жауапкершілігі өзіңізде."
            textSize = 16f; gravity = Gravity.CENTER
        }
        val intervalLabel = TextView(this).apply { text = "Басу аралығы (мс):"; textSize = 16f }
        val interval = EditText(this).apply { setText("1000"); inputType = 2 }
        val enable = Button(this).apply { text = "Accessibility рұқсатын ашу" }
        enable.setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        val instructions = TextView(this).apply {
            text = "\nРұқсатты қосқаннан кейін қызмет экрандағы нүктені басуға мүмкіндік береді.\nБұл бастапқы жоба автоматты түрде InDrive тапсырыстарын қабылдамайды."
            textSize = 14f
        }
        layout.addView(title); layout.addView(note); layout.addView(intervalLabel); layout.addView(interval)
        layout.addView(enable); layout.addView(instructions)
        setContentView(layout)
    }
}