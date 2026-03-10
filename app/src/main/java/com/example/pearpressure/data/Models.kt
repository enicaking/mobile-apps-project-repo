package com.example.pearpressure.data

import java.util.UUID

data class Subject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = ""  // ← add default values to ALL fields
)

data class Exam(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String = "",
    val title: String = "",
    val endsAtEpochMs: Long = 0L
)