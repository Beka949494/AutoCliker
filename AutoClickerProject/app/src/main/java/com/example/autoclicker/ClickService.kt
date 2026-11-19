package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ClickService : AccessibilityService() {

    private val clickedOrders = mutableSetOf<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val prefs = getSharedPreferences("autoclicker", MODE_PRIVATE)

        if (!prefs.getBoolean("running", false)) return

        val packageName = event.packageName?.toString() ?: return

        // Тек inDrive экраны
        if (!packageName.contains("indrive", ignoreCase = true) &&
            !packageName.contains("indriver", ignoreCase = true)
        ) return

        val root = rootInActiveWindow ?: return

        findOrder(root, prefs.getLong("min_price", 1L))
    }

    private fun findOrder(
        root: AccessibilityNodeInfo,
        minPrice: Long
    ) {
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue

            val text = child.text?.toString()
                ?: child.contentDescription?.toString()
                ?: ""

            val price = readPrice(text)

            if (price != null && price >= minPrice) {
                val orderText = getNodeText(child)
                val signature = "$orderText|$price"

                if (!clickedOrders.contains(signature)) {
                    if (tapOrder(child)) {
                        clickedOrders.add(signature)
                    }
                }
            }

            findOrder(child, minPrice)
        }
    }

    private fun readPrice(text: String): Long? {
        val regex = Regex("""(\d[\d\s\u00A0]*)\s*(₸|тг|тенге)""",
            RegexOption.IGNORE_CASE)

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

    private fun tapOrder(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node

        // Алдымен карточканың басылатын элементін табамыз
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }
            current = current.parent
        }

        // Басылатын элемент табылмаса, баға тұрған жерді басамыз
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        if (bounds.isEmpty) return false

        val path = Path().apply {
            moveTo(bounds.exactCenterX(), bounds.exactCenterY())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(path, 0, 50)
            )
            .build()

        return dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() {
    }
}
