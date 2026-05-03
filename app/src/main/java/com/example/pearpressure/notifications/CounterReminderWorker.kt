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

        val notificationId = NotificationIdFactory.nextId()

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationUtils.HEADS_UP_CHANNEL_ID
        )
            .setSmallIcon(com.example.pearpressure.R.mipmap.ic_launcher)
            .setContentTitle("Water reminder")
            .setContentText("You have not drunk water recently. Remember, it is vital to drink enough water!")            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setOnlyAlertOnce(false)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(notificationId, notification)

        return Result.success()
    }

    companion object {
        const val KEY_SUBJECT_NAME = "subject_name"
        const val KEY_EXAM_TITLE = "exam_title"
        const val WATER_REMINDER_NOTIFICATION_ID = 3001
    }
}