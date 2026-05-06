/* Models.kt
This file contains the structure of the Firestore database */

package com.example.pearpressure.data

import com.google.firebase.firestore.DocumentId

data class Subject(
    @DocumentId val id: String = "", // Automatically generated
    var ownerId: String = "", //ID of user that created it
    var name: String = "",
    var members: List<String> = emptyList() // List of UIDs of users that joined (non owner users)
)

data class Exam(
    @DocumentId val id: String = "", // Automatically generated
    var subjectId: String = "",
    var ownerId: String = "",
    var title: String = "",
    var endsAtEpochMs: Long = 0L,

    // Added after the exam has finished
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
    val totalStudyTime: Long = 0L,
    val currentStreak: Int = 0,
    val lastStudyDateMs: Long = 0L      
)

data class FriendRequest(
    @DocumentId val id: String = "",   // docId (used as fromUid_toUid)
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
    var waterCount: Int = 0,        // ml water
    var coffeeCount: Int = 0,       // nº coffees
    var energyDrinkCount: Int = 0,  // nº energy drinks
    var bathroomBreaks: Int = 0     // nº poop
)

data class StudyEvent(
    val fromUserId: String = "",
    val fromUserName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val examTitle: String = "",
    val startedAtEpochMs: Long = 0L,
    val type: String = "study_started"
)