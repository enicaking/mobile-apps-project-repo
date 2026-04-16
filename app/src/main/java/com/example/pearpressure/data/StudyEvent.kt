package com.example.pearpressure.data

data class StudyEvent(
    val fromUserId: String = "",
    val fromUserName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val examTitle: String = "",
    val startedAtEpochMs: Long = 0L,
    val type: String = "study_started"
)