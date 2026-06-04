package com.jarvis.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.forEach { sms ->
            val from = sms.originatingAddress ?: "unknown"
            val body = sms.messageBody ?: ""
            // Notifica al servidor local para que el agente lo procese
            ApiServer.notifySms(from, body)
        }
    }
}
