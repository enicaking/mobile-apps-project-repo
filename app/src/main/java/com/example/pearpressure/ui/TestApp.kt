/* TestApp.kt
Handles screen navigation and loads the screens */
package com.example.pearpressure.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pearpressure.MainViewModel
import com.example.pearpressure.ui.screens.*
import com.example.pearpressure.ui.navigation.AppRoutes
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import com.example.pearpressure.notifications.ExamReminderScheduler


// AuthScreen handles login logic
private enum class AuthScreen { LOGIN, COMPLETE_PROFILE, APP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestApp(viewModel: MainViewModel = viewModel()) {
    // Check if user is logged in
    var authScreen by remember {
        mutableStateOf(
            if (viewModel.isUserLoggedIn()) AuthScreen.APP else AuthScreen.LOGIN
        )
    }

    // Flow for sign up -> complete profile -> log in
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

        // Main app logic
        AuthScreen.APP -> {
            val navController = rememberNavController()
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            val subjects by viewModel.subjects.collectAsState()
            val context = LocalContext.current
            val exams by viewModel.exams.collectAsState()
            //Notification exam remainder
            LaunchedEffect(subjects, exams) {
                subjects.forEach { subject ->
                    exams
                        .filter { it.subjectId == subject.id }
                        .forEach { exam ->
                            ExamReminderScheduler.scheduleOneDayBefore(
                                context = context.applicationContext,
                                examId = exam.id,
                                subjectName = subject.name,
                                examTitle = exam.title,
                                examEndsAtMs = exam.endsAtEpochMs
                            )

                            ExamReminderScheduler.scheduleExamFinished(
                                context = context.applicationContext,
                                examId = exam.id,
                                subjectName = subject.name,
                                examTitle = exam.title,
                                examEndsAtMs = exam.endsAtEpochMs
                            )
                        }
                }
            }
            Scaffold(
                // Bottom menu bar
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute?.startsWith("subjects") == true,
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
                        // Nav controller is what allows flow between app pages
                        navController = navController,
                        startDestination = AppRoutes.Subjects.route,
                    ) {

                        composable(AppRoutes.Subjects.route) {
                            SubjectsScreen(
                                subjects = subjects,
                                currentUserId = viewModel.getCurrentUserId(),

                                onAddSubject = { name -> viewModel.addSubject(name) },
                                onOpenSubject = { subjectId ->
                                    navController.navigate(AppRoutes.Exams.createExamsRoute(subjectId))
                                },
                                onActionSubject = { subject -> viewModel.deleteOrLeaveSubject(subject) },
                                onUpdateSubject = { id, newName -> viewModel.updateSubjectName(id, newName) }
                            )
                        }

                        composable(
                            route = AppRoutes.Exams.route,
                            arguments = listOf(
                                navArgument("subjectId") {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
                            // Load data when entering
                            LaunchedEffect(subjectId) { viewModel.loadExams(subjectId) }

                            val subject = viewModel.getSubjectById(subjectId)
                            val exams by viewModel.exams.collectAsState()
                            val friends by viewModel.filteredFriends.collectAsState()

                            if (subject == null) {
                                // Safety fallback
                                Text("Subject not found")
                                return@composable
                            }

                            val currentUserId = viewModel.getCurrentUserId()
                            val isOwner = subject.ownerId == currentUserId

                            ExamsScreen(
                                subjectName = subject.name,
                                exams = exams,
                                isOwner = isOwner,
                                friends = friends,
                                currentUserId = currentUserId,

                                onSearchFriends = { query -> viewModel.searchFriends(query) },
                                onUserSelected = { user ->
                                    viewModel.addMemberToSubject(subject.id, user.uid)
                                },
                                onAddMember = { viewModel.searchFriends("") },
                                onLeaveSubject = {
                                    viewModel.deleteOrLeaveSubject(subject)
                                    navController.popBackStack()
                                },

                                onAddExam = { title, endsAtMs, maxGrade ->
                                    viewModel.addExam(subject.id, title, endsAtMs, maxGrade)


                                },
                                onUpdateExam = { examId, title, endsAtMs, maxGrade ->
                                    viewModel.updateExam(examId, title, endsAtMs, maxGrade)

                                    ExamReminderScheduler.scheduleOneDayBefore(
                                        context = context.applicationContext,
                                        examId = examId,
                                        subjectName = subject.name,
                                        examTitle = title,
                                        examEndsAtMs = endsAtMs
                                    )
                                },
                                // Access dynamic route
                                onOpenInProgressExam = { exam ->
                                    navController.navigate(
                                        AppRoutes.Stopwatch.createStopwatchRoute(exam.id)
                                    )
                                },
                                onDeleteExam = { examId ->
                                    viewModel.deleteExam(examId)
                                    ExamReminderScheduler.cancel(context.applicationContext, examId)
                                },

                                // Button to go back to subjects
                                onBack = { navController.popBackStack() },
                                onSaveResults = { examId, expected, sleep, actual ->
                                    viewModel.saveExamResults(examId, expected, sleep, actual) }
                            )
                        }

                        composable(
                            route = AppRoutes.Stopwatch.route,
                            arguments = listOf(navArgument("examId") {
                                type = NavType.StringType
                            })
                        ) { backStackEntry ->
                            // Backstack contains a history of previous screens
                            // This is what allows the 'back' button to work

                            val examId = backStackEntry.arguments?.getString("examId") ?: ""
                            val exam = viewModel.getExamById(examId)
                            if (exam == null) {
                                Text("Exam not found")
                                return@composable
                            }

                            val subject = viewModel.getSubjectById(exam.subjectId)
                            if (subject == null) {
                                Text("Subject not found")
                                return@composable
                            }

                            StopwatchPage(
                                viewModel = viewModel,
                                subjectName = subject.name,
                                examId = exam.id,
                                examTitle = exam.title,
                                endsAtEpochMs = exam.endsAtEpochMs,

                                onBack = { navController.popBackStack() },
                                onStudyStarted = {
                                    println("CLICK STUDY STARTED")
                                    println("DEBUG subject.id = ${subject.id}")
                                    println("DEBUG subject.name = ${subject.name}")
                                    println("DEBUG exam.title = ${exam.title}")

                                    viewModel.notifyStudyStarted(
                                        subjectId = subject.id,
                                        subjectName = subject.name,
                                        examTitle = exam.title
                                    )
                                }
                            )
                        }

                        // Rest of the main screens that don't contain dynamic routes
                        composable(AppRoutes.Ranking.route) { RankingScreen() }
                        composable(AppRoutes.Friends.route) { FriendsScreen() }
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
