package com.example.pearpressure.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ExamReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra("title") ?: "Exam Reminder"
        val message = intent?.getStringExtra("message") ?: "Your exam is soon!"

        val helper = NotificationHelper(context)
        helper.showReminderNotification(title, message)
    }
}