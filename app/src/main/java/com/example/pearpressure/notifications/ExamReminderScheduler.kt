package com.example.pearpressure.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ExamReminderScheduler {
    private const val WORK_PREFIX = "exam_reminder_"
    //private const val ONE_DAY_MS = 24L * 60 * 60 * 1000
    private const val ONE_DAY_MS = 60_000L

    fun scheduleOneDayBefore(
        context: Context,
        examId: String,
        subjectName: String,
        examTitle: String,
        examEndsAtMs: Long
    ) {
        val triggerAtMs = examEndsAtMs - ONE_DAY_MS
        val delayMs = triggerAtMs - System.currentTimeMillis()

        if (delayMs <= 0L) {
            // Too late to schedule (exam is < 24h away or already passed)
            return
        }

        val data = workDataOf(
            ExamReminderWorker.KEY_EXAM_ID to examId,
            ExamReminderWorker.KEY_SUBJECT_NAME to subjectName,
            ExamReminderWorker.KEY_EXAM_TITLE to examTitle,
            ExamReminderWorker.KEY_EXAM_ENDS_AT_MS to examEndsAtMs
        )

        val request = OneTimeWorkRequestBuilder<ExamReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_PREFIX + examId,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, examId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_PREFIX + examId)
    }
}