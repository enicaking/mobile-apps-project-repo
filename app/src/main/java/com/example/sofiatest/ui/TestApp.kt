package com.example.sofiatest.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.sofiatest.ui.screens.ExamsScreen
import com.example.sofiatest.ui.screens.StopwatchPage
import com.example.sofiatest.ui.screens.SubjectsScreen

private enum class Screen { SUBJECTS, EXAMS, STOPWATCH }

@Composable
fun SofiaTestApp() {
    val appState = rememberAppState()

    var screenName by rememberSaveable { mutableStateOf(Screen.SUBJECTS.name) }
    var selectedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedExamId by rememberSaveable { mutableStateOf<String?>(null) }

    val screen = Screen.valueOf(screenName)

    when (screen) {
        Screen.SUBJECTS -> {
            SubjectsScreen(
                subjects = appState.subjects,
                onAddSubject = { name -> appState.addSubject(name) },
                onOpenSubject = { subjectId ->
                    selectedSubjectId = subjectId
                    screenName = Screen.EXAMS.name
                }
            )
        }

        Screen.EXAMS -> {
            val subject = selectedSubjectId?.let { appState.getSubject(it) }
            if (subject == null) {
                LaunchedEffect(Unit) { screenName = Screen.SUBJECTS.name }
                return
            }

            ExamsScreen(
                subjectName = subject.name,
                exams = appState.examsForSubject(subject.id),
                onAddExam = { title, endsAtMs ->
                    appState.addExam(subjectId = subject.id, title = title, endsAtEpochMs = endsAtMs)
                },
                onOpenInProgressExam = { exam ->
                    selectedExamId = exam.id
                    screenName = Screen.STOPWATCH.name
                },
                onBack = { screenName = Screen.SUBJECTS.name }
            )
        }

        Screen.STOPWATCH -> {
            val exam = selectedExamId?.let { appState.getExam(it) }
            if (exam == null) {
                LaunchedEffect(Unit) { screenName = Screen.SUBJECTS.name }
                return
            }

            StopwatchPage(
                examTitle = exam.title,
                onBack = { screenName = Screen.EXAMS.name }
            )
        }
    }
}