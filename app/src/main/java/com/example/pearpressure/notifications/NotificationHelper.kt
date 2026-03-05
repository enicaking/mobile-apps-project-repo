package com.example.pearpressure.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.pearpressure.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val GENERAL_CHANNEL_ID = "general_channel"
        const val REMINDER_CHANNEL_ID = "reminder_channel"
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val generalChannel = NotificationChannel(
                GENERAL_CHANNEL_ID,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            manager.createNotificationChannel(generalChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }

    fun showGeneralNotification(title: String, message: String) {

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun showReminderNotification(title: String, message: String) {

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), builder.build())
    }
    fun scheduleExamReminder(
        examTimeMs: Long,
        title: String,
        message: String
    ) {
        val reminderTime = examTimeMs - (24 * 60 * 60 * 1000) // 24h antes

        if (reminderTime <= System.currentTimeMillis()) return

        val intent = Intent(context, ExamReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderTime.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE)
                as android.app.AlarmManager

        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            reminderTime,
            pendingIntent
        )
    }
}