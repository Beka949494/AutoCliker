package com.example.autoclicker

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("autoclicker", MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "inDrive AutoClicker"
            textSize = 25f
            gravity = Gravity.CENTER
        }

        val label = TextView(this).apply {
            text = "Ең төменгі баға (₸)"
            textSize = 18f
        }

        val price = EditText(this).apply {
            setText(prefs.getLong("min_price", 1L).toString())
            inputType = 2
        }

        val start = Button(this).apply {
            text = "START"
        }

        val stop = Button(this).apply {
            text = "STOP"
        }

        val accessibility = Button(this).apply {
            text = "Accessibility рұқсатын ашу"
        }

        val status = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
            text = if (prefs.getBoolean("running", false))
                "Күйі: қосулы"
            else
                "Күйі: тоқтап тұр"
        }

        start.setOnClickListener {
            val minPrice = price.text.toString().toLongOrNull()

            if (minPrice == null || minPrice < 1) {
                Toast.makeText(
                    this,
                    "Бағаны дұрыс енгізіңіз",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putLong("min_price", minPrice)
                .putBoolean("running", true)
                .apply()

            status.text = "Күйі: қосулы"
        }

        stop.setOnClickListener {
            prefs.edit()
                .putBoolean("running", false)
                .apply()

            status.text = "Күйі: тоқтап тұр"
        }

        accessibility.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        }

        layout.addView(title)
        layout.addView(label)
        layout.addView(price)
        layout.addView(start)
        layout.addView(stop)
        layout.addView(accessibility)
        layout.addView(status)

        setContentView(layout)
    }
}
