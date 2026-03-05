package com.example.sofiatest.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.example.sofiatest.data.Exam
import com.example.sofiatest.data.Subject
import java.util.Calendar
import java.util.Locale

class AppState {
    val subjects = mutableStateListOf<Subject>()
    val exams = mutableStateListOf<Exam>()

    init {
        // Add the default subject + exam once when AppState is created.
        ensureDefaultMobileApplicationsProjectExam()
    }

    fun addSubject(name: String): Boolean {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return false
        subjects.add(Subject(name = cleaned))
        return true
    }

    fun addExam(subjectId: String, title: String, endsAtEpochMs: Long): Boolean {
        val cleaned = title.trim()
        if (cleaned.isEmpty()) return false
        exams.add(
            Exam(
                subjectId = subjectId,
                title = cleaned,
                endsAtEpochMs = endsAtEpochMs
            )
        )
        return true
    }

    fun getSubject(id: String): Subject? = subjects.firstOrNull { it.id == id }
    fun getExam(id: String): Exam? = exams.firstOrNull { it.id == id }

    fun examsForSubject(subjectId: String): List<Exam> =
        exams.filter { it.subjectId == subjectId }
            .sortedByDescending { it.endsAtEpochMs }

    // -----------------------------
    // Default data
    // -----------------------------
    private fun ensureDefaultMobileApplicationsProjectExam() {
        val defaultSubjectName = "Mobile Applications"
        val defaultExamTitle = "Project"

        // 1) Ensure subject exists
        val subject = subjects.firstOrNull { it.name.equals(defaultSubjectName, ignoreCase = true) }
            ?: Subject(name = defaultSubjectName).also { subjects.add(it) }

        // 2) Desired end time: May 20th at 00:00 (midnight)
        val desiredEndsAtMs = may20thMidnightEpochMs()

        // 3) Ensure exam exists (no duplicates). If it exists but has a different date, update it.
        val index = exams.indexOfFirst {
            it.subjectId == subject.id && it.title.equals(defaultExamTitle, ignoreCase = true)
        }

        if (index == -1) {
            exams.add(
                Exam(
                    subjectId = subject.id,
                    title = defaultExamTitle,
                    endsAtEpochMs = desiredEndsAtMs
                )
            )
        } else {
            val current = exams[index]
            if (current.endsAtEpochMs != desiredEndsAtMs) {
                exams[index] = current.copy(endsAtEpochMs = desiredEndsAtMs)
            }
        }
    }

    /**
     * Returns epoch milliseconds for May 5th at 00:00 (local time).
     * If May 20th 00:00 already passed this year, it uses next year.
     */
    private fun may20thMidnightEpochMs(): Long {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)

        fun buildForYear(y: Int): Long {
            return Calendar.getInstance(Locale.getDefault()).apply {
                set(Calendar.YEAR, y)
                set(Calendar.MONTH, Calendar.MAY)         // May = 4 internally, but use Calendar.MAY
                set(Calendar.DAY_OF_MONTH, 20)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val thisYear = buildForYear(year)
        return if (thisYear > now.timeInMillis) thisYear else buildForYear(year + 1)
    }
}

@Composable
fun rememberAppState(): AppState = remember { AppState() }