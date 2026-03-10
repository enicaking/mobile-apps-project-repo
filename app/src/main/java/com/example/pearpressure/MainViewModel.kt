package com.example.pearpressure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pearpressure.data.Exam
import com.example.pearpressure.data.FirestoreRepository
import com.example.pearpressure.data.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repo = FirestoreRepository()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects

    private val _exams = MutableStateFlow<List<Exam>>(emptyList())
    val exams: StateFlow<List<Exam>> = _exams

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadSubjects() = viewModelScope.launch {
        repo.getSubjects()
            .onSuccess { _subjects.value = it }
            .onFailure { _error.value = it.message }
    }

    fun addSubject(name: String) = viewModelScope.launch {
        val subject = Subject(name = name)
        repo.addSubject(subject)
            .onSuccess { loadSubjects() }
            .onFailure { _error.value = it.message }
    }

    fun loadExams(subjectId: String) = viewModelScope.launch {
        repo.getExamsForSubject(subjectId)
            .onSuccess { _exams.value = it }
            .onFailure { _error.value = it.message }
    }

    fun addExam(subjectId: String, title: String, endsAtMs: Long) = viewModelScope.launch {
        val exam = Exam(subjectId = subjectId, title = title, endsAtEpochMs = endsAtMs)
        repo.addExam(exam)
            .onSuccess { loadExams(subjectId) }
            .onFailure { _error.value = it.message }
    }
}