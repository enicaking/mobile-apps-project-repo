package com.example.pearpressure.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pearpressure.MainViewModel
import com.example.pearpressure.ui.screens.*

private enum class HomeScreen { SUBJECTS, EXAMS, STOPWATCH }

@Composable
fun SofiaTestApp(viewModel: MainViewModel = viewModel()) {
    // 1. New state to track if we are logged in
    var isLoggedIn by remember { mutableStateOf(viewModel.isUserLoggedIn()) }

    // 2. Logic: If NOT logged in, show LoginScreen. If logged in, show the app.
    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = { isLoggedIn = true },
            viewModel = viewModel
        )
    } else {
        //showing the app when logged in:
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
                    MainTab.HOME -> HomeFlow(viewModel)
                    MainTab.RANKING -> RankingScreen()
                    MainTab.FRIENDS -> FriendsScreen()
                    MainTab.PROFILE -> ProfileScreen(
                        viewModel = viewModel,
                        onLogout = { isLoggedIn = false } // This sends the user back to LoginScreen
                    )
                }
            }
        }

    }
}

@Composable
private fun HomeFlow(viewModel: MainViewModel) {
    var screen by rememberSaveable { mutableStateOf(HomeScreen.SUBJECTS) }
    var selectedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedExamId by rememberSaveable { mutableStateOf<String?>(null) }

    val subjects by viewModel.subjects.collectAsState()
    val exams by viewModel.exams.collectAsState()

    when (screen) {
        HomeScreen.SUBJECTS -> {
            SubjectsScreen(
                subjects = subjects,
                currentUserId = viewModel.getCurrentUserId(), // <--- AÑADIDO
                onAddSubject = { name -> viewModel.addSubject(name) },
                onOpenSubject = { subjectId ->
                    selectedSubjectId = subjectId
                    viewModel.loadExams(subjectId)
                    screen = HomeScreen.EXAMS
                },
                onActionSubject = { subject -> // <--- AÑADIDO
                    viewModel.deleteOrLeaveSubject(subject)
                }
            )
        }

        HomeScreen.EXAMS -> {
            val subject = selectedSubjectId?.let { viewModel.getSubjectById(it) }
            if (subject == null) {
                screen = HomeScreen.SUBJECTS
                return
            }

            ExamsScreen(
                subjectName = subject.name,
                exams = exams,
                onAddExam = { title, endsAtMs ->
                    viewModel.addExam(subjectId = subject.id, title = title, endsAtMs = endsAtMs)
                },
                onOpenInProgressExam = { exam ->
                    selectedExamId = exam.id
                    screen = HomeScreen.STOPWATCH
                },
                onDeleteExam = { examId -> //to deete exam
                    viewModel.deleteExam(examId)
                },
                onBack = { screen = HomeScreen.SUBJECTS }
            )
        }

        HomeScreen.STOPWATCH -> {
            val exam = selectedExamId?.let { viewModel.getExamById(it) }
            val subject = selectedSubjectId?.let { viewModel.getSubjectById(it) }

            if (exam == null || subject == null) {
                screen = HomeScreen.SUBJECTS
                return
            }

            StopwatchPage(
                subjectName = subject.name,
                examTitle = exam.title,
                endsAtEpochMs = exam.endsAtEpochMs,
                onBack = { screen = HomeScreen.EXAMS }
            )
        }
    }
}