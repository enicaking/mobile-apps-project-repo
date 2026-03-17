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
}


