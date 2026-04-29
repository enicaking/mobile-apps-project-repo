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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pearpressure.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExamReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Android 13+ needs runtime permission
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return Result.success() // nothing to do
        }

        val examId = inputData.getString(KEY_EXAM_ID) ?: return Result.failure()
        val subjectName = inputData.getString(KEY_SUBJECT_NAME) ?: "Subject"
        val examTitle = inputData.getString(KEY_EXAM_TITLE) ?: "Exam"
        val endsAtMs = inputData.getLong(KEY_EXAM_ENDS_AT_MS, 0L)

        val reminderType = inputData.getString(KEY_REMINDER_TYPE) ?: TYPE_ONE_DAY_BEFORE

        NotificationUtils.createChannels(applicationContext)

        val endsAtText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(endsAtMs))

        val isExamFinished = reminderType == TYPE_EXAM_FINISHED

        val title = if (isExamFinished) {
            "Exam finished"
        } else {
            "Exam tomorrow: $examTitle"
        }

        val body = if (isExamFinished) {
            "Add your expected grade for $examTitle"
        } else {
            "$subjectName • Ends at $endsAtText"
        }

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

        val channelId = if (isExamFinished) {
            NotificationUtils.HEADS_UP_CHANNEL_ID
        } else {
            NotificationUtils.STANDARD_CHANNEL_ID
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(
                if (isExamFinished) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setDefaults(
                if (isExamFinished) NotificationCompat.DEFAULT_ALL
                else 0
            )
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setOnlyAlertOnce(false)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(notificationId, notification)

        return Result.success()
    }

    companion object {
        const val KEY_EXAM_ID = "exam_id"
        const val KEY_SUBJECT_NAME = "subject_name"
        const val KEY_EXAM_TITLE = "exam_title"
        const val KEY_EXAM_ENDS_AT_MS = "exam_ends_at_ms"

        const val KEY_REMINDER_TYPE = "reminder_type"
        const val TYPE_ONE_DAY_BEFORE = "one_day_before"
        const val TYPE_EXAM_FINISHED = "exam_finished"

    }
}