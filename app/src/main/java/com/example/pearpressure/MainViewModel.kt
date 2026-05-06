/* MainViewModel.kt
This file handles Firestore logic
Allows frontend to interact with backend via FirestoreRepository.kt  */
package com.example.pearpressure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pearpressure.data.*
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.pearpressure.notifications.AppFirebaseMessagingService

// Ranking scope filter
enum class RankingScope(val label: String) {
    TOTAL("All Time"),
    WEEKLY("This Week")
}

// Ranking page display
data class RankingEntryUi(
    val uid: String,
    val userName: String,
    val totalStudyTimeMs: Long,
    val avgAccuracy: Double = 0.0,
    val efficiencyScore: Double = 0.0,
    val totalWater: Int = 0,
    val totalCoffee: Int = 0,
    val totalEnergy: Int = 0,
    val totalBathroom: Int = 0,
    val avgSleep: Double = 0.0,
    val avgExpectedGrade: Double = 0.0,
    val avgActualGrade: Double = 0.0,
    val currentStreak: Int = 0
)

// Incoming friend request
data class IncomingFriendRequestUi(
    val request: FriendRequest,
    val from: UserProfile
)

// Outgoing friend request
data class OutgoingFriendRequestUi(
    val request: FriendRequest,
    val to: UserProfile
)

// Exam ranking
data class ExamParticipantUi(
    val uid: String,
    val displayName: String,
    val studiedTimeMs: Long
)

