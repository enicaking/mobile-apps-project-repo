package com.example.sofiatest.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.example.sofiatest.data.Exam
import com.example.sofiatest.data.Subject

class AppState {
    val subjects = mutableStateListOf<Subject>()
    val exams = mutableStateListOf<Exam>()

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
}

@Composable
fun rememberAppState(): AppState = remember { AppState() }