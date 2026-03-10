package com.example.pearpressure.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db: FirebaseFirestore = Firebase.firestore

    // ── SUBJECTS ──────────────────────────────────────────

    suspend fun addSubject(subject: Subject): Result<Unit> = runCatching {
        db.collection("subjects")
            .document(subject.id)
            .set(subject)
            .await()
    }

    suspend fun getSubjects(): Result<List<Subject>> = runCatching {
        db.collection("subjects")
            .get()
            .await()
            .toObjects(Subject::class.java)
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> = runCatching {
        db.collection("subjects")
            .document(subjectId)
            .delete()
            .await()
    }

    // ── EXAMS ─────────────────────────────────────────────

    suspend fun addExam(exam: Exam): Result<Unit> = runCatching {
        db.collection("exams")
            .document(exam.id)
            .set(exam)
            .await()
    }

    suspend fun getExamsForSubject(subjectId: String): Result<List<Exam>> = runCatching {
        db.collection("exams")
            .whereEqualTo("subjectId", subjectId)
            .get()
            .await()
            .toObjects(Exam::class.java)
    }

    suspend fun getAllExams(): Result<List<Exam>> = runCatching {
        db.collection("exams")
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

    // ── REAL-TIME LISTENER (bonus) ─────────────────────────

    fun listenToSubjects(onChange: (List<Subject>) -> Unit) {
        db.collection("subjects")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val subjects = snapshot.toObjects(Subject::class.java)
                onChange(subjects)
            }
    }
}