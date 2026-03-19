package com.example.pearpressure.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

data class Subject(
    @DocumentId val id: String = "", // Firebase pondrá aquí el ID del documento automáticamente
    var ownerId: String = "", // Nuevo: ID del usuario que la creo
    var name: String = "",
    var members: List<String> = emptyList() // Lista de UIDs de los que se han unido(non owner users)
)

data class Exam(
    @DocumentId val id: String = "", // Firebase pondrá aquí el ID del documento automáticamente
    var subjectId: String = "",
    var ownerId: String = "", //Para filtrar exámenes por usuario
    var title: String = "",
    var endsAtEpochMs: Long = 0L
)

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val totalStudyTime: Long = 0L // Esto nos servirá para el Ranking más adelante
)

data class FriendRequest(
    @DocumentId val id: String = "",   // docId (lo usaremos como fromUid_toUid)
    var fromUid: String = "",
    var toUid: String = "",
    var status: String = "pending",    // pending | accepted | declined
    var createdAtEpochMs: Long = 0L
)

data class FriendLink(
    @DocumentId val uid: String = "",  // docId = friendUid
    var createdAtEpochMs: Long = 0L
)