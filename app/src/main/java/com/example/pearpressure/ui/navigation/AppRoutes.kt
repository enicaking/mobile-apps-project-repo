package com.example.pearpressure.ui.navigation

sealed class AppRoutes(val route: String) {

    // Main screens
    object Subjects : AppRoutes("subjects")
    object Ranking : AppRoutes("ranking")
    object Friends : AppRoutes("friends")
    object Profile : AppRoutes("profile")

    // Screens with arguments
    // Dynamic path for exam
    object Exams : AppRoutes("exams/{subjectId}?postExamId={postExamId}") {
        fun createExamsRoute(subjectId: String): String {
            return "exams/$subjectId"
        }

    }

    // Dynamic path for stopwatch
    object Stopwatch : AppRoutes("stopwatch/{examId}") {
        fun createStopwatchRoute(examId: String): String {
            return "stopwatch/$examId"
        }
    }

}