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

        // 1. Find exams belonging to the subject
        val examsSnapshot = db.collection("exams")
            .whereEqualTo("subjectId", subjectId)
            .get()
            .await()

        // 2. Delete exams
        val batch = db.batch()
        examsSnapshot.documents.forEach { doc ->
            batch.delete(doc.reference)
        }

        // 3. Delete subject
        val subjectRef = db.collection("subjects").document(subjectId)
        batch.delete(subjectRef)

        // 4. Commit batch
        batch.commit().await()
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

    fun listenToSubjects(onChange: (List<Subject>) -> Unit): ListenerRegistration {
        return db.collection("subjects")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                onChange(snapshot.toObjects(Subject::class.java))
            }
    }

    fun listenToExams(subjectId: String, onChange: (List<Exam>) -> Unit): ListenerRegistration {
        return db.collection("exams")
            .whereEqualTo("subjectId", subjectId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                onChange(snapshot.toObjects(Exam::class.java))
            }
    }
}


