package com.example.sofiatest.data

import java.util.UUID

data class Subject(
    val id: String = UUID.randomUUID().toString(),
    val name: String
)

data class Exam(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val title: String,
    val endsAtEpochMs: Long
)