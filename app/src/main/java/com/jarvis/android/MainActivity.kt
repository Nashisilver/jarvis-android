package com.jarvis.android

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UI simple programática
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 120, 60, 60)
        }

        val title = TextView(this).apply {
            text = "🤖 Jarvis"
            textSize = 32f
            setPadding(0, 0, 0, 40)
        }

        val status = TextView(this).apply {
            text = if (isServiceRunning()) "● Servidor activo en :8080" else "○ Servidor inactivo"
            textSize = 16f
            setPadding(0, 0, 0, 40)
        }

        val btnStart = Button(this).apply {
            text = "Iniciar Jarvis"
            setOnClickListener {
                startForegroundService(Intent(this@MainActivity, ApiServer::class.java))
                status.text = "● Iniciando servidor..."
            }
        }

        val btnAccess = Button(this).apply {
            text = "Activar Accesibilidad"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
        layout.addView(status)
        layout.addView(btnStart)
        layout.addView(btnAccess)
        layout.addView(btnPerms)
        setContentView(layout)
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(android.app.ActivityManager::class.java)
        return manager.getRunningServices(10).any {
            it.service.className == ApiServer::class.java.name
        }
    }
}
