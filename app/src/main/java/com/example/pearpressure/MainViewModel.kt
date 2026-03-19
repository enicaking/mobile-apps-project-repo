package com.example.pearpressure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pearpressure.data.Exam
import com.example.pearpressure.data.FirestoreRepository
import com.example.pearpressure.data.Subject
import com.example.pearpressure.data.UserProfile
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RankingEntryUi(
    val uid: String,
    val userName: String,
    val totalStudyTimeMs: Long
)

class MainViewModel : ViewModel() {

    private val repo = FirestoreRepository()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects

    private val _exams = MutableStateFlow<List<Exam>>(emptyList())
    val exams: StateFlow<List<Exam>> = _exams

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // ✅ Ranking state
    private val _selectedRankingSubjectId = MutableStateFlow<String?>(null)
    val selectedRankingSubjectId: StateFlow<String?> = _selectedRankingSubjectId

    private val _rankingEntries = MutableStateFlow<List<RankingEntryUi>>(emptyList())
    val rankingEntries: StateFlow<List<RankingEntryUi>> = _rankingEntries

    // ✅ Friends state
    private val _friends = MutableStateFlow<List<UserProfile>>(emptyList())
    val friends: StateFlow<List<UserProfile>> = _friends

    private val _friendSearchResult = MutableStateFlow<UserProfile?>(null)
    val friendSearchResult: StateFlow<UserProfile?> = _friendSearchResult

    private val _friendSearchError = MutableStateFlow<String?>(null)
    val friendSearchError: StateFlow<String?> = _friendSearchError

    // listeners
    private var subjectsListeners: List<ListenerRegistration> = emptyList()
    private var examsListener: ListenerRegistration? = null

    // authentication logic
    private val authRepo = com.example.pearpressure.data.AuthRepository()

    init {
        authRepo.currentUser?.uid?.let { userId ->
            startListening(userId)
        }
    }

    fun getCurrentUserId(): String = authRepo.currentUser?.uid ?: ""

    fun getCurrentUserEmail(): String = authRepo.currentUser?.email ?: "No email found"

    fun isUserLoggedIn(): Boolean = authRepo.currentUser != null

    private fun startListening(userId: String) {
        // stop old listeners
        subjectsListeners.forEach { it.remove() }
        subjectsListeners = repo.listenToSubjectsForUser(userId) { updatedList ->
            _subjects.value = updatedList

            // Default ranking subject if none selected yet
            if (_selectedRankingSubjectId.value == null && updatedList.isNotEmpty()) {
                _selectedRankingSubjectId.value = updatedList.first().id
            }

            // Keep friends + ranking updated when subjects change
            refreshFriendsFromSubjects()
            loadRanking()
        }
    }

    fun signIn(email: String, pass: String, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepo.signIn(email, pass)
            .onSuccess { user ->
                user?.uid?.let { startListening(it) }
                onSuccess()
            }
            .onFailure { _error.value = it.message }
    }

    fun signUp(email: String, pass: String, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepo.signUp(email, pass)
            .onSuccess { user ->
                user?.let {
                    val profile = UserProfile(
                        uid = it.uid,
                        name = email.substringBefore("@"),
                        email = email,
                        totalStudyTime = 0L
                    )
                    repo.createUserProfile(profile)
                    startListening(it.uid)
                    onSuccess()
                }
            }
            .onFailure { _error.value = it.message }
    }

    fun signOut(onSuccess: () -> Unit) {
        authRepo.signOut()
        subjectsListeners.forEach { it.remove() }
        examsListener?.remove()

        _subjects.value = emptyList()
        _exams.value = emptyList()
        _rankingEntries.value = emptyList()
        _friends.value = emptyList()
        _selectedRankingSubjectId.value = null
        _friendSearchResult.value = null
        _friendSearchError.value = null

        onSuccess()
    }

    fun loadExams(subjectId: String) {
        examsListener?.remove()
        examsListener = repo.listenToExams(subjectId) { updatedList ->
            _exams.value = updatedList
        }
    }
    //quita el listener anterior
    //empieza a escuchar los exámenes de esa asignatura

