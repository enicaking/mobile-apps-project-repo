package com.example.pearpressure.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.pearpressure.ui.screens.*

private enum class HomeScreen { SUBJECTS, EXAMS, STOPWATCH }

@Composable
fun SofiaTestApp() {
    val appState = rememberAppState()

    // Tab seleccionada (menú de abajo)
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTabs.forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item.tab,
                        onClick = { selectedTab = item.tab },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.HOME -> HomeFlow(appState)
                MainTab.RANKING -> RankingScreen()
                MainTab.FRIENDS -> FriendsScreen()
                MainTab.PROFILE -> ProfileScreen()
            }
        }
    }
}

@Composable
private fun HomeFlow(appState: AppState) {
    // Navegación “de Inicio” (tu flujo actual)
    var screen by rememberSaveable { mutableStateOf(HomeScreen.SUBJECTS) }
    var selectedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedExamId by rememberSaveable { mutableStateOf<String?>(null) }

    when (screen) {
        HomeScreen.SUBJECTS -> {
            SubjectsScreen(
                subjects = appState.subjects,
                onAddSubject = { name -> appState.addSubject(name) },
                onOpenSubject = { subjectId ->
                    selectedSubjectId = subjectId
                    screen = HomeScreen.EXAMS
                }
            )
        }

        HomeScreen.EXAMS -> {
            val subject = selectedSubjectId?.let { appState.getSubject(it) }
            if (subject == null) {
                screen = HomeScreen.SUBJECTS
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
                    screen = HomeScreen.STOPWATCH
                },
                onBack = { screen = HomeScreen.SUBJECTS }
            )
        }

        HomeScreen.STOPWATCH -> {
            val exam = selectedExamId?.let { appState.getExam(it) }
            if (exam == null) {
                screen = HomeScreen.SUBJECTS
                return
            }

            StopwatchPage(
                subjectName = appState.getSubject(exam.subjectId)?.name ?: "Asignatura",
                examTitle = exam.title,
                endsAtEpochMs = exam.endsAtEpochMs,
                onBack = { screen = HomeScreen.EXAMS }
            )
        }
    }
}