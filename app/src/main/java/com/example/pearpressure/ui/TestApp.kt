package com.example.pearpressure.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pearpressure.MainViewModel
import com.example.pearpressure.ui.screens.*
import com.example.pearpressure.data.UserProfile
import com.example.pearpressure.ui.navigation.AppRoutes
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState

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
            val navController = rememberNavController()
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            val subjects by viewModel.subjects.collectAsState()
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == AppRoutes.Subjects.route,
                            onClick = {
                                navController.navigate(AppRoutes.Subjects.route) {
                                    popUpTo(AppRoutes.Subjects.route)
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Subjects") },
                            label = { Text("Home") }
                        )

                        NavigationBarItem(
                            selected = currentRoute == AppRoutes.Ranking.route,
                            onClick = {
                                navController.navigate(AppRoutes.Ranking.route) {
                                    popUpTo(AppRoutes.Subjects.route)
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.Star, contentDescription = "Ranking") },
                            label = { Text("Ranking") }
                        )

                        NavigationBarItem(
                            selected = currentRoute == AppRoutes.Friends.route,
                            onClick = {
                                navController.navigate(AppRoutes.Friends.route) {
                                    popUpTo(AppRoutes.Subjects.route)
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Friends") },
                            label = { Text("Friends") }
                        )

                        NavigationBarItem(
                            selected = currentRoute == AppRoutes.Profile.route,
                            onClick = {
                                navController.navigate(AppRoutes.Profile.route) {
                                    popUpTo(AppRoutes.Subjects.route)
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
                            label = { Text("Profile") }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.Subjects.route,
                    ) {

                        composable(AppRoutes.Subjects.route) {
                            SubjectsScreen(
                                subjects = subjects,
                                currentUserId = viewModel.getCurrentUserId(),

                                onAddSubject = { name ->
                                    viewModel.addSubject(name)
                                },

                                onOpenSubject = { subjectId ->
                                    navController.navigate(
                                        AppRoutes.Exams.createExamsRoute(subjectId)
                                    )
                                },

                                onActionSubject = { subject ->
                                    viewModel.deleteOrLeaveSubject(subject)
                                },

                                onUpdateSubject = { id, newName ->
                                    viewModel.updateSubjectName(id, newName)
                                }
                            )
                        }

                        composable(AppRoutes.Exams.route) { backStackEntry ->
                            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
                            Text("Exams for subject: $subjectId") // temporal
                        }

                        composable(AppRoutes.Ranking.route) {
                            RankingScreen()
                        }

                        composable(AppRoutes.Friends.route) {
                            FriendsScreen()
                        }

                        composable(AppRoutes.Profile.route) {
                            ProfileScreen(
                                viewModel = viewModel,
                                onLogout = {
                                    authScreen = AuthScreen.LOGIN
                                }
                            )
                        }
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
                // Action handles delete/leave logic from VM
                onActionSubject = { subject ->
                    viewModel.deleteOrLeaveSubject(subject)
                },
                // FIXED: Passing the update logic to clear red errors
                onUpdateSubject = { subjectId, newName ->
                    viewModel.updateSubjectName(subjectId, newName)
                }
            )
        }

        HomeScreen.EXAMS -> {
            val subject = selectedSubjectId?.let { id: String ->
                viewModel.getSubjectById(id)
            }
            if (subject == null) {
                screen = HomeScreen.SUBJECTS
                return
            }

            val currentUserId = viewModel.getCurrentUserId()
            val isOwner = subject.ownerId == currentUserId
            val friends by viewModel.filteredFriends.collectAsState()

            ExamsScreen(
                subjectName = subject.name,
                exams = exams,
                isOwner = isOwner,
                friends = friends,
                currentUserId = currentUserId,

                onSearchFriends = { query ->
                    viewModel.searchFriends(query)
                },

                onUserSelected = { user ->
                    viewModel.addMemberToSubject(subject.id, user.uid)
                },

                onAddMember = {
                    viewModel.searchFriends("") // Reset search when opening member dialog
                },

                onLeaveSubject = {
                    viewModel.deleteOrLeaveSubject(subject)
                    screen = HomeScreen.SUBJECTS
                },

                // UPDATED: Added maxGrade to match new ExamsScreen signature
                onAddExam = { title, endsAtMs, maxGrade ->
                    viewModel.addExam(
                        subjectId = subject.id,
                        title = title,
                        endsAtMs = endsAtMs,
                        maxGrade = maxGrade
                    )
                },

                // FIXED: Passing update logic to clear red errors
                // UPDATED: Now passing maxGrade so editing the scale actually works
                onUpdateExam = { examId, title, endsAtMs, maxGrade ->
                    viewModel.updateExam(examId, title, endsAtMs, maxGrade)
                },

                onOpenInProgressExam = { exam ->
                    selectedExamId = exam.id
                    screen = HomeScreen.STOPWATCH
                },

                onDeleteExam = { examId ->
                    viewModel.deleteExam(examId)
                },

                onBack = { screen = HomeScreen.SUBJECTS },

                // Phase 3 Stats recording
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