package com.jarvis.android

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicia el servidor HTTP
        val serviceIntent = Intent(this, ApiServer::class.java)
        startForegroundService(serviceIntent)

        // Abre ajustes de accesibilidad si no está activado
        if (!isAccessibilityEnabled()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        finish()
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = "${packageName}/${JarvisAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(service)
    }
}
