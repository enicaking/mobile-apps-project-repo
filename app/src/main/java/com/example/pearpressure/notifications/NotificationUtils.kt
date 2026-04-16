package com.example.pearpressure.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationUtils {
    const val EXAM_CHANNEL_ID = "exam_reminders"
    const val SOCIAL_CHANNEL_ID = "social_notifications"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val examChannel = NotificationChannel(
            EXAM_CHANNEL_ID,
            "Exam reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications shown before exams."
            setShowBadge(true)
        }

        val socialChannel = NotificationChannel(
            SOCIAL_CHANNEL_ID,
            "Social notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications when other users start studying."
            setShowBadge(true)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(examChannel)
        manager.createNotificationChannel(socialChannel)
    }
}