package com.example.pearpressure.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
object NotificationUtils {

    const val STANDARD_CHANNEL_ID = "standard_notifications"
    const val STANDARD_CHANNEL_NAME = "Standard notifications"
    const val STANDARD_CHANNEL_DESCRIPTION = "Standard app notifications"
    const val HEADS_UP_CHANNEL_ID = "heads_up_notifications_v2"
    const val HEADS_UP_CHANNEL_NAME = "Heads-up notifications"
    const val HEADS_UP_CHANNEL_DESCRIPTION = "Important notifications shown as pop-up"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val standardChannel = NotificationChannel(
                STANDARD_CHANNEL_ID,
                STANDARD_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = STANDARD_CHANNEL_DESCRIPTION
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }

            val headsUpChannel = NotificationChannel(
                HEADS_UP_CHANNEL_ID,
                HEADS_UP_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = HEADS_UP_CHANNEL_DESCRIPTION
                setShowBadge(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(standardChannel)
            notificationManager.createNotificationChannel(headsUpChannel)
        }
    }
}