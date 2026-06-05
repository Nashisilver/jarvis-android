package com.jarvis.android

import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private val PORT = 38080
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permitir networking en hilo principal (para pruebas)
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        )

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 120, 60, 60)
        }

        val title = TextView(this).apply {
            text = "🤖 Akira"
            textSize = 32f
            setPadding(0, 0, 0, 40)
        }

        statusText = TextView(this).apply {
            text = "○ Servidor inactivo"
            textSize = 16f
            setPadding(0, 0, 0, 40)
        }

        val btnStart = Button(this).apply {
            text = "Iniciar Servidor"
            setOnClickListener { startServer() }
        }

        val btnStop = Button(this).apply {
            text = "Detener Servidor"
            setOnClickListener { stopServer() }
        }

        val btnAccess = Button(this).apply {
            text = "Activar Accesibilidad"
            setOnClickListener {
                startActivity(android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val btnPerms = Button(this).apply {
            text = "Dar Permisos"
            setOnClickListener {
                requestPermissions(arrayOf(
                    android.Manifest.permission.READ_SMS,
                    android.Manifest.permission.RECEIVE_SMS,
                    android.Manifest.permission.SEND_SMS,
                    android.Manifest.permission.READ_CONTACTS,
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.READ_CALL_LOG,
                    android.Manifest.permission.CAMERA
                ), 1)
            }
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(btnStart)
        layout.addView(btnStop)
        layout.addView(btnAccess)
        layout.addView(btnPerms)
        setContentView(layout)
    }

    private fun startServer() {
        serverJob = scope.launch {
            try {
                val server = ServerSocket(PORT)
                runOnUiThread { statusText.text = "● Servidor activo en :$PORT" }
                while (isActive) {
                    val client = server.accept()
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                runOnUiThread { statusText.text = "✗ Error: ${e.message}" }
            }
        }
    }

    private fun stopServer() {
        serverJob?.cancel()
        statusText.text = "○ Servidor detenido"
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
            val responseBody = response.toString()
            writer.println("HTTP/1.1 200 OK")
            writer.println("Content-Type: application/json")
            writer.println("Content-Length: ${responseBody.length}")
            writer.println("Connection: close")
            writer.println()
            writer.println(responseBody)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket.close()
        }
    }

    private fun route(method: String, path: String, body: String): JSONObject {
        val acc = JarvisAccessibilityService.instance
        return when {
            method == "GET" && path == "/ping" ->
                JSONObject().put("status", "alive").put("port", PORT)
            method == "GET" && path == "/screen" ->
                acc?.getScreenContent() ?: JSONObject().put("error", "accessibility_off")
            method == "POST" && path == "/tap" -> {
                val j = JSONObject(body)
                acc?.tap(j.getDouble("x").toFloat(), j.getDouble("y").toFloat())
                JSONObject().put("ok", true)
            }
            method == "POST" && path == "/tap_text" -> {
                val j = JSONObject(body)
                val ok = acc?.tapByText(j.getString("text")) ?: false
                JSONObject().put("ok", ok)
            }
            method == "POST" && path == "/key" -> {
                val j = JSONObject(body)
                when (j.getString("key")) {
                    "back" -> acc?.pressBack()
                    "home" -> acc?.pressHome()
                    "recents" -> acc?.pressRecents()
                }
                JSONObject().put("ok", true)
            }
            method == "POST" && path == "/type" -> {
                val j = JSONObject(body)
                acc?.typeText(j.getString("text"))
                JSONObject().put("ok", true)
            }
            else -> JSONObject().put("error", "unknown_route")
        }
    }

    override fun onDestroy() {
        serverJob?.cancel()
        super.onDestroy()
    }
}
