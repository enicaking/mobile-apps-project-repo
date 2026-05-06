/* FirestoreRepository.kt
This file controls the Firestore logic
Allows users to add, modify, and delete from the database */
package com.example.pearpressure.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val firestore = FirebaseFirestore.getInstance()

    // ── SUBJECTS/EXAMS ──────────────────────────────────────────

    // Create a subject as the owner
    suspend fun addSubject(subject: Subject): Result<Unit> = runCatching {
        // If subject.id is empty, Firestore will generate one.
        // If it has one (e.g. from @DocumentId), it will use it.
        if (subject.id.isEmpty()) {
            val docRef = db.collection("subjects").document()
            val subjectWithId = subject.copy(id = docRef.id)
            docRef.set(subjectWithId).await()
        } else {
            db.collection("subjects")
                .document(subject.id)
                .set(subject)
                .await()
        }
    }

    // Cascade delete subject and nested exams
    suspend fun deleteSubject(subjectId: String): Result<Unit> = runCatching {
        val batch = db.batch()

        // 1. Find exams belonging to the subject
        val examsSnapshot = db.collection("exams")
            .whereEqualTo("subjectId", subjectId)
            .get()
            .await()

        // 2. Add them to the delete batch
        examsSnapshot.documents.forEach { doc ->
            batch.delete(doc.reference)
        }

        val subjectRef = db.collection("subjects").document(subjectId)
        batch.delete(subjectRef)

        batch.commit().await()
    }

    // Leave a subject for the non-owner members
    suspend fun leaveSubject(subjectId: String, userId: String): Result<Unit> = runCatching {
        // Usamos FieldValue.arrayRemove para quitar el ID del usuario de la lista de miembros
        db.collection("subjects").document(subjectId)
            .update("members", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
            .await()
        Unit
    }

    // Add exam to a subject you own
    suspend fun addExam(exam: Exam): Result<Unit> = runCatching {
        if (exam.id.isEmpty()) {
            val docRef = db.collection("exams").document()
            val examWithId = exam.copy(id = docRef.id)
            docRef.set(examWithId).await()
        } else {
            db.collection("exams")
                .document(exam.id)
                .set(exam)
                .await()
        }
    }

    // Delete an exam
    suspend fun deleteExam(examId: String): Result<Unit> = runCatching {
        db.collection("exams")
            .document(examId)
            .delete()
            .await()
    }

    // Add friend to a subject as the owner
    suspend fun addMemberToSubject(subjectId: String, userId: String): Result<Unit> = runCatching {
        db.collection("subjects")
            .document(subjectId)
            .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
            .await()
    }

    // Exam listener
    fun listenToExams(subjectId: String, onChange: (List<Exam>) -> Unit): ListenerRegistration {
        return db.collection("exams")
            .whereEqualTo("subjectId", subjectId) // Filter for subject
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                onChange(snapshot.toObjects(Exam::class.java))
            }
    }

    fun listenToSubjectsForUser(
        userId: String,
        onChange: (List<Subject>) -> Unit
    ): List<ListenerRegistration> {

        var owned: List<Subject> = emptyList()
        var member: List<Subject> = emptyList()

        fun emit() {
            onChange((owned + member).distinctBy { it.id })
        }

        val l1 = db.collection("subjects")
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                owned = snapshot.toObjects(Subject::class.java)
                emit()
            }

        val l2 = db.collection("subjects")
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                member = snapshot.toObjects(Subject::class.java)
                emit()
            }

        return listOf(l1, l2)
    }


    // ── PROFILE ───────────────────────────────

    // Check for unique username
    suspend fun isUsernameAvailable(username: String): Result<Boolean> = runCatching {
        val normalized = username.trim().lowercase()

        val snapshot = db.collection("users")
            .whereEqualTo("username", normalized)
            .get()
            .await()

        snapshot.isEmpty
    }

    // Create new user
    suspend fun saveCompletedUserProfile(user: UserProfile): Result<Unit> = runCatching {
        val normalizedUser = user.copy(username = user.username.trim().lowercase())
        db.collection("users")
            .document(user.uid)
            .set(normalizedUser, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }


    // ── USERS ───────────────────────────────────────────────

    // Get user profile by ID
    suspend fun getUserProfile(uid: String): Result<UserProfile?> = runCatching {
        db.collection("users")
            .document(uid)
            .get()
            .await()
            .toObject(UserProfile::class.java)
    }

    // Get user profiles by ID
    suspend fun getUserProfilesByIds(uids: List<String>): Result<List<UserProfile>> = runCatching {
        if (uids.isEmpty()) return@runCatching emptyList<UserProfile>()

        // Firestore "whereIn" max 10 -> chunks
        val chunks = uids.distinct().chunked(10)
        val result = mutableListOf<UserProfile>()

        for (chunk in chunks) {
            val snap = db.collection("users")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                .get()
                .await()

            result += snap.toObjects(UserProfile::class.java)
        }
        result
    }

    // Get user profile by email for adding as friends
    suspend fun findUserByEmail(email: String): Result<UserProfile?> = runCatching {
        val snap = db.collection("users")
            .whereEqualTo("email", email.trim())
            .limit(1)
            .get()
            .await()

        snap.documents.firstOrNull()?.toObject(UserProfile::class.java)
    }


    // ── FRIENDS / REQUESTS ─────────────────────────────────────────────

    // Listener for friends
    fun listenFriends(friendOwnerUid: String, onChange: (List<String>) -> Unit): ListenerRegistration {
        return db.collection("users")
            .document(friendOwnerUid)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val friendUids = snapshot.documents.map { it.id }.distinct()
                onChange(friendUids)
            }
    }

    // Listener for incoming friend requests
    fun listenIncomingFriendRequests(
        myUid: String,
        onChange: (List<FriendRequest>) -> Unit
    ): ListenerRegistration {
        return db.collection("friend_requests")
            .whereEqualTo("toUid", myUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val all = snapshot.toObjects(FriendRequest::class.java)
                onChange(all.filter { it.status == "pending" })
            }
    }

    // Listener for outgoing friend requests
    fun listenOutgoingFriendRequests(
        myUid: String,
        onChange: (List<FriendRequest>) -> Unit
    ): ListenerRegistration {
        return db.collection("friend_requests")
            .whereEqualTo("fromUid", myUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val all = snapshot.toObjects(FriendRequest::class.java)
                onChange(all.filter { it.status == "pending" })
            }
    }

    // Once email is searched, send outgoing friend request
    suspend fun sendFriendRequest(fromUid: String, toUid: String): Result<Unit> = runCatching {
        require(fromUid.isNotBlank() && toUid.isNotBlank()) { "Missing uid" }
        require(fromUid != toUid) { "You cannot add yourself" }

        // If already friends -> do nothing
        val already = db.collection("users").document(fromUid)
            .collection("friends").document(toUid)
            .get().await()
            .exists()

        check(!already) { "You are already friends" }

        val requestId = "${fromUid}_${toUid}"

        val req = FriendRequest(
            id = requestId,
            fromUid = fromUid,
            toUid = toUid,
            status = "pending",
            createdAtEpochMs = System.currentTimeMillis()
        )

        db.collection("friend_requests")
            .document(requestId)
            .set(req)
            .await()
    }

    // Accept incoming friend request
    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> = runCatching {
        val fromUid = request.fromUid
        val toUid = request.toUid
        require(fromUid.isNotBlank() && toUid.isNotBlank()) { "Missing uid" }

        val now = System.currentTimeMillis()
        val batch = db.batch()

        // Create both friend links
        val aRef = db.collection("users").document(fromUid)
            .collection("friends").document(toUid)
        val bRef = db.collection("users").document(toUid)
            .collection("friends").document(fromUid)

        batch.set(aRef, mapOf("createdAtEpochMs" to now))
        batch.set(bRef, mapOf("createdAtEpochMs" to now))

        // Delete request (and also delete the reverse request if it exists)
        val reqId = if (request.id.isNotBlank()) request.id else "${fromUid}_${toUid}"
        val reqRef = db.collection("friend_requests").document(reqId)
        val reverseRef = db.collection("friend_requests").document("${toUid}_${fromUid}")

        batch.delete(reqRef)
        batch.delete(reverseRef)

        batch.commit().await()
    }

    // Decline incoming friend request
    suspend fun declineFriendRequest(request: FriendRequest): Result<Unit> = runCatching {
        val fromUid = request.fromUid
        val toUid = request.toUid
        val reqId = if (request.id.isNotBlank()) request.id else "${fromUid}_${toUid}"

        db.collection("friend_requests")
            .document(reqId)
            .delete()
            .await()
    }

    // Remove someone from being your friend
    suspend fun removeFriend(myUid: String, friendUid: String): Result<Unit> = runCatching {
        val batch = db.batch()

        val aRef = db.collection("users").document(myUid)
            .collection("friends").document(friendUid)
        val bRef = db.collection("users").document(friendUid)
            .collection("friends").document(myUid)

        batch.delete(aRef)
        batch.delete(bRef)

        batch.commit().await()
    }


    // ── SESSIONS ───────────────────────────────────────────────

    // Record a session under a specific user and exam
    suspend fun addSession(session: Session): Result<Unit> = runCatching {

        val docRef = db.collection("sessions").document()

        // Create a copy that includes the generated ID so the document
        // inside Firestore knows its own ID
        val sessionWithId = session.copy(id = docRef.id)

        // .set() takes the WHOLE object and maps it to Firestore fields
        docRef.set(sessionWithId).await()

    }

    // Aggregate all sessions for a user
    fun listenToSessionsForUser(
        userId: String,
        onChange: (List<Session>) -> Unit
    ): ListenerRegistration {
        return db.collection("sessions")
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                onChange(snapshot.toObjects(Session::class.java))
            }
    }


    // ── STATS & RANKING UPDATES  ──────────────────

    // Update exam stats for a specific user
    suspend fun updateExamStats(
        examId: String,
        userId: String,
        expected: Double?,
        sleep: Double?,
        actual: Double?
    ): Result<Unit> = runCatching {
        val docRef = db.collection("exams").document(examId)
        val updates = mutableMapOf<String, Any>()

        // If a value is provided, we set it.
        // If it is NULL, we use FieldValue.delete() to remove that user's entry entirely.
        updates["expectedGrades.$userId"] = expected ?: com.google.firebase.firestore.FieldValue.delete()
        updates["sleepHours.$userId"] = sleep ?: com.google.firebase.firestore.FieldValue.delete()
        updates["actualGrades.$userId"] = actual ?: com.google.firebase.firestore.FieldValue.delete()

        docRef.update(updates).await()
    }

    // Edit subjects and exams
    suspend fun updateSubjectName(subjectId: String, newName: String): Result<Unit> {
        return try {
            db.collection("subjects").document(subjectId)
                .update("name", newName)
                .await() // Added .await() to ensure it finishes
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateExam(examId: String, newTitle: String, newEndsAtMs: Long, newMaxGrade: Double = 10.0): Result<Unit> {
        return try {
            db.collection("exams").document(examId)
                .update(
                    "title", newTitle,
                    "endsAtEpochMs", newEndsAtMs,
                    "maxGrade", newMaxGrade
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gets all exams for a specific subject (for Accuracy/Efficiency rankings)
    suspend fun getExamsBySubjectSync(subjectId: String): List<Exam> = try {
        db.collection("exams")
            .whereEqualTo("subjectId", subjectId)
            .get()
            .await()
            .toObjects(Exam::class.java)
    } catch (e: Exception) { emptyList() }

    // Gets all study sessions linked to specific exams (for Habit/Hard Work rankings)
    suspend fun getSessionsForExamsSync(examIds: List<String>): List<Session> = try {
        if (examIds.isEmpty()) emptyList()
        else {
            val result = mutableListOf<Session>()
            // Firestore limit is 10 for 'whereIn', so we chunk the IDs
            examIds.chunked(10).forEach { chunk ->
                val snap = db.collection("sessions")
                    .whereIn("examId", chunk)
                    .get()
                    .await()
                result += snap.toObjects(Session::class.java)
            }
            result
        }
    } catch (e: Exception) { emptyList() }

    // Add study event and trigger notification
    suspend fun addStudyEvent(event: StudyEvent) {
        firestore.collection("study_events")
            .add(event)
            .await()
    }


    // ──── STREAKS & STATS UPDATES ──────────────

    // Update total time, streak and last session date
    suspend fun updateUserStreakAndStats(
        userId: String,
        addedMs: Long,
        newStreak: Int,
        lastDateMs: Long
    ): Result<Unit> = runCatching {
        val docRef = db.collection("users").document(userId)
        val updates = mapOf(
            "totalStudyTime" to com.google.firebase.firestore.FieldValue.increment(addedMs),
            "currentStreak" to newStreak,
            "lastStudyDateMs" to lastDateMs
        )
        docRef.update(updates).await()
    }
}