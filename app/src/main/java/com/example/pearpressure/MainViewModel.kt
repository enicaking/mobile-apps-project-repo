package com.example.pearpressure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pearpressure.data.*
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RankingEntryUi(
    val uid: String,
    val userName: String,
    val totalStudyTimeMs: Long
)

data class IncomingFriendRequestUi(
    val request: FriendRequest,
    val from: UserProfile
)

data class OutgoingFriendRequestUi(
    val request: FriendRequest,
    val to: UserProfile
)

class MainViewModel : ViewModel() {

    private val repo = FirestoreRepository()
    private val authRepo = AuthRepository()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects

    private val _exams = MutableStateFlow<List<Exam>>(emptyList())
    val exams: StateFlow<List<Exam>> = _exams

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // ── Ranking
    private val _selectedRankingSubjectId = MutableStateFlow<String?>(null)
    val selectedRankingSubjectId: StateFlow<String?> = _selectedRankingSubjectId

    private val _rankingEntries = MutableStateFlow<List<RankingEntryUi>>(emptyList())
    val rankingEntries: StateFlow<List<RankingEntryUi>> = _rankingEntries

    // ── Friends (real friends)
    private val _friends = MutableStateFlow<List<UserProfile>>(emptyList())
    val friends: StateFlow<List<UserProfile>> = _friends

    private val _incomingRequests = MutableStateFlow<List<IncomingFriendRequestUi>>(emptyList())
    val incomingRequests: StateFlow<List<IncomingFriendRequestUi>> = _incomingRequests

    private val _outgoingRequests = MutableStateFlow<List<OutgoingFriendRequestUi>>(emptyList())
    val outgoingRequests: StateFlow<List<OutgoingFriendRequestUi>> = _outgoingRequests

    // ── Study buddies (derived from subjects members/owner)
    private val _studyBuddies = MutableStateFlow<List<UserProfile>>(emptyList())
    val studyBuddies: StateFlow<List<UserProfile>> = _studyBuddies

    // ── Search
    private val _friendSearchResult = MutableStateFlow<UserProfile?>(null)
    val friendSearchResult: StateFlow<UserProfile?> = _friendSearchResult

    private val _friendSearchError = MutableStateFlow<String?>(null)
    val friendSearchError: StateFlow<String?> = _friendSearchError

    // listeners
    private var subjectsListeners: List<ListenerRegistration> = emptyList()
    private var examsListener: ListenerRegistration? = null

    private var friendsListener: ListenerRegistration? = null
    private var incomingReqListener: ListenerRegistration? = null
    private var outgoingReqListener: ListenerRegistration? = null

    init {
        authRepo.currentUser?.uid?.let { startListening(it) }
    }

    fun isUserLoggedIn(): Boolean = authRepo.currentUser != null
    fun getCurrentUserId(): String = authRepo.currentUser?.uid ?: ""
    fun getCurrentUserEmail(): String = authRepo.currentUser?.email ?: "No email"

    private fun startListening(userId: String) {
        // stop old listeners
        subjectsListeners.forEach { it.remove() }
        friendsListener?.remove()
        incomingReqListener?.remove()
        outgoingReqListener?.remove()

        // subjects (owner + member)
        subjectsListeners = repo.listenToSubjectsForUser(userId) { updatedList ->
            _subjects.value = updatedList

            if (_selectedRankingSubjectId.value == null && updatedList.isNotEmpty()) {
                _selectedRankingSubjectId.value = updatedList.first().id
            }

            // derived lists
            refreshStudyBuddiesFromSubjects()
            loadRanking()
        }

        // friends + requests
        startFriendsListeners(userId)
    }

    private fun startFriendsListeners(userId: String) {
        friendsListener?.remove()
        friendsListener = repo.listenFriends(userId) { friendUids ->
            viewModelScope.launch {
                repo.getUserProfilesByIds(friendUids)
                    .onSuccess { _friends.value = it }
                    .onFailure { _error.value = it.message }
            }
        }

        incomingReqListener?.remove()
        incomingReqListener = repo.listenIncomingFriendRequests(userId) { requests ->
            viewModelScope.launch {
                val fromUids = requests.map { it.fromUid }.distinct()
                repo.getUserProfilesByIds(fromUids)
                    .onSuccess { profiles ->
                        val map = profiles.associateBy { it.uid }
                        _incomingRequests.value = requests
                            .mapNotNull { r ->
                                val from = map[r.fromUid] ?: return@mapNotNull null
                                IncomingFriendRequestUi(r, from)
                            }
                    }
                    .onFailure { _error.value = it.message }
            }
        }

        outgoingReqListener?.remove()
        outgoingReqListener = repo.listenOutgoingFriendRequests(userId) { requests ->
            viewModelScope.launch {
                val toUids = requests.map { it.toUid }.distinct()
                repo.getUserProfilesByIds(toUids)
                    .onSuccess { profiles ->
                        val map = profiles.associateBy { it.uid }
                        _outgoingRequests.value = requests
                            .mapNotNull { r ->
                                val to = map[r.toUid] ?: return@mapNotNull null
                                OutgoingFriendRequestUi(r, to)
                            }
                    }
                    .onFailure { _error.value = it.message }
            }
        }
    }

