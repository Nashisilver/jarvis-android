package com.jarvis.android

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        var instance: JarvisAccessibilityService? = null
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun getScreenContent(): JSONObject {
    return try {
        val root = rootInActiveWindow ?: return JSONObject("{\"elements\":[],\"pkg\":\"none\"}")
        val sb = StringBuilder("[")
        var first = true
        fun traverse(node: AccessibilityNodeInfo?) {
            node ?: return
            val text = node.text?.toString()?.replace("\"","'") ?: ""
            val desc = node.contentDescription?.toString()?.replace("\"","'") ?: ""
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            if (text.isNotEmpty() || desc.isNotEmpty()) {
                if (!first) sb.append(",")
                sb.append("{\"text\":\"$text\",\"desc\":\"$desc\",\"clickable\":${node.isClickable},\"x\":${bounds.centerX()},\"y\":${bounds.centerY()}}")
                first = false
            }
            for (i in 0 until node.childCount) traverse(node.getChild(i))
        }
        traverse(root)
        sb.append("]")
        val pkg = root.packageName?.toString()?.replace("\"","") ?: "unknown"
        JSONObject("{\"elements\":$sb,\"pkg\":\"$pkg\"}")
    } catch (e: Exception) {
        JSONObject("{\"elements\":[],\"pkg\":\"error\"}")
    }
}

    }

    fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun tapByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        if (nodes.isNullOrEmpty()) return false
        val bounds = Rect()
        nodes[0].getBoundsInScreen(bounds)
        tap(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        return true
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 300) {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun typeText(text: String) {
        val node = findFocusedInput() ?: return
        val args = Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text
        )
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findFocusedInput(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
    }

    fun pressBack() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun pressHome() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun pressRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)
}
