
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

        val priceLabel = TextView(this).apply {
            text = "Ең төменгі баға (₸)"
            textSize = 18f
        }

        val price = EditText(this).apply {
            setText("1")
            inputType = 2
            hint = "Мысалы: 2000"
        }

        val start = Button(this).apply {
            text = "START"
        }

        val stop = Button(this).apply {
            text = "STOP"
            isEnabled = false
        }

        val accessibility = Button(this).apply {
            text = "Accessibility рұқсатын ашу"
        }

        val status = TextView(this).apply {
            text = "Күйі: тоқтап тұр"
            textSize = 16f
            gravity = Gravity.CENTER
        }

        accessibility.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        }

        start.setOnClickListener {
            val minPrice = price.text.toString().toLongOrNull()

            if (minPrice == null || minPrice < 1) {
                Toast.makeText(
                    this,
                    "Бағаны 1 теңгеден бастап енгіз",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            status.text = "Күйі: іске қосуға дайын"
            start.isEnabled = false
            stop.isEnabled = true
        }

        stop.setOnClickListener {
            status.text = "Күйі: тоқтап тұр"
            start.isEnabled = true
            stop.isEnabled = false
        }

        layout.addView(title)
        layout.addView(priceLabel)
        layout.addView(price)
        layout.addView(start)
        layout.addView(stop)
        layout.addView(accessibility)
        layout.addView(status)

        setContentView(layout)
    }
}
