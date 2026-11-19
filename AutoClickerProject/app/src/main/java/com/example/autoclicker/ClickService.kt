
package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.View
import android.widget.*
import android.text.InputType

class ClickService : AccessibilityService() {

    private var previousTopSignature = ""
    private var lastClickTime = 0L
    private var processing = false

    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null
    private lateinit var priceInput: EditText
    private lateinit var statusText: TextView

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
    }

    private fun showPanel() {
        if (panel != null) return

        val prefs = getSharedPreferences("autoclicker", MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 20f
                setStroke(2, Color.GRAY)
            }
        }

        val title = TextView(this).apply {
            text = "inDrive AutoClicker"
            textSize = 18f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        val label = TextView(this).apply {
            text = "Ең төменгі баға (₸)"
            textSize = 14f
            setTextColor(Color.BLACK)
        }

        priceInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setText(prefs.getLong("min_price", 1L).toString())
            textSize = 18f
        }

        val saveButton = Button(this).apply {
            text = "САҚТАУ"
        }

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val startButton = Button(this).apply {
            text = "START"
        }

        val stopButton = Button(this).apply {
            text = "STOP"
        }

        statusText = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            text = if (prefs.getBoolean("running", false))
                "Күйі: ҚОСУЛЫ"
            else
                "Күйі: ТОҚТАП ТҰР"
        }

        saveButton.setOnClickListener {
            val value = priceInput.text.toString().toLongOrNull()

            if (value == null || value < 1) {
                Toast.makeText(
                    this,
                    "Бағаны дұрыс енгізіңіз",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                prefs.edit().putLong("min_price", value).apply()
                Toast.makeText(
                    this,
                    "Баға сақталды: $value ₸",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        startButton.setOnClickListener {
            val value = priceInput.text.toString().toLongOrNull()

            if (value == null || value < 1) {
                Toast.makeText(this, "Алдымен бағаны енгізіңіз", Toast.LENGTH_SHORT).show()
            } else {
                prefs.edit()
                    .putLong("min_price", value)
                    .putBoolean("running", true)
                    .apply()

                statusText.text = "Күйі: ҚОСУЛЫ"
            }
        }

        stopButton.setOnClickListener {
            prefs.edit().putBoolean("running", false).apply()
            statusText.text = "Күйі: ТОҚТАП ТҰР"
        }

        buttons.addView(startButton)
        buttons.addView(stopButton)

        layout.addView(title)
        layout.addView(label)
        layout.addView(priceInput)
        layout.addView(saveButton)
        layout.addView(buttons)
        layout.addView(statusText)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 15
            y = 100
        }

        try {
            windowManager.addView(layout, params)
            panel = layout
        } catch (e: Exception) {
            Toast.makeText(this, "Панельді ашу мүмкін болмады", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || processing) return

        val prefs = getSharedPreferences("autoclicker", MODE_PRIVATE)
        if (!prefs.getBoolean("running", false)) return

        val packageName = event.packageName?.toString() ?: return

        if (!packageName.contains("indrive", true) &&
            !packageName.contains("indriver", true)) return

        val root = rootInActiveWindow ?: return
        val orders = mutableListOf<Order>()

        collectOrders(root, orders)

        val topOrder = orders.minByOrNull { it.top } ?: run {
            previousTopSignature = ""
            return
        }

        if (topOrder.signature == previousTopSignature) return

        previousTopSignature = topOrder.signature

        if (topOrder.price < prefs.getLong("min_price", 1L)) return

        if (System.currentTimeMillis() - lastClickTime < 500) return

        lastClickTime = System.currentTimeMillis()
        processing = true

        tapCard(topOrder.card) {
            processing = false
        }
    }

    data class Order(
        val price: Long,
        val top: Int,
        val signature: String,
        val card: AccessibilityNodeInfo
    )

    private fun collectOrders(
        node: AccessibilityNodeInfo,
        result: MutableList<Order>
    ) {
        val text = node.text?.toString()
            ?: node.contentDescription?.toString()
            ?: ""

        val price = readPrice(text)

        if (price != null) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val card = findCard(node)
            val cardBounds = Rect()
            card.getBoundsInScreen(cardBounds)

            if (!cardBounds.isEmpty) {
                result.add(
                    Order(
                        price,
                        cardBounds.top,
                        "${cardBounds.top}|${cardBounds.bottom}|$price",
                        AccessibilityNodeInfo.obtain(card)
                    )
                )
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectOrders(child, result)
        }
    }

    private fun readPrice(text: String): Long? {
        val regex = Regex(
            """(\d[\d\s\u00A0]*)\s*(₸|тг\.?|тенге)""",
            RegexOption.IGNORE_CASE
        )

        val match = regex.find(text) ?: return null

        return match.groupValues[1]
            .replace(" ", "")
            .replace("\u00A0", "")
            .toLongOrNull()
    }

    private fun findCard(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo {
        var current = node
        var best = node
        var depth = 0

        while (depth < 10) {
            val bounds = Rect()
            current.getBoundsInScreen(bounds)

            val screenWidth = resources.displayMetrics.widthPixels

            if (bounds.width() >= screenWidth * 0.65 &&
                bounds.height() >= 70 * resources.displayMetrics.density
            ) {
                best = current
            }

            val parent = current.parent ?: break
            current = parent
            depth++
        }

        return best
    }

    private fun tapCard(
        card: AccessibilityNodeInfo,
        onFinished: () -> Unit
    ) {
        if (card.isClickable &&
            card.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        ) {
            onFinished()
            return
        }

        val bounds = Rect()
        card.getBoundsInScreen(bounds)

        if (bounds.isEmpty) {
            onFinished()
            return
        }

        val path = Path().apply {
            moveTo(bounds.exactCenterX(), bounds.exactCenterY())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()

        dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(
                    gestureDescription: GestureDescription?
                ) {
                    onFinished()
                }

                override fun onCancelled(
                    gestureDescription: GestureDescription?
                ) {
                    onFinished()
                }
            },
            null
        )
    }

    override fun onInterrupt() {
        processing = false
    }

    override fun onDestroy() {
        try {
            panel?.let { windowManager.removeView(it) }
        } catch (_: Exception) {
        }
        panel = null
        super.onDestroy()
    }
}
