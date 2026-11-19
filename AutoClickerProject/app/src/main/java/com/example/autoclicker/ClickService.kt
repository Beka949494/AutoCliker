package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.view.View
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.EditText
import android.widget.Button
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ClickService : AccessibilityService() {

    private lateinit var wm: WindowManager
    private var panel: View? = null
    private var running = false
    private var lastSignature = ""
    private var lastTap = 0L

    private val prefs by lazy {
        getSharedPreferences("autoclicker", MODE_PRIVATE)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
    }

    private fun bg(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun showPanel() {
        if (panel != null) return

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(7), dp(8), dp(7))
            background = bg(Color.rgb(17, 24, 39), 14)
        }

        val header = TextView(this).apply {
            text = "🚗 AutoClicker   ⋮"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(2), dp(3), dp(5))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val price = EditText(this).apply {
            setText(prefs.getLong("min_price", 1L).toString())
            setTextColor(Color.WHITE)
            textSize = 12f
            hint = "₸"
            setSingleLine(true)
            inputType = 2
            background = bg(Color.rgb(31, 41, 55), 8)
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }

        val save = Button(this).apply {
            text = "SAVE"
            textSize = 10f
            setTextColor(Color.WHITE)
            background = bg(Color.rgb(37, 99, 235), 8)
            setPadding(dp(4), 0, dp(4), 0)
        }

        row.addView(price, LinearLayout.LayoutParams(0, dp(34), 1f))

        row.addView(save, LinearLayout.LayoutParams(dp(54), dp(34)).apply {
            leftMargin = dp(5)
        })

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val start = Button(this).apply {
            text = "START"
            textSize = 10f
            setTextColor(Color.WHITE)
            background = bg(Color.rgb(22, 163, 74), 8)
        }

        val stop = Button(this).apply {
            text = "STOP"
            textSize = 10f
            setTextColor(Color.WHITE)
            background = bg(Color.rgb(220, 38, 38), 8)
        }

        buttons.addView(start, LinearLayout.LayoutParams(0, dp(34), 1f))
        buttons.addView(stop, LinearLayout.LayoutParams(0, dp(34), 1f).apply {
            leftMargin = dp(5)
        })

        val status = TextView(this).apply {
            text = if (prefs.getBoolean("running", false)) "● ON" else "● OFF"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(4), 0, 0)
        }

        outer.addView(header)
        outer.addView(row)
        outer.addView(buttons)
        outer.addView(status)

        save.setOnClickListener {
            val value = price.text.toString().toLongOrNull()
            if (value != null && value >= 0) {
                prefs.edit().putLong("min_price", value).apply()
                status.text = "● Баға сақталды"
            } else {
                status.text = "● Бағаны тексер"
            }
        }

        start.setOnClickListener {
            prefs.edit().putBoolean("running", true).apply()
            running = true
            lastSignature = ""
            status.text = "● ON"
            status.setTextColor(Color.GREEN)
        }

        stop.setOnClickListener {
            prefs.edit().putBoolean("running", false).apply()
            running = false
            status.text = "● OFF"
            status.setTextColor(Color.RED)
        }

        val params = WindowManager.LayoutParams(
            dp(190),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(10)
            y = dp(100)
        }

        // Панельді header арқылы жылжыту
        header.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0
            var startY = 0
            var touchX = 0f
            var touchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        params.x = startX + (event.rawX - touchX).toInt()
                        params.y = startY + (event.rawY - touchY).toInt()
                        wm.updateViewLayout(outer, params)
                        return true
                    }
                }
                return true
            }
        })

        panel = outer
        wm.addView(outer, params)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!prefs.getBoolean("running", false)) return

        val pkg = event.packageName?.toString() ?: return

        if (!pkg.contains("indrive", true) &&
            !pkg.contains("indriver", true)) return

        val root = rootInActiveWindow ?: return
        val orders = mutableListOf<Order>()

        collectOrders(root, orders)

        val topOrder = orders
            .filter { it.price >= prefs.getLong("min_price", 1L) }
            .minByOrNull { it.bounds.top } ?: return

        val signature = "${topOrder.text}|${topOrder.price}|${topOrder.bounds.top}"

        if (signature == lastSignature) return

        val now = System.currentTimeMillis()
        if (now - lastTap < 1200) return

        lastSignature = signature
        lastTap = now

        tapCard(topOrder.bounds)
    }

    data class Order(
        val text: String,
        val price: Long,
        val bounds: Rect
    )

    private fun collectOrders(
        node: AccessibilityNodeInfo,
        orders: MutableList<Order>
    ) {
        val text = node.text?.toString()
            ?: node.contentDescription?.toString()
            ?: ""

        val price = readPrice(text)

        if (price != null) {
            val card = findCard(node)
            val bounds = Rect()
            card.getBoundsInScreen(bounds)

            if (bounds.width() > dp(150) && bounds.height() > dp(60)) {
                orders.add(
                    Order(
                        getNodeText(card),
                        price,
                        bounds
                    )
                )
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectOrders(child, orders)
        }
    }

    private fun findCard(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var current = node
        var best = node

        repeat(8) {
            val parent = current.parent ?: return best
            val bounds = Rect()
            parent.getBoundsInScreen(bounds)

            if (bounds.width() >= resources.displayMetrics.widthPixels * 0.70 &&
                bounds.height() > dp(60) &&
                bounds.height() < dp(500)
            ) {
                best = parent
            }

            current = parent
        }

        return best
    }

    private fun readPrice(text: String): Long? {
        val regex = Regex(
            """(\d[\d\s\u00A0]*)\s*(₸|тг|тенге)""",
            RegexOption.IGNORE_CASE
        )

        val match = regex.find(text) ?: return null

        return match.groupValues[1]
            .replace(" ", "")
            .replace("\u00A0", "")
            .toLongOrNull()
    }

    private fun getNodeText(node: AccessibilityNodeInfo): String {
        val result = StringBuilder()

        node.text?.let { result.append(it).append(" ") }
        node.contentDescription?.let { result.append(it).append(" ") }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            result.append(getNodeText(child))
        }

        return result.toString().trim()
    }

    private fun tapCard(bounds: Rect) {
        if (bounds.isEmpty) return

        val x = bounds.exactCenterX()
        val y = bounds.exactCenterY()

        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0,
                    100
                )
            )
            .build()

        dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        panel?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        panel = null
        super.onDestroy()
    }
}
