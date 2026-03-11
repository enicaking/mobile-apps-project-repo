package com.example.pearpressure.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

data class Subject(
    @DocumentId @get:Exclude var id: String = "",
    var name: String = ""
)

data class Exam(
    @DocumentId @get:Exclude var id: String = "",
    var subjectId: String = "",
    var title: String = "",
    var endsAtEpochMs: Long = 0L
)