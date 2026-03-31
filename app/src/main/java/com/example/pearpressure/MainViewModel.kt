package com.example.pearpressure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pearpressure.data.*
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


enum class RankingScope(val label: String) {
    TOTAL("All Time"),
    WEEKLY("This Week")
}
data class RankingEntryUi(
    val uid: String,
    val userName: String,
    val totalStudyTimeMs: Long,
    val avgAccuracy: Double = 0.0,      // Margin of error
    val efficiencyScore: Double = 0.0, // Grade / Hour
    val totalWater: Int = 0,
    val totalCoffee: Int = 0,
    val totalEnergy: Int = 0,
    val totalBathroom: Int = 0,
    //just general average sleep, expected and real grades:
    val avgSleep: Double = 0.0,
    val avgExpectedGrade: Double = 0.0,
    val avgActualGrade: Double = 0.0
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

    private val _needsProfileCompletion = MutableStateFlow(false)
    val needsProfileCompletion: StateFlow<Boolean> = _needsProfileCompletion

    // ── Ranking
    private val _selectedRankingSubjectId = MutableStateFlow<String?>(null)
    val selectedRankingSubjectId: StateFlow<String?> = _selectedRankingSubjectId

    private val _rankingEntries = MutableStateFlow<List<RankingEntryUi>>(emptyList())
    val rankingEntries: StateFlow<List<RankingEntryUi>> = _rankingEntries

    // ── Friends (real friends)
    private val _friends = MutableStateFlow<List<UserProfile>>(emptyList())
    val friends: StateFlow<List<UserProfile>> = _friends

    private val _filteredFriends = MutableStateFlow<List<UserProfile>>(emptyList())
    val filteredFriends: StateFlow<List<UserProfile>> = _filteredFriends

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

    // Profile
    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile

    // Sessions

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions

    private var sessionsListener: ListenerRegistration? = null

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

            refreshStudyBuddiesFromSubjects()

            //Pass default arguments to match the  function signature
            loadRanking(examId = null, scope = RankingScope.TOTAL)
        }

        // friends + requests
        startFriendsListeners(userId)

        // sessions
        sessionsListener?.remove()
        sessionsListener = repo.listenToSessionsForUser(userId) {
            _sessions.value = it
        }
    }


    private fun startFriendsListeners(userId: String) {
        friendsListener?.remove()
        friendsListener = repo.listenFriends(userId) { friendUids ->
            viewModelScope.launch {
                repo.getUserProfilesByIds(friendUids)
                    .onSuccess {
                        _friends.value = it
                        _filteredFriends.value = it
                    }
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

    fun signUp(email: String, pass: String, onProfileStepRequired: () -> Unit) = viewModelScope.launch {
        _error.value = null

        authRepo.signUp(email, pass)
            .onSuccess { user ->
                if (user != null) {
                    _needsProfileCompletion.value = true
                    onProfileStepRequired()
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

    fun addExam(subjectId: String, title: String, endsAtMs: Long, maxGrade: Double) = viewModelScope.launch {
        val userId = authRepo.currentUser?.uid ?: return@launch
        repo.addExam(Exam(
            subjectId = subjectId,
            ownerId = userId,
            title = title,
            endsAtEpochMs = endsAtMs,
            maxGrade = maxGrade // THIS SAVES IT TO DB
        )).onFailure { _error.value = it.message }
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

    fun addMemberToSubject(subjectId: String, userIdToAdd: String) = viewModelScope.launch {
        repo.addMemberToSubject(subjectId, userIdToAdd)
            .onFailure { _error.value = it.message }
    }

    fun deleteExam(examId: String) = viewModelScope.launch {
        repo.deleteExam(examId).onFailure { _error.value = it.message }
    }

    // ── Ranking

    fun selectRankingSubject(subjectId: String) {
        _selectedRankingSubjectId.value = subjectId
        // this line fetches the exams so the dropdown has data immediately
        loadExams(subjectId)
        loadRanking(examId = null, scope = RankingScope.TOTAL)
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

    // Profile
    fun loadCurrentUserProfile() {
        val uid = authRepo.currentUser?.uid ?: return

        viewModelScope.launch {
            repo.getUserProfile(uid)
                .onSuccess { _currentUserProfile.value = it }
        }
    }

    fun getCurrentUserName(): String =
        currentUserProfile.value?.username ?: "No username"

    fun getCurrentUserTime(): Long =
        currentUserProfile.value?.totalStudyTime ?: 10000000


    override fun onCleared() {
        subjectsListeners.forEach { it.remove() }
        examsListener?.remove()
        friendsListener?.remove()
        incomingReqListener?.remove()
        outgoingReqListener?.remove()
        sessionsListener?.remove()
        super.onCleared()
    }



    fun completeUserProfile(
        fullName: String,
        username: String,
        sex: String,
        birthdayEpochMs: Long,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        _error.value = null

        val currentUser = authRepo.currentUser
        if (currentUser == null) {
            _error.value = "No logged user found"
            return@launch
        }

        val cleanFullName = fullName.trim()
        val cleanUsername = username.trim().lowercase()
        val cleanSex = sex.trim()

        if (cleanFullName.isEmpty()) {
            _error.value = "Full name is required"
            return@launch
        }

        if (cleanUsername.isEmpty()) {
            _error.value = "Username is required"
            return@launch
        }

        if (cleanSex.isEmpty()) {
            _error.value = "Sex is required"
            return@launch
        }

        if (birthdayEpochMs <= 0L) {
            _error.value = "Birthday is required"
            return@launch
        }

        repo.isUsernameAvailable(cleanUsername)
            .onSuccess { available ->
                if (!available) {
                    _error.value = "This username is already taken"
                    return@onSuccess
                }

                val profile = UserProfile(
                    uid = currentUser.uid,
                    fullName = cleanFullName,
                    username = cleanUsername,
                    sex = cleanSex,
                    birthdayEpochMs = birthdayEpochMs,
                    email = currentUser.email ?: "",
                    totalStudyTime = 0L
                )

                repo.saveCompletedUserProfile(profile)
                    .onSuccess {
                        _needsProfileCompletion.value = false
                        startListening(currentUser.uid)
                        onSuccess()
                    }
                    .onFailure { _error.value = it.message }
            }
            .onFailure { _error.value = it.message }
    }

    fun shouldCompleteProfile(): Boolean = _needsProfileCompletion.value

    // ── Adding member actions

    fun searchFriends(query: String) {
        val currentFriends = _friends.value

        _filteredFriends.value = if (query.isBlank()) {
            currentFriends
        } else {
            currentFriends.filter {
                it.fullName.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true) ||
                        it.username.contains(query, ignoreCase = true)
            }
        }
    }

    // Función requerida por ExamsScreen en TestApp ---
    fun saveExamResults(examId: String, expected: Double?, sleep: Double?, actual: Double?) = viewModelScope.launch {
        val userId = authRepo.currentUser?.uid ?: return@launch
        // To change wrong info, the user just submits new values.
        // To "delete", we assume the UI will allow passing null/empty.
        repo.updateExamStats(examId, userId, expected, sleep, actual)
            .onSuccess {
                // Refresh data so the Ranking changes immediately
                loadRanking(_selectedRankingSubjectId.value, RankingScope.TOTAL)
            }
            .onFailure { _error.value = it.message }
    }

    //FUNCTION TO SAVE SESSION
    fun saveSession(
        examId: String,
        durationMs: Long,
        water: Int = 0,
        coffee: Int = 0,
        energy: Int = 0,
        bathroom: Int = 0
    ) = viewModelScope.launch {
        val userId = authRepo.currentUser?.uid ?: return@launch

        val session = Session(
            ownerId = userId,
            examId = examId,
            durationMs = durationMs,
            createdAtEpochMs = System.currentTimeMillis(),
            waterCount = water,
            coffeeCount = coffee,
            energyDrinkCount = energy,
            bathroomBreaks = bathroom
        )
        // Add session into firestore
        repo.addSession(session).onFailure { _error.value = it.message }

        // Add this session to user's total study time
        repo.updateUserTotalStudyTime(userId, durationMs)
            .onFailure { _error.value = it.message }

        loadCurrentUserProfile() // Refresh so that the totals are updated
    }

    // Update logic for Subjects and Exams
    fun updateSubjectName(subjectId: String, newName: String) = viewModelScope.launch {
        val cleaned = newName.trim()
        if (cleaned.isEmpty()) return@launch

        repo.updateSubjectName(subjectId, cleaned)
            .onFailure { _error.value = it.message }
    }

    fun updateExam(examId: String, newTitle: String, newEndsAtMs: Long, newMaxGrade: Double) = viewModelScope.launch {
        // Keep your check: don't allow empty titles
        val cleaned = newTitle.trim()
        if (cleaned.isEmpty()) return@launch

        // Pass the newMaxGrade to the repo so it actually updates in Firestore
        repo.updateExam(examId, cleaned, newEndsAtMs, newMaxGrade)
            .onFailure { _error.value = it.message }
    }

    // ALL THE RANKING LOGIC HERE
    fun loadRanking(
        examId: String? = null,
        scope: RankingScope = RankingScope.TOTAL
    ) = viewModelScope.launch {
        val subjectId = _selectedRankingSubjectId.value ?: run {
            _rankingEntries.value = emptyList(); return@launch
        }

        val subject = _subjects.value.firstOrNull { it.id == subjectId } ?: run {
            _rankingEntries.value = emptyList(); return@launch
        }

        // 1. Get all exams for this subject
        val exams = repo.getExamsBySubjectSync(subjectId)

        // 2. FILTER SESSIONS BASED ON SELECTION
        val examIds = if (examId != null) listOf(examId) else exams.map { it.id }
        var sessions = repo.getSessionsForExamsSync(examIds)

        // Apply Weekly Filter if selected
        if (scope == RankingScope.WEEKLY) {
            val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            sessions = sessions.filter { it.createdAtEpochMs >= oneWeekAgo }
        }

        // 3. Get all member profiles
        val memberUids = (listOf(subject.ownerId) + subject.members)
            .filter { it.isNotBlank() }
            .distinct()

        repo.getUserProfilesByIds(memberUids)
            .onSuccess { profiles ->
                _rankingEntries.value = profiles.map { profile ->
                    val userId = profile.uid
                    val userSessions = sessions.filter { it.ownerId == userId }

                    // --- HABITS & TIME ---
                    val totalMs = userSessions.sumOf { it.durationMs }
                    val water = userSessions.sumOf { it.waterCount }
                    val coffee = userSessions.sumOf { it.coffeeCount }
                    val energy = userSessions.sumOf { it.energyDrinkCount }
                    val bathroom = userSessions.sumOf { it.bathroomBreaks }

                    // --- EXAM DATA ---
                    val relevantExams = if (examId != null) exams.filter { it.id == examId } else exams
                    // Only count exams where the user actually entered an actual grade
                    val completedExams = relevantExams.filter { it.actualGrades.containsKey(userId) }

                    // We sum the points for "All Exams" view to see total Reality Gap
                    val sumActual = completedExams.sumOf { it.actualGrades[userId] ?: 0.0 }
                    val sumExpected = completedExams.sumOf { it.expectedGrades[userId] ?: 0.0 }
                    val avgSleep = if (completedExams.isNotEmpty()) completedExams.map { it.sleepHours[userId] ?: 0.0 }.average() else 0.0

                    // --- STUDY EFFICIENCY ---
                    val totalHours = totalMs / 3600000.0
                    // Safety check: Only calculate if studied more than 10 seconds (0.0027 hours)
                    // This allows low time to show up while blocking 0-second errors.
                    val efficiency = if (totalHours > 0.0027) sumActual / totalHours else 0.0
                    RankingEntryUi(
                        uid = userId,
                        userName = profile.username.ifBlank { profile.fullName.ifBlank { profile.email } },
                        totalStudyTimeMs = totalMs,
                        avgAccuracy = 0.0, // Deprecated in favor of Reality Gap
                        efficiencyScore = efficiency,
                        totalWater = water,
                        totalCoffee = coffee,
                        totalEnergy = energy,
                        totalBathroom = bathroom,
                        avgSleep = avgSleep,
                        avgActualGrade = sumActual, // This is now a SUM
                        avgExpectedGrade = sumExpected // This is now a SUM
                    )
                }
            }
            .onFailure { _error.value = it.message }
    }

}