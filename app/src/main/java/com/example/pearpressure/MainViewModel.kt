package com.example.pearpressure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pearpressure.data.Exam
import com.example.pearpressure.data.FirestoreRepository
import com.example.pearpressure.data.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.ListenerRegistration
import com.example.pearpressure.data.UserProfile
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

    //authentication logic
    private val authRepo = com.example.pearpressure.data.AuthRepository()

    init {
        // Al arrancar, si hay usuario, empezamos a escuchar sus datos
        authRepo.currentUser?.uid?.let { userId ->
            startListening(userId)
        }
    }

    //FUNCTION to know if the current user is the owner later
    fun getCurrentUserId(): String {
        return authRepo.currentUser?.uid ?: ""
    }

    // Función auxiliar para conectar el listener con el ID del usuario
    private fun startListening(userId: String) {
        subjectsListener?.remove()
        subjectsListener = repo.listenToSubjects(userId) { updatedList ->
            _subjects.value = updatedList
        }
    }

    fun signIn(email: String, pass: String, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepo.signIn(email, pass)
            .onSuccess { user ->
                // Una vez logueado, activamos el listener con su ID
                user?.uid?.let { startListening(it) }
                onSuccess()
            }
            .onFailure { _error.value = it.message }
    }

    fun signUp(email: String, pass: String, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepo.signUp(email, pass)
            .onSuccess { user ->
                user?.let {
                    // Creamos el perfil en Firestore
                    val profile = UserProfile(
                        uid = it.uid,
                        name = email.substringBefore("@"), // Nombre temporal
                        email = email
                    )

                    // Guardamos en la colección "users" usando su UID como ID del documento
                    repo.createUserProfile(profile)

                    startListening(it.uid)
                    onSuccess()
                }
            }
            .onFailure { _error.value = it.message }
    }

    fun isUserLoggedIn(): Boolean = authRepo.currentUser != null

    fun signOut(onSuccess: () -> Unit) {
        authRepo.signOut()
        subjectsListener?.remove()
        examsListener?.remove()
        _subjects.value = emptyList()
        _exams.value = emptyList()
        onSuccess()
    }

    fun getCurrentUserEmail(): String {
        return authRepo.currentUser?.email ?: "No email found"
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
        val userId = authRepo.currentUser?.uid ?: return@launch // Si no hay usuario, no hacemos nada
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return@launch

        val subject = Subject(name = cleaned, ownerId = userId) // <--- Guardamos con el ID del usuario
        repo.addSubject(subject)
            .onFailure { _error.value = it.message }
    }

    fun addExam(subjectId: String, title: String, endsAtMs: Long) = viewModelScope.launch {
        val userId = authRepo.currentUser?.uid ?: return@launch
        val cleaned = title.trim()
        if (cleaned.isEmpty()) return@launch

        val exam = Exam(
            subjectId = subjectId,
            ownerId = userId, //Guardamos con el ID del usuario
            title = cleaned,
            endsAtEpochMs = endsAtMs
        )
        repo.addExam(exam)
            .onFailure { _error.value = it.message }
    }

    fun getSubjectById(id: String): Subject? {
        return _subjects.value.find { it.id == id }
    }

    fun getExamById(id: String): Exam? {
        return _exams.value.find { it.id == id }
    }
    //eliminar subject if owner, leave if non owner
    fun deleteOrLeaveSubject(subject: Subject) = viewModelScope.launch {
        val currentUserId = authRepo.currentUser?.uid ?: return@launch

        if (subject.ownerId == currentUserId) {
            // Si soy el dueño, borro todo (lo que ya tenías)
            repo.deleteSubject(subject.id).onFailure { _error.value = it.message }
        } else {
            // Si no soy el dueño, solo me salgo
            repo.leaveSubject(subject.id, currentUserId).onFailure { _error.value = it.message }
        }
    }

    fun deleteExam(examId: String) = viewModelScope.launch {
        repo.deleteExam(examId)
            .onFailure { _error.value = it.message }
    }
}