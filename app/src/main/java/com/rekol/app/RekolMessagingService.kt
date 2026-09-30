package com.rekol.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RekolMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        getSharedPreferences("rekol_profile", MODE_PRIVATE).edit().putString("fcm_token", token).apply()
    }
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Rekol"
        val body = message.notification?.body ?: message.data["body"] ?: "You have a new update."
        val channelId = "rekol_updates"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(channelId, "Rekol updates", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channelId).setSmallIcon(R.drawable.rekol_mobile_icon).setContentTitle(title).setContentText(body).setAutoCancel(true).setContentIntent(pending).build()
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}

object RekolNotificationHelper {
    fun show(context: android.content.Context, title: String, body: String) {
        val channelId = "rekol_updates"
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(channelId, "Rekol updates", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), NotificationCompat.Builder(context, channelId).setSmallIcon(R.drawable.rekol_mobile_icon).setContentTitle(title).setContentText(body).setAutoCancel(true).setContentIntent(pending).build())
    }
}
