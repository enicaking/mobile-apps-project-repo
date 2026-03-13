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
            db.collection("subjects")
                .add(subject)
                .await()
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

        // 1. Find all exams belonging to this subject
        val examsSnapshot = db.collection("exams")
            .whereEqualTo("subjectId", subjectId)
            .get()
            .await()

        // 2. Add them to the delete batch
        examsSnapshot.documents.forEach { doc ->
            batch.delete(doc.reference)
        }

        // 3. Delete the subject itself
        val subjectRef = db.collection("subjects").document(subjectId)
        batch.delete(subjectRef)

        // 4. Execute all at once
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
        // Si el ID está vacío, usamos .add(). Si ya tiene ID (porque es edición), usamos .set()
        if (exam.id.isEmpty()) {
            db.collection("exams").add(exam).await()
        } else {
            db.collection("exams").document(exam.id).set(exam).await()
        }
        Unit
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

}


