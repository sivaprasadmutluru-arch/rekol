package com.rekol.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Rekol reminder"
        val notes = intent.getStringExtra("notes").orEmpty().ifBlank { "Your scheduled reminder is due." }
        RekolNotificationHelper.show(context, title, notes)
    }
}