// Main logic
class MainViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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

    // Ranking
    private val _selectedRankingSubjectId = MutableStateFlow<String?>(null)
    val selectedRankingSubjectId: StateFlow<String?> = _selectedRankingSubjectId

    private val _rankingEntries = MutableStateFlow<List<RankingEntryUi>>(emptyList())
    val rankingEntries: StateFlow<List<RankingEntryUi>> = _rankingEntries

    // Friends
    private val _friends = MutableStateFlow<List<UserProfile>>(emptyList())
    val friends: StateFlow<List<UserProfile>> = _friends

    private val _filteredFriends = MutableStateFlow<List<UserProfile>>(emptyList())
    val filteredFriends: StateFlow<List<UserProfile>> = _filteredFriends

    private val _incomingRequests = MutableStateFlow<List<IncomingFriendRequestUi>>(emptyList())
    val incomingRequests: StateFlow<List<IncomingFriendRequestUi>> = _incomingRequests

    private val _outgoingRequests = MutableStateFlow<List<OutgoingFriendRequestUi>>(emptyList())
    val outgoingRequests: StateFlow<List<OutgoingFriendRequestUi>> = _outgoingRequests

    // Study buddies
    private val _studyBuddies = MutableStateFlow<List<UserProfile>>(emptyList())
    val studyBuddies: StateFlow<List<UserProfile>> = _studyBuddies

    // Search
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

    // Exam participants for Stopwatch
    private val _examParticipants = MutableStateFlow<List<ExamParticipantUi>>(emptyList())
    val examParticipants: StateFlow<List<ExamParticipantUi>> = _examParticipants

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

    // Activate listeners
    private fun startListening(userId: String) {
        subjectsListeners.forEach { it.remove() }
        friendsListener?.remove()
        incomingReqListener?.remove()
        outgoingReqListener?.remove()

        subjectsListeners = repo.listenToSubjectsForUser(userId) { updatedList ->
            _subjects.value = updatedList

            if (_selectedRankingSubjectId.value == null && updatedList.isNotEmpty()) {
                _selectedRankingSubjectId.value = updatedList.first().id
            }

            refreshStudyBuddiesFromSubjects()
            loadRanking(examId = null, scope = RankingScope.TOTAL)
        }

        startFriendsListeners(userId)

        sessionsListener?.remove()
        sessionsListener = repo.listenToSessionsForUser(userId) {
            _sessions.value = it
        }

        loadCurrentUserProfile()
    }

    // Restart friends listener after change
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


    // ── PROFILE ──────────────────────────────────────────
    // Sign in with email and password
    fun signIn(email: String, pass: String, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepo.signIn(email, pass)
            .onSuccess { user ->
                user?.uid?.let {
                    startListening(it)
                    loadCurrentUserProfile()
                    fetchAndSaveFcmToken()
                }
                onSuccess()
            }
            .onFailure { _error.value = it.message }
    }

    // Sign up with email, password and more information
    fun signUp(email: String, pass: String, onProfileStepRequired: () -> Unit) = viewModelScope.launch {
        _error.value = null

        authRepo.signUp(email, pass)
            .onSuccess { user ->
                if (user != null) {
                    _needsProfileCompletion.value = true
                    onProfileStepRequired()
                    fetchAndSaveFcmToken()
                }
            }
            .onFailure { _error.value = it.message }
    }

    // Sign out, end all listeners and reset values
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
        _currentUserProfile.value = null
        _examParticipants.value = emptyList()

        _selectedRankingSubjectId.value = null
        _friendSearchResult.value = null
        _friendSearchError.value = null

        onSuccess()
    }

    // Complete user profile with inputs
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


    // ── EXAMS/SUBJECTS ──────────────────────────────────────────
    // Listen to exams for a subject
    fun loadExams(subjectId: String) {
        examsListener?.remove()
        examsListener = repo.listenToExams(subjectId) { _exams.value = it }
    }

    // Add subject
    fun addSubject(name: String) = viewModelScope.launch {
        val userId = authRepo.currentUser?.uid ?: return@launch
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return@launch

        repo.addSubject(Subject(name = cleaned, ownerId = userId))
            .onFailure { _error.value = it.message }
    }

    // Add exam
    fun addExam(subjectId: String, title: String, endsAtMs: Long, maxGrade: Double) = viewModelScope.launch {
        val userId = authRepo.currentUser?.uid ?: return@launch
        repo.addExam(
            Exam(
                subjectId = subjectId,
                ownerId = userId,
                title = title,
                endsAtEpochMs = endsAtMs,
                maxGrade = maxGrade
            )
        ).onFailure { _error.value = it.message }
    }

    // Find subject by ID
    fun getSubjectById(id: String): Subject? = _subjects.value.find { it.id == id }

    // Find exam by ID
    fun getExamById(id: String): Exam? = _exams.value.find { it.id == id }

    // Load in exam participants
    fun loadExamParticipants(examId: String) = viewModelScope.launch {
        val exam = _exams.value.firstOrNull { it.id == examId } ?: run {
            _examParticipants.value = emptyList()
            return@launch
        }

        val subject = _subjects.value.firstOrNull { it.id == exam.subjectId } ?: run {
            _examParticipants.value = emptyList()
            return@launch
        }

        val participantUids = (listOf(subject.ownerId) + subject.members)
            .filter { it.isNotBlank() }
            .distinct()

        val examSessions = repo.getSessionsForExamsSync(listOf(examId))

        repo.getUserProfilesByIds(participantUids)
            .onSuccess { profiles ->
                _examParticipants.value = profiles
                    .map { profile ->
                        val studiedMs = examSessions
                            .filter { it.ownerId == profile.uid }
                            .sumOf { it.durationMs }

                        ExamParticipantUi(
                            uid = profile.uid,
                            displayName = profile.username.ifBlank {
                                profile.fullName.ifBlank { profile.email }
                            },
                            studiedTimeMs = studiedMs
                        )
                    }
                    .sortedByDescending { it.studiedTimeMs }
            }
            .onFailure { _error.value = it.message }
    }

    // Delete (owner) or leave (non-owner) subject
    fun deleteOrLeaveSubject(subject: Subject) = viewModelScope.launch {
        val currentUserId = authRepo.currentUser?.uid ?: return@launch
        if (subject.ownerId == currentUserId) {
            repo.deleteSubject(subject.id).onFailure { _error.value = it.message }
        } else {
            repo.leaveSubject(subject.id, currentUserId).onFailure { _error.value = it.message }
        }
    }

    // Add member to subject (owner action)
    fun addMemberToSubject(subjectId: String, userIdToAdd: String) = viewModelScope.launch {
        repo.addMemberToSubject(subjectId, userIdToAdd)
            .onFailure { _error.value = it.message }
    }

    // Delete exam
    fun deleteExam(examId: String) = viewModelScope.launch {
        repo.deleteExam(examId).onFailure { _error.value = it.message }
    }

    // Search for friends
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

    // Edit subject name
    fun updateSubjectName(subjectId: String, newName: String) = viewModelScope.launch {
        val cleaned = newName.trim()
        if (cleaned.isEmpty()) return@launch

        repo.updateSubjectName(subjectId, cleaned)
            .onFailure { _error.value = it.message }
    }

    // Update exam
    fun updateExam(examId: String, newTitle: String, newEndsAtMs: Long, newMaxGrade: Double) = viewModelScope.launch {
        val cleaned = newTitle.trim()
        if (cleaned.isEmpty()) return@launch

        repo.updateExam(examId, cleaned, newEndsAtMs, newMaxGrade)
            .onFailure { _error.value = it.message }
    }


    // ── RANKING ──────────────────────────────────────────
    // Filter by subject
    fun selectRankingSubject(subjectId: String) {
        _selectedRankingSubjectId.value = subjectId
        loadExams(subjectId)
        loadRanking(examId = null, scope = RankingScope.TOTAL)
    }

    // Study buddies (members of a subject)
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

    // Load the entire ranking
    fun loadRanking(
        examId: String? = null,
        scope: RankingScope = RankingScope.TOTAL
    ) = viewModelScope.launch {
        // 1. We FORCE the subjects to be loaded
        //If ist is empty, try to load profiles/subjects first
        if (_subjects.value.isEmpty()) {
        }

        val selectedId = _selectedRankingSubjectId.value

        // 2. We choose ALL subjects(user is either owner or member of it)
        val subjectsToProcess = if (selectedId != null) {
            _subjects.value.filter { it.id == selectedId }
        } else {
            _subjects.value
        }

        // If its still empty after trying to load, we cannot continue
        if (subjectsToProcess.isEmpty()) return@launch

        val allExams = mutableListOf<Exam>()
        subjectsToProcess.forEach { subject ->
            allExams.addAll(repo.getExamsBySubjectSync(subject.id))
        }

        val examIds = if (examId != null) listOf(examId) else allExams.map { it.id }
        var sessions = repo.getSessionsForExamsSync(examIds)

        if (scope == RankingScope.WEEKLY) {
            val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            sessions = sessions.filter { it.createdAtEpochMs >= oneWeekAgo }
        }

        val memberUids = subjectsToProcess.flatMap { listOf(it.ownerId) + it.members }
            .filter { it.isNotBlank() }
            .distinct()

        repo.getUserProfilesByIds(memberUids)
            .onSuccess { profiles ->
                _rankingEntries.value = profiles.map { profile ->
                    val userId = profile.uid
                    val userSessions = sessions.filter { it.ownerId == userId }

                    val totalMs = userSessions.sumOf { it.durationMs }
                    val water = userSessions.sumOf { it.waterCount }
                    val coffee = userSessions.sumOf { it.coffeeCount }
                    val energy = userSessions.sumOf { it.energyDrinkCount }
                    val bathroom = userSessions.sumOf { it.bathroomBreaks }

                    val relevantExams = if (examId != null) allExams.filter { it.id == examId } else allExams

                    fun normalize(value: Double?, max: Double): Double {
                        val actualMax = if (max <= 0.0) 10.0 else max
                        return ((value ?: 0.0) / actualMax) * 10.0
                    }

                    // Sum the difference between grades(your grade/reality gap)
                    var totalGap = 0.0
                    relevantExams.forEach { exam ->
                        val actual = exam.actualGrades[userId]
                        val expected = exam.expectedGrades[userId]

                        if (actual != null && expected != null) {
                            val nActual = normalize(actual, exam.maxGrade)
                            val nExpected = normalize(expected, exam.maxGrade)
                            totalGap += (nActual - nExpected) // just a SUM
                        }
                    }

                    // Maintained the averages for the rest of the app
                    val examsWithActual = relevantExams.filter { it.actualGrades.containsKey(userId) }
                    val avgActual = if (examsWithActual.isNotEmpty()) {
                        examsWithActual.map { normalize(it.actualGrades[userId], it.maxGrade) }.average()
                    } else 0.0

                    val examsWithExpected = relevantExams.filter { it.expectedGrades.containsKey(userId) }
                    val avgExpected = if (examsWithExpected.isNotEmpty()) {
                        examsWithExpected.map { normalize(it.expectedGrades[userId], it.maxGrade) }.average()
                    } else 0.0

                    // LOGIC FOR SLEEP
                    val examsWithSleep = relevantExams.filter { it.sleepHours.containsKey(userId) }
                    val avgSleepVal = if (examsWithSleep.isNotEmpty()) {
                        examsWithSleep.mapNotNull { it.sleepHours[userId] }.average()
                    } else 0.0

                    val totalHours = totalMs / 3600000.0
                    val efficiency = if (totalHours > 0.0027) avgActual / totalHours else 0.0

                    RankingEntryUi( // logic explained in report
                        uid = userId,
                        userName = profile.username.ifBlank { profile.fullName.ifBlank { profile.email } },
                        totalStudyTimeMs = totalMs,
                        currentStreak = profile.currentStreak,
                        avgAccuracy = totalGap, // Sum of the  your guesses for all exams
                        efficiencyScore = efficiency,
                        totalWater = water,
                        totalCoffee = coffee,
                        totalEnergy = energy,
                        totalBathroom = bathroom,
                        avgSleep = avgSleepVal,
                        avgActualGrade = avgActual,
                        avgExpectedGrade = avgExpected
                    )
                }
            }
    }


    // ── FRIENDS ──────────────────────────────────────────
    // Search for a user by email
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

    // Send a friend request
    fun sendFriendRequest(toUid: String) = viewModelScope.launch {
        val fromUid = authRepo.currentUser?.uid ?: return@launch
        repo.sendFriendRequest(fromUid, toUid)
            .onFailure { _error.value = it.message }
    }

    // Accept a friend request
    fun acceptRequest(request: FriendRequest) = viewModelScope.launch {
        repo.acceptFriendRequest(request)
            .onFailure { _error.value = it.message }
    }

    // Decline a friend request
    fun declineRequest(request: FriendRequest) = viewModelScope.launch {
        repo.declineFriendRequest(request)
            .onFailure { _error.value = it.message }
    }

    // Remove a friend
    fun removeFriend(friendUid: String) = viewModelScope.launch {
        val myUid = authRepo.currentUser?.uid ?: return@launch
        repo.removeFriend(myUid, friendUid)
            .onFailure { _error.value = it.message }
    }

    // ── PROFILE PAGE ──────────────────────────────────────────
    // Load in the profile
    fun loadCurrentUserProfile() {
        val uid = authRepo.currentUser?.uid ?: return

        viewModelScope.launch {
            repo.getUserProfile(uid)
                .onSuccess { profile ->
                    if (profile != null) {
                        val now = System.currentTimeMillis()
                        val lastDate = profile.lastStudyDateMs

                        // If the streak is broken, calculating visually if it should be 0(for how its seen in the profile)
                        val calLast = java.util.Calendar.getInstance().apply { timeInMillis = lastDate }

                        // We add a day to the last study
                        calLast.add(java.util.Calendar.DAY_OF_YEAR, 1)

                        // If not today and yesterday either (as we already sum 1), STREAK resets to 0
                        val displayStreak = if (lastDate == 0L) 0
                        else if (isSameDay(now, lastDate) || isNextDay(now, lastDate)) profile.currentStreak
                        else 0

                        //Updating the StateFlow with the profile, with the streak seen correctly
                        _currentUserProfile.value = profile.copy(currentStreak = displayStreak)
                    }
                }
        }
    }


    // ── AUXILIARY FUNCTIONS ──────────────────────────────────────────
    // Helpers to not repeat the code of calendar
    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    // Helpers to not repeat the code of calendar
    private fun isNextDay(now: Long, last: Long): Boolean {
        val calLast = java.util.Calendar.getInstance().apply { timeInMillis = last }
        calLast.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return isSameDay(now, calLast.timeInMillis)
    }
    override fun onCleared() {
        subjectsListeners.forEach { it.remove() }
        examsListener?.remove()
        friendsListener?.remove()
        incomingReqListener?.remove()
        outgoingReqListener?.remove()
        sessionsListener?.remove()
        super.onCleared()
    }


    // ── SESSIONS ───────────────────────────────────────────────
    // Log exam results
    fun saveExamResults(examId: String, expected: Double?, sleep: Double?, actual: Double?) = viewModelScope.launch {
        val userId = authRepo.currentUser?.uid ?: return@launch
        repo.updateExamStats(examId, userId, expected, sleep, actual)
            .onSuccess {
                // Trigger notification when actual grade is submitted
                if (actual != null) {
                    val subjectId = _selectedRankingSubjectId.value ?: ""
                    // implement the actual broadcast logic in the service
                    android.util.Log.d("PEAR_NOTIF", "Sofia: Broadcast to $subjectId that user $userId posted a grade")
                }
                loadCurrentUserProfile()
                loadRanking(_selectedRankingSubjectId.value, RankingScope.TOTAL)
            }
            .onFailure { _error.value = it.message }
    }

    // Save a session
    fun saveSession(
        examId: String,
        durationMs: Long,
        water: Int = 0,
        coffee: Int = 0,
        energy: Int = 0,
        bathroom: Int = 0
    ) = viewModelScope.launch {
        val userId = authRepo.currentUser?.uid ?: return@launch
        val profile = _currentUserProfile.value ?: return@launch // We need the current profile

        // 1. Calculating new streak
        val newStreak = calculateNewStreak(profile.currentStreak, profile.lastStudyDateMs)

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

        // 2. Saving session and updating profile(Streak and total time)
        repo.addSession(session).onFailure { _error.value = it.message }

        // method to upload streak and date
        repo.updateUserStreakAndStats(userId, durationMs, newStreak, System.currentTimeMillis())
            .onSuccess {
                // RELOADS AUTOMATICALLY AFTER STUDYING
                loadCurrentUserProfile() // Reload to see the fire in the UI
                loadRanking(_selectedRankingSubjectId.value, RankingScope.TOTAL)
            }
            .onFailure { _error.value = it.message }
    }

    // Calculate streak in-app
    private fun calculateNewStreak(currentStreak: Int, lastDateMs: Long): Int {
        if (lastDateMs == 0L) return 1 // First time that you study in day

        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        // We use Calendar to compare a natural day (not just exactly 24 hours)
        val calNow = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val calLast = java.util.Calendar.getInstance().apply { timeInMillis = lastDateMs }

        val isSameDay = calNow.get(java.util.Calendar.YEAR) == calLast.get(java.util.Calendar.YEAR) &&
                calNow.get(java.util.Calendar.DAY_OF_YEAR) == calLast.get(java.util.Calendar.DAY_OF_YEAR)

        // Its the next day if the difference of the calendar is 1 day
        calLast.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val isNextDay = calNow.get(java.util.Calendar.YEAR) == calLast.get(java.util.Calendar.YEAR) &&
                calNow.get(java.util.Calendar.DAY_OF_YEAR) == calLast.get(java.util.Calendar.DAY_OF_YEAR)

        return when {
            isSameDay -> currentStreak // Already studied today so keep streak
            isNextDay -> currentStreak + 1 // Streak +1 day
            else -> 1 // More than 48h passed, streak lost. go back to 1
        }
    }


    // ── NOTIFICATIONS ──────────────────────────────────────────
    // FCM token to signal change in database
    private fun fetchAndSaveFcmToken() {
        AppFirebaseMessagingService.fetchCurrentFcmToken { token ->
            saveFcmToken(token)
        }
    }

    // Trigger notification for study_event
    fun notifyStudyStarted(
        subjectId: String,
        subjectName: String,
        examTitle: String
    ) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val userName = when {
            !currentUser.displayName.isNullOrBlank() -> currentUser.displayName!!
            !currentUser.email.isNullOrBlank() -> currentUser.email!!
            else -> "Someone"
        }

        viewModelScope.launch {
            try {
                val event = StudyEvent(
                    fromUserId = userId,
                    fromUserName = userName,
                    subjectId = subjectId,
                    subjectName = subjectName,
                    examTitle = examTitle,
                    startedAtEpochMs = System.currentTimeMillis(),
                    type = "study_started"
                )

                repo.addStudyEvent(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Save FCM token
    fun saveFcmToken(token: String) {
        val currentUser = auth.currentUser ?: run {
            android.util.Log.d("FCM", "No authenticated user, token not saved")
            return
        }

        val uid = currentUser.uid
        android.util.Log.d("FCM", "Saving token for uid=$uid")

        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "fcmToken" to token,
                    "fcmTokenUpdatedAt" to FieldValue.serverTimestamp()
                )

                firestore.collection("users")
                    .document(uid)
                    .set(updates, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                android.util.Log.d("FCM", "Token saved successfully")
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Error saving token", e)
            }
        }
    }
}