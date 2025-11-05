package com.markdownbinder.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.markdownbinder.MainActivity
import com.markdownbinder.R

/**
 * NotificationHelper - Creating notifications for Foreground Service
 *
 * Required for Android 8.0+ (API 26+) when the service is running in foreground
 */
object NotificationHelper {

    private const val CHANNEL_ID = "overlay_service_channel"
    private const val CHANNEL_NAME = "Overlay Service"
    private const val NOTIFICATION_ID = 1001

    /**
     * Creating a NotificationChannel (for Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Notification for floating overlay service"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creating a notification for Foreground Service
     */
    fun createNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            pendingIntentFlags
        )

        val stopIntent = Intent(context, OverlayService::class.java).apply {
            action = "ACTION_STOP"
        }
        
        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            stopIntent,
            pendingIntentFlags
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("MarkDown Binder")
            .setContentText("Overlay is active. Tap to open app.")
            .setSmallIcon(R.drawable.ic_menu)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)
            .addAction(
                R.drawable.ic_close,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    /**
     * Getting Notification ID
     */
    fun getNotificationId(): Int = NOTIFICATION_ID
}
