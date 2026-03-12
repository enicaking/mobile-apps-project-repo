package com.example.pearpressure.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationUtils {
    const val EXAM_CHANNEL_ID = "exam_reminders"

    fun createExamReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            EXAM_CHANNEL_ID,
            "Exam reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications shown 1 day before an exam."
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}