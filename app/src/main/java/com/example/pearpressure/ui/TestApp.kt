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
import com.example.pearpressure.data.UserProfile

private enum class HomeScreen { SUBJECTS, EXAMS, STOPWATCH }

private enum class AuthScreen {
    LOGIN,
    COMPLETE_PROFILE,
    APP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestApp(viewModel: MainViewModel = viewModel()) {
    var authScreen by remember {
        mutableStateOf(
            if (viewModel.isUserLoggedIn()) AuthScreen.APP else AuthScreen.LOGIN
        )
    }

    when (authScreen) {
        AuthScreen.LOGIN -> {
            LoginScreen(
                onLoginSuccess = { authScreen = AuthScreen.APP },
                onRegisterNeedsProfile = { authScreen = AuthScreen.COMPLETE_PROFILE },
                viewModel = viewModel
            )
        }

        AuthScreen.COMPLETE_PROFILE -> {
            CompleteProfileScreen(
                viewModel = viewModel,
                onProfileCompleted = { authScreen = AuthScreen.APP }
            )
        }

        AuthScreen.APP -> {
            val tabs = listOf(MainTab.HOME, MainTab.RANKING, MainTab.FRIENDS, MainTab.PROFILE)
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
                            onLogout = { authScreen = AuthScreen.LOGIN }
                        )
                    }
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
                currentUserId = viewModel.getCurrentUserId(),
                onAddSubject = { name -> viewModel.addSubject(name) },
                onOpenSubject = { subjectId ->
                    selectedSubjectId = subjectId
                    viewModel.loadExams(subjectId)
                    screen = HomeScreen.EXAMS
                },
                onActionSubject = { subject ->
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

            val currentUserId = viewModel.getCurrentUserId()
            val isOwner = subject.ownerId == currentUserId
            val friends by viewModel.filteredFriends.collectAsState()

            // Now passing currentUserId and onSaveResults callback
            ExamsScreen(
                subjectName = subject.name,
                exams = exams,
                isOwner = isOwner,
                friends = friends,
                currentUserId = currentUserId, // This fixes the red error!

                onSearchFriends = { query ->
                    viewModel.searchFriends(query)
                },

                onUserSelected = { user ->
                    if (!subject.members.contains(user.uid)) {
                        viewModel.addMemberToSubject(subject.id, user.uid)
                    }
                },

                onAddMember = {
                    viewModel.searchFriends("")
                },

                onLeaveSubject = {
                    viewModel.deleteOrLeaveSubject(subject)
                    screen = HomeScreen.SUBJECTS
                },

                onAddExam = { title, endsAtMs ->
                    viewModel.addExam(
                        subjectId = subject.id,
                        title = title,
                        endsAtMs = endsAtMs
                    )
                },

                onOpenInProgressExam = { exam ->
                    selectedExamId = exam.id
                    screen = HomeScreen.STOPWATCH
                },

                onDeleteExam = { examId ->
                    viewModel.deleteExam(examId)
                },

                onBack = { screen = HomeScreen.SUBJECTS },

                // This saves the stats to the database
                onSaveResults = { examId, expected, sleep, actual ->
                    viewModel.saveExamResults(examId, expected, sleep, actual)
                }
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
                viewModel = viewModel,
                subjectName = subject.name,
                examId = exam.id,
                examTitle = exam.title,
                endsAtEpochMs = exam.endsAtEpochMs,
                onBack = { screen = HomeScreen.EXAMS }
            )
        }
    }
}