    // ── Auth

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
        friendsListener?.remove()
        incomingReqListener?.remove()
        outgoingReqListener?.remove()

        _subjects.value = emptyList()
        _exams.value = emptyList()
        _rankingEntries.value = emptyList()
        _friends.value = emptyList()
        _incomingRequests.value = emptyList()
        _outgoingRequests.value = emptyList()
        _studyBuddies.value = emptyList()

        _selectedRankingSubjectId.value = null
        _friendSearchResult.value = null
        _friendSearchError.value = null

        onSuccess()
    }

    // ── Exams/Subjects

    fun loadExams(subjectId: String) {
        examsListener?.remove()
        examsListener = repo.listenToExams(subjectId) { _exams.value = it }
    }

    fun addSubject(name: String) = viewModelScope.launch {
        val userId = authRepo.currentUser?.uid ?: return@launch
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return@launch

        repo.addSubject(Subject(name = cleaned, ownerId = userId))
            .onFailure { _error.value = it.message }
    }

    fun addExam(subjectId: String, title: String, endsAtMs: Long) = viewModelScope.launch {
        val userId = authRepo.currentUser?.uid ?: return@launch
        val cleaned = title.trim()
        if (cleaned.isEmpty()) return@launch

        repo.addExam(
            Exam(
                subjectId = subjectId,
                ownerId = userId,
                title = cleaned,
                endsAtEpochMs = endsAtMs
            )
        ).onFailure { _error.value = it.message }
    }

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

    // ── Ranking

    fun selectRankingSubject(subjectId: String) {
        _selectedRankingSubjectId.value = subjectId
        loadRanking()
    }

    fun loadRanking() = viewModelScope.launch {
        val subjectId = _selectedRankingSubjectId.value ?: run {
            _rankingEntries.value = emptyList(); return@launch
        }

        val subject = _subjects.value.firstOrNull { it.id == subjectId } ?: run {
            _rankingEntries.value = emptyList(); return@launch
        }

        val memberUids = (listOf(subject.ownerId) + subject.members)
            .filter { it.isNotBlank() }
            .distinct()

        repo.getUserProfilesByIds(memberUids)
            .onSuccess { profiles ->
                _rankingEntries.value = profiles
                    .map {
                        val display = when {
                            it.name.isNotBlank() -> it.name
                            it.email.isNotBlank() -> it.email
                            else -> it.uid
                        }
                        RankingEntryUi(it.uid, display, it.totalStudyTime)
                    }
                    .sortedByDescending { it.totalStudyTimeMs }
            }
            .onFailure { _error.value = it.message }
    }

    // ── Study buddies (from subjects)

    private fun refreshStudyBuddiesFromSubjects() = viewModelScope.launch {
        val currentUid = authRepo.currentUser?.uid ?: return@launch

        val allUids = _subjects.value
            .flatMap { s -> listOf(s.ownerId) + s.members }
            .filter { it.isNotBlank() }
            .distinct()
            .filter { it != currentUid }

        repo.getUserProfilesByIds(allUids)
            .onSuccess { _studyBuddies.value = it }
            .onFailure { _error.value = it.message }
    }

    // ── Friends actions

    fun searchUserByEmail(email: String) = viewModelScope.launch {
        _friendSearchError.value = null
        _friendSearchResult.value = null

        val cleaned = email.trim()
        if (cleaned.isEmpty()) return@launch

        repo.findUserByEmail(cleaned)
            .onSuccess { user ->
                if (user == null) _friendSearchError.value = "No user found with that email."
                else _friendSearchResult.value = user
            }
            .onFailure { e ->
                _friendSearchError.value = e.message ?: "Error searching user."
            }
    }

    fun sendFriendRequest(toUid: String) = viewModelScope.launch {
        val fromUid = authRepo.currentUser?.uid ?: return@launch
        repo.sendFriendRequest(fromUid, toUid)
            .onFailure { _error.value = it.message }
    }

    fun acceptRequest(request: FriendRequest) = viewModelScope.launch {
        repo.acceptFriendRequest(request)
            .onFailure { _error.value = it.message }
    }

    fun declineRequest(request: FriendRequest) = viewModelScope.launch {
        repo.declineFriendRequest(request)
            .onFailure { _error.value = it.message }
    }

    fun removeFriend(friendUid: String) = viewModelScope.launch {
        val myUid = authRepo.currentUser?.uid ?: return@launch
        repo.removeFriend(myUid, friendUid)
            .onFailure { _error.value = it.message }
    }

    override fun onCleared() {
        subjectsListeners.forEach { it.remove() }
        examsListener?.remove()
        friendsListener?.remove()
        incomingReqListener?.remove()
        outgoingReqListener?.remove()
        super.onCleared()
    }
}