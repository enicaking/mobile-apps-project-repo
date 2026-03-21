package com.example.pearpressure.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db: FirebaseFirestore = Firebase.firestore

    // ── SUBJECTS ──────────────────────────────────────────


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

    suspend fun getSubjects(): Result<List<Subject>> = runCatching {
        db.collection("subjects")
            .get()
            .await()
            .toObjects(Subject::class.java)
    }

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
    //LEAVE, for the non owner users of a subject
    suspend fun leaveSubject(subjectId: String, userId: String): Result<Unit> = runCatching {
        // Usamos FieldValue.arrayRemove para quitar el ID del usuario de la lista de miembros
        db.collection("subjects").document(subjectId)
            .update("members", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
            .await()
        Unit
    }

    // ── EXAMS ─────────────────────────────────────────────

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

    suspend fun getExamsForSubject(subjectId: String): Result<List<Exam>> = runCatching {
        db.collection("exams")
            .whereEqualTo("subjectId", subjectId)
            .get()
            .await()
            .toObjects(Exam::class.java)
    }

    suspend fun deleteExam(examId: String): Result<Unit> = runCatching {
        db.collection("exams")
            .document(examId)
            .delete()
            .await()
    }

    // ── REAL-TIME LISTENERS ───────────────────────────────

    // Cambia la función de escuchar asignaturas
    fun listenToSubjects(userId: String, onChange: (List<Subject>) -> Unit): ListenerRegistration {
        return db.collection("subjects")
            .whereEqualTo("ownerId", userId) //Only the owner
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                onChange(snapshot.toObjects(Subject::class.java))
            }
    }

    fun listenToExams(subjectId: String, onChange: (List<Exam>) -> Unit): ListenerRegistration {
        return db.collection("exams")
            .whereEqualTo("subjectId", subjectId) //ESTO es lo que filtra por asignatura
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                onChange(snapshot.toObjects(Exam::class.java))
            }
    }

    suspend fun createUserProfile(user: UserProfile) = runCatching {
        db.collection("users").document(user.uid).set(user).await()
    }  //for storing when creating user

    suspend fun isUsernameAvailable(username: String): Result<Boolean> = runCatching {
        val normalized = username.trim().lowercase()

        val snapshot = db.collection("users")
            .whereEqualTo("username", normalized)
            .get()
            .await()

        snapshot.isEmpty
    }

    suspend fun saveCompletedUserProfile(user: UserProfile): Result<Unit> = runCatching {
        val normalizedUser = user.copy(username = user.username.trim().lowercase())
        db.collection("users").document(user.uid).set(normalizedUser).await()
    }


    // ── SUBJECTS (owner OR member) ──────────────────────────
    // Devuelve 2 listeners (owned + member). El ViewModel los guardará y los cerrará.
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

    // ── USERS ───────────────────────────────────────────────

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

    suspend fun findUserByEmail(email: String): Result<UserProfile?> = runCatching {
        val snap = db.collection("users")
            .whereEqualTo("email", email.trim())
            .limit(1)
            .get()
            .await()

        snap.documents.firstOrNull()?.toObject(UserProfile::class.java)
    }

    // ── FRIENDS / REQUESTS ─────────────────────────────────────────────

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

    suspend fun declineFriendRequest(request: FriendRequest): Result<Unit> = runCatching {
        val fromUid = request.fromUid
        val toUid = request.toUid
        val reqId = if (request.id.isNotBlank()) request.id else "${fromUid}_${toUid}"

        db.collection("friend_requests")
            .document(reqId)
            .delete()
            .await()
    }

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


    suspend fun addSession(session: Session): Result<Unit> = runCatching {
        val docRef = db.collection("sessions").document()
        val sessionWithId = session.copy(id = docRef.id)
        docRef.set(sessionWithId).await()
    }

    suspend fun getSessionsForUser(userId: String): Result<List<Session>> = runCatching {
        db.collection("sessions")
            .whereEqualTo("ownerId", userId)
            .get()
            .await()
            .toObjects(Session::class.java)
    }

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

}


