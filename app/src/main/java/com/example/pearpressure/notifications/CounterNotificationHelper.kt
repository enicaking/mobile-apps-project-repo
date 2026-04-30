package com.example.pearpressure.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.pearpressure.MainActivity
import java.util.concurrent.TimeUnit

object CounterNotificationHelper {
    private const val WATER_REMINDER_WORK = "water_reminder_work"

    // TEST ONLY: 1 minute.
    // For final version, use 30L.
    private const val WATER_REMINDER_MINUTES = 1L

    fun scheduleWaterReminder(
        context: Context,
        subjectName: String,
        examTitle: String
    ) {
        val request = OneTimeWorkRequestBuilder<CounterReminderWorker>()
            .setInitialDelay(WATER_REMINDER_MINUTES, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    CounterReminderWorker.KEY_SUBJECT_NAME to subjectName,
                    CounterReminderWorker.KEY_EXAM_TITLE to examTitle
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WATER_REMINDER_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelWaterReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WATER_REMINDER_WORK)
    }

    fun showCoffeeWarning(context: Context, coffeeTotal: Int) {
        showImmediateCounterNotification(
            context = context,
            title = "Coffee warning",
            body = "You have already had $coffeeTotal coffees today. You won't sleep tonight!"
        )
    }

    fun showBoostWarning(context: Context, boostTotal: Int) {
        showImmediateCounterNotification(
            context = context,
            title = "Monster High warning",
            body = "You have already had $boostTotal energy drinks. Don't be a MONSTER HIGH!"
        )
    }

    private fun showImmediateCounterNotification(
        context: Context,
        title: String,
        body: String
    ) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        NotificationUtils.createChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val notificationId = NotificationIdFactory.nextId()

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationUtils.HEADS_UP_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setOnlyAlertOnce(false)
            .build()

        NotificationManagerCompat.from(context)
            .notify(notificationId, notification)
    }
}