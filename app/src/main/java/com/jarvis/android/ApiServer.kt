package com.jarvis.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class ApiServer : Service() {

    private val PORT = 8080
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private val smsQueue = mutableListOf<JSONObject>()

        fun notifySms(from: String, body: String) {
            val sms = JSONObject()
            sms.put("from", from)
            sms.put("body", body)
            sms.put("timestamp", System.currentTimeMillis())
            smsQueue.add(sms)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // startForeground PRIMERO antes de cualquier otra cosa
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Iniciar servidor después de startForeground
        if (serverJob == null || !serverJob!!.isActive) {
            startServer()
        }
        return START_STICKY
    }

    private fun startServer() {
        serverJob = scope.launch {
            try {
                val server = ServerSocket(PORT)
                while (isActive) {
                    try {
                        val client = server.accept()
                        launch { handleClient(client) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val path = parts[1]

            var contentLength = 0
            var line = reader.readLine()
            while (line != null && line.isNotEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    contentLength = line.split(":")[1].trim().toInt()
                }
                line = reader.readLine()
            }

            val body = if (contentLength > 0) {
                val chars = CharArray(contentLength)
                reader.read(chars)
                String(chars)
            } else ""

            val response = route(method, path, body)
            sendResponse(writer, response)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket.close()
        }
    }

    private fun route(method: String, path: String, body: String): JSONObject {
        val acc = JarvisAccessibilityService.instance
        return when {
            method == "GET" && path == "/screen" -> {
                acc?.getScreenContent() ?: JSONObject().put("error", "accessibility_not_enabled")
            }
            method == "POST" && path == "/tap" -> {
                val json = JSONObject(body)
                acc?.tap(json.getDouble("x").toFloat(), json.getDouble("y").toFloat())
                JSONObject().put("ok", true)
            }
            method == "POST" && path == "/tap_text" -> {
                val json = JSONObject(body)
                val found = acc?.tapByText(json.getString("text")) ?: false
                JSONObject().put("ok", found)
            }
            method == "POST" && path == "/swipe" -> {
                val json = JSONObject(body)
                acc?.swipe(
                    json.getDouble("x1").toFloat(),
                    json.getDouble("y1").toFloat(),
                    json.getDouble("x2").toFloat(),
                    json.getDouble("y2").toFloat()
                )
                JSONObject().put("ok", true)
            }
            method == "POST" && path == "/type" -> {
                val json = JSONObject(body)
                acc?.typeText(json.getString("text"))
                JSONObject().put("ok", true)
            }
            method == "POST" && path == "/key" -> {
                val json = JSONObject(body)
                when (json.getString("key")) {
                    "back" -> acc?.pressBack()
                    "home" -> acc?.pressHome()
                    "recents" -> acc?.pressRecents()
                }
                JSONObject().put("ok", true)
            }
            method == "GET" && path == "/sms" -> {
                val result = JSONObject()
                result.put("messages", smsQueue.toList())
                smsQueue.clear()
                result
            }
            method == "GET" && path == "/ping" -> {
                JSONObject().put("status", "alive").put("port", PORT)
            }
            else -> JSONObject().put("error", "unknown_route")
        }
    }

    private fun sendResponse(writer: PrintWriter, json: JSONObject) {
        val body = json.toString()
        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: application/json")
        writer.println("Content-Length: ${body.length}")
        writer.println("Connection: close")
        writer.println()
        writer.println(body)
    }

    private fun buildNotification(): Notification {
        val channelId = "jarvis_service"
        val channel = NotificationChannel(
            channelId, "Jarvis Service",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("Jarvis activo")
            .setContentText("Servidor en puerto $PORT")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serverJob?.cancel()
        super.onDestroy()
    }
}
