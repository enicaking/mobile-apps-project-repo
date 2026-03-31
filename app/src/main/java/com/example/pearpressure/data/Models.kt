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
    var endsAtEpochMs: Long = 0L,

    //ADDED FOR EXPECTED GRADE, REAL GRADE, SLEEPING HOURS
    var expectedGrades: Map<String, Double> = emptyMap(),
    var actualGrades: Map<String, Double> = emptyMap(),
    var sleepHours: Map<String, Double> = emptyMap(),
    val maxGrade: Double = 10.0
)

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val username: String = "",
    val sex: String = "",
    val birthdayEpochMs: Long = 0L,
    val email: String = "",
    val totalStudyTime: Long = 0L
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

data class Session(
    @DocumentId val id: String = "",
    var ownerId: String = "",
    var examId: String = "",
    var durationMs: Long = 0L,
    var createdAtEpochMs: Long = 0L,
    //counters for rankings and statistics
    var waterCount: Int = 0,      // ml de agua
    var coffeeCount: Int = 0,     // cantidad de cafés
    var energyDrinkCount: Int = 0, // cantidad de bebidas energéticas
    var bathroomBreaks: Int = 0    // cantidad de veces al baño (poop)
)
// UI-only model for the Ranking Screen
data class RankingEntryUi(
    val uid: String,
    val userName: String,
    val totalStudyTimeMs: Long,
    val avgAccuracy: Double = 0.0, // (Actual - Expected) difference
    val efficiencyScore: Double = 0.0, // Grade / Hours
    val totalWater: Int = 0,
    val totalCoffee: Int = 0,
    val totalEnergy: Int = 0,
    val totalBathroom: Int = 0
)