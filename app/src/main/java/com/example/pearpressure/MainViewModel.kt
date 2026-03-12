package com.example.pearpressure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pearpressure.data.FirestoreRepository
import com.example.pearpressure.data.Subject
import com.example.pearpressure.data.Exam
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.ListenerRegistration

class MainViewModel : ViewModel() {

    private val repo = FirestoreRepository()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects

    private val _exams = MutableStateFlow<List<Exam>>(emptyList())
    val exams: StateFlow<List<Exam>> = _exams

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error


    private var subjectsListener: ListenerRegistration? = null
    private var examsListener: ListenerRegistration? = null

    init {
        subjectsListener = repo.listenToSubjects { updatedList -> _subjects.value = updatedList }
    }

    fun loadExams(subjectId: String) {
        examsListener?.remove() // cancel previous before starting new one
        examsListener = repo.listenToExams(subjectId) { updatedList ->
            _exams.value = updatedList
        }
    }

    override fun onCleared() {
        super.onCleared()
        subjectsListener?.remove()
        examsListener?.remove()
    }

    fun addSubject(name: String) = viewModelScope.launch {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return@launch
        
        val subject = Subject(name = cleaned)
        repo.addSubject(subject)
            .onFailure { _error.value = it.message }
    }

    fun addExam(subjectId: String, title: String, endsAtMs: Long) = viewModelScope.launch {
        val cleaned = title.trim()
        if (cleaned.isEmpty()) return@launch

        val exam = Exam(subjectId = subjectId, title = cleaned, endsAtEpochMs = endsAtMs)
        repo.addExam(exam)
            .onFailure { _error.value = it.message }
    }

    fun getSubjectById(id: String): Subject? {
        return _subjects.value.find { it.id == id }
    }

    fun getExamById(id: String): Exam? {
        return _exams.value.find { it.id == id }
    }

    fun deleteSubject(subjectId: String) = viewModelScope.launch {
        repo.deleteSubject(subjectId)
            .onFailure { _error.value = it.message }
    }

    fun deleteExam(examId: String) = viewModelScope.launch {
        repo.deleteExam(examId)
            .onFailure { _error.value = it.message }
    }
}