    fun addSubject(name: String) = viewModelScope.launch {
        val userId = authRepo.currentUser?.uid ?: return@launch
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return@launch

        val subject = Subject(name = cleaned, ownerId = userId)
        repo.addSubject(subject).onFailure { _error.value = it.message }
    }

    fun addExam(subjectId: String, title: String, endsAtMs: Long) = viewModelScope.launch {
        val userId = authRepo.currentUser?.uid ?: return@launch
        val cleaned = title.trim()
        if (cleaned.isEmpty()) return@launch

        val exam = Exam(
            subjectId = subjectId,
            ownerId = userId,
            title = cleaned,
            endsAtEpochMs = endsAtMs
        )
        repo.addExam(exam).onFailure { _error.value = it.message }
    }

    fun getSubjectById(id: String): Subject? = _subjects.value.find { it.id == id }
    //Esto se usa para mostrar el nombre de la asignatura arriba del crono.
    fun getExamById(id: String): Exam? = _exams.value.find { it.id == id }
    //Sirve para recuperar el examen elegido cuando vas a abrir el cronómetro.

    fun deleteOrLeaveSubject(subject: Subject) = viewModelScope.launch {
        val currentUserId = authRepo.currentUser?.uid ?: return@launch

        if (subject.ownerId == currentUserId) {
            repo.deleteSubject(subject.id).onFailure { _error.value = it.message }
        } else {
            repo.leaveSubject(subject.id, currentUserId).onFailure { _error.value = it.message }
        }
    }

    fun deleteExam(examId: String) = viewModelScope.launch {
        repo.deleteExam(examId).onFailure { _error.value = it.message }
    }

    // ── RANKING ───────────────────────────────────────────

    fun selectRankingSubject(subjectId: String) {
        _selectedRankingSubjectId.value = subjectId
        loadRanking()
    }

    fun loadRanking() = viewModelScope.launch {
        val subjectId = _selectedRankingSubjectId.value ?: run {
            _rankingEntries.value = emptyList()
            return@launch
        }

        val subject = _subjects.value.firstOrNull { it.id == subjectId } ?: run {
            _rankingEntries.value = emptyList()
            return@launch
        }

        val memberUids = (listOf(subject.ownerId) + subject.members)
            .filter { it.isNotBlank() }
            .distinct()

        repo.getUserProfilesByIds(memberUids)
            .onSuccess { profiles ->
                val entries = profiles
                    .map {
                        val name = when {
                            it.name.isNotBlank() -> it.name
                            it.email.isNotBlank() -> it.email
                            else -> it.uid
                        }
                        RankingEntryUi(it.uid, name, it.totalStudyTime)
                    }
                    .sortedByDescending { it.totalStudyTimeMs }

                _rankingEntries.value = entries
            }
            .onFailure { _error.value = it.message }
    }

    // ── FRIENDS (study buddies via shared subjects) ───────

    private fun refreshFriendsFromSubjects() = viewModelScope.launch {
        val currentUid = authRepo.currentUser?.uid ?: return@launch

        val allUids = _subjects.value
            .flatMap { s -> listOf(s.ownerId) + s.members }
            .filter { it.isNotBlank() }
            .distinct()
            .filter { it != currentUid }

        repo.getUserProfilesByIds(allUids)
            .onSuccess { profiles -> _friends.value = profiles }
            .onFailure { _error.value = it.message }
    }

    fun searchUserByEmail(email: String) = viewModelScope.launch {
        _friendSearchError.value = null
        _friendSearchResult.value = null

        val cleaned = email.trim()
        if (cleaned.isEmpty()) return@launch

        repo.findUserByEmail(cleaned)
            .onSuccess { user ->
                if (user == null) _friendSearchError.value = "No existe un usuario con ese email"
                else _friendSearchResult.value = user
            }
            .onFailure { e ->
                _friendSearchError.value = e.message ?: "Error buscando usuario"
            }
    }

    override fun onCleared() {
        super.onCleared()
        subjectsListeners.forEach { it.remove() }
        examsListener?.remove()
    }
}