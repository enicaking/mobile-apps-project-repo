package com.example.pearpressure.data

import com.google.firebase.firestore.DocumentId

data class Subject(
    @DocumentId var id: String = "",
    var name: String = ""
)

data class Exam(
    @DocumentId var id: String = "",
    var subjectId: String = "",
    var title: String = "",
    var endsAtEpochMs: Long = 0L
)