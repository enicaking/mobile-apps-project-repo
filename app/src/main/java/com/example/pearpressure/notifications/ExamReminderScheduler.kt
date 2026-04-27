package com.example.pearpressure.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ExamReminderScheduler {
    private const val WORK_PREFIX = "exam_reminder_"
    private const val EXAM_FINISHED_WORK_PREFIX = "exam_finished_"

    // TEST ONLY: 1 minute before exam.
    // For final version, use: 24L * 60 * 60 * 1000
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

        if (delayMs <= 0L) return

        val data = workDataOf(
            ExamReminderWorker.KEY_EXAM_ID to examId,
            ExamReminderWorker.KEY_SUBJECT_NAME to subjectName,
            ExamReminderWorker.KEY_EXAM_TITLE to examTitle,
            ExamReminderWorker.KEY_EXAM_ENDS_AT_MS to examEndsAtMs,
            ExamReminderWorker.KEY_REMINDER_TYPE to ExamReminderWorker.TYPE_ONE_DAY_BEFORE
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

    fun scheduleExamFinished(
        context: Context,
        examId: String,
        subjectName: String,
        examTitle: String,
        examEndsAtMs: Long
    ) {
        val delayMs = examEndsAtMs - System.currentTimeMillis()

        if (delayMs <= 0L) return

        val data = workDataOf(
            ExamReminderWorker.KEY_EXAM_ID to examId,
            ExamReminderWorker.KEY_SUBJECT_NAME to subjectName,
            ExamReminderWorker.KEY_EXAM_TITLE to examTitle,
            ExamReminderWorker.KEY_EXAM_ENDS_AT_MS to examEndsAtMs,
            ExamReminderWorker.KEY_REMINDER_TYPE to ExamReminderWorker.TYPE_EXAM_FINISHED
        )

        val request = OneTimeWorkRequestBuilder<ExamReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            EXAM_FINISHED_WORK_PREFIX + examId,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, examId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_PREFIX + examId)
        WorkManager.getInstance(context).cancelUniqueWork(EXAM_FINISHED_WORK_PREFIX + examId)
    }
}