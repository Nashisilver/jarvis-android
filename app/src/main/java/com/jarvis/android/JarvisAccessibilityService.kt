package com.jarvis.android

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
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

    // Obtener todo el texto visible en pantalla
    fun getScreenContent(): JSONObject {
        val result = JSONObject()
        val elements = JSONArray()
        val root = rootInActiveWindow ?: return result.put("error", "no_window")

        fun traverse(node: AccessibilityNodeInfo?) {
            node ?: return
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            if (text.isNotEmpty() || desc.isNotEmpty()) {
                val el = JSONObject()
                el.put("text", text)
                el.put("desc", desc)
                el.put("clickable", node.isClickable)
                el.put("x", bounds.centerX())
                el.put("y", bounds.centerY())
                el.put("id", node.viewIdResourceName ?: "")
                elements.put(el)
            }
            for (i in 0 until node.childCount) traverse(node.getChild(i))
        }

        traverse(root)
        result.put("elements", elements)
        result.put("package", root.packageName ?: "")
        return result
    }

    // Tap en coordenadas
    fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    // Tap por texto visible
    fun tapByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        if (nodes.isNullOrEmpty()) return false
        val bounds = Rect()
        nodes[0].getBoundsInScreen(bounds)
        tap(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        return true
    }

    // Swipe
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

    // Escribir texto en campo activo
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

    // Botón atrás
    fun pressBack() = performGlobalAction(GLOBAL_ACTION_BACK)

    // Home
    fun pressHome() = performGlobalAction(GLOBAL_ACTION_HOME)

    // Recientes
    fun pressRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)
}
