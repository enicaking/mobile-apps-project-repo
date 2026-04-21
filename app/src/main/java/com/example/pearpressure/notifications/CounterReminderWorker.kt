package com.example.pearpressure.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pearpressure.MainActivity

class CounterReminderWorker(
    appContext: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return Result.success()
        }

        NotificationUtils.createChannels(applicationContext)

        val subjectName = inputData.getString(KEY_SUBJECT_NAME) ?: "this subject"
        val examTitle = inputData.getString(KEY_EXAM_TITLE) ?: "this exam"

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationUtils.COUNTER_CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Water reminder")
            .setContentText("You have not added water for 30 minutes in $subjectName • $examTitle")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(WATER_REMINDER_NOTIFICATION_ID, notification)

        return Result.success()
    }

    companion object {
        const val KEY_SUBJECT_NAME = "subject_name"
        const val KEY_EXAM_TITLE = "exam_title"
        const val WATER_REMINDER_NOTIFICATION_ID = 3001
    }
}