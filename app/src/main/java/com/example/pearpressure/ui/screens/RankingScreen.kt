package com.example.pearpressure.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pearpressure.MainViewModel
import com.example.pearpressure.RankingEntryUi
import com.example.pearpressure.RankingScope

// 2. Ranking Types (Dropdown)
enum class RankingCategory(val label: String, val unit: String) {
    HARD_WORK("Hard Work (Time)", ""),
    REALITY_GAP("Your Guess", "pts"),
    STUDY_EFFICIENCY("Study Efficiency", "pts/hr"), // UPDATED NAME
    HABIT_WATER("Water Intake", "glasses"),
    HABIT_COFFEE("Coffee Consumed", "cups"),
    HABIT_ENERGY("Energy Drinks", "cans"),
    HABIT_BATHROOM("Bathroom Breaks", "breaks"),

    // FIXED: Removed the hardcoded "/10" so it doesn't conflict with your new maxGrade system
    GRADE_ACTUAL("Final Grade", ""),
    GRADE_EXPECTED("Expected Grade", ""),
    SLEEP("Sleep", "hrs")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(viewModel: MainViewModel = viewModel()) {
    // Collect State from ViewModel
    val subjects by viewModel.subjects.collectAsState()
    val exams by viewModel.exams.collectAsState()
    val selectedSubjectId by viewModel.selectedRankingSubjectId.collectAsState()
    val entries by viewModel.rankingEntries.collectAsState()

    // NEW: Collect friend and request states
    val friends by viewModel.friends.collectAsState()
    val outgoingRequests by viewModel.outgoingRequests.collectAsState()
    val currentUserId = viewModel.getCurrentUserId()

    // UI State for Filters
    var selectedScope by remember { mutableStateOf(RankingScope.TOTAL) }
    var selectedCategory by remember { mutableStateOf(RankingCategory.HARD_WORK) }
    var selectedExamId by remember { mutableStateOf<String?>(null) }

    // Dropdown Visibility States
    var categoryExpanded by remember { mutableStateOf(false) }
    var examExpanded by remember { mutableStateOf(false) }
    var subjectPickerExpanded by remember { mutableStateOf(false) }

    // UI State for Friend Confirmation
    var userToConfirm by remember { mutableStateOf<RankingEntryUi?>(null) }

    // CARGA AUTOMÁTICA DE EXÁMENES
    // Si ya hay una asignatura seleccionada al entrar, cargamos sus exámenes
    LaunchedEffect(selectedSubjectId) {
        selectedSubjectId?.let { id ->
            viewModel.loadExams(id)
        }
    }
    // --- AUTOMATIC REFRESH ---
    // Triggers whenever Subject, Exam, or Scope changes
    LaunchedEffect(selectedSubjectId, selectedExamId, selectedScope) {
        viewModel.loadRanking(examId = selectedExamId, scope = selectedScope)
    }

    val currentSubject = subjects.firstOrNull { it.id == selectedSubjectId }

    // Find selected exam to know the max scale for display
    val currentSelectedExam = exams.find { it.id == selectedExamId }

    val hasMyExpectedGrade = currentSelectedExam?.expectedGrades?.containsKey(currentUserId) == true
    val hasMyFinalGrade = currentSelectedExam?.actualGrades?.containsKey(currentUserId) == true
    val hasMySleepHours = currentSelectedExam?.sleepHours?.containsKey(currentUserId) == true

    val availableCategories = RankingCategory.entries.filter { category ->
        when (category) {
            RankingCategory.HARD_WORK -> true
            RankingCategory.STUDY_EFFICIENCY -> true
            RankingCategory.HABIT_WATER -> true
            RankingCategory.HABIT_COFFEE -> true
            RankingCategory.HABIT_ENERGY -> true
            RankingCategory.HABIT_BATHROOM -> true

            RankingCategory.REALITY_GAP ->
                selectedExamId != null && hasMyExpectedGrade && hasMyFinalGrade

            RankingCategory.GRADE_EXPECTED ->
                selectedExamId != null && hasMyExpectedGrade

            RankingCategory.GRADE_ACTUAL ->
                selectedExamId != null && hasMyFinalGrade

            RankingCategory.SLEEP ->
                selectedExamId != null && hasMySleepHours
        }
    }

    LaunchedEffect(
        selectedExamId,
        hasMyExpectedGrade,
        hasMyFinalGrade,
        hasMySleepHours
    ) {
        if (selectedCategory !in availableCategories) {
            selectedCategory = RankingCategory.HARD_WORK
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Subject Header Card
            OutlinedCard(
                onClick = { subjectPickerExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Subject", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = currentSubject?.name ?: "Select Subject",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("Change", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }

            // 2. Time Scope Segmented Toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                RankingScope.entries.forEachIndexed { index, scope ->
                    SegmentedButton(
                        selected = selectedScope == scope,
                        onClick = { selectedScope = scope },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                    ) {
                        Text(scope.label)
                    }
                }
            }

            // 3. Dual Dropdowns Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // EXAM FILTER DROPDOWN
                ExposedDropdownMenuBox(
                    expanded = examExpanded,
                    onExpandedChange = { examExpanded = !examExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = exams.find { it.id == selectedExamId }?.title ?: "All Exams",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Exam") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = examExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = examExpanded,
                        onDismissRequest = { examExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Exams (Total)") },
                            onClick = {
                                selectedExamId = null
                                selectedCategory = RankingCategory.HARD_WORK
                                examExpanded = false
                            }
                        )
                        exams.forEach { exam ->
                            DropdownMenuItem(
                                text = { Text(exam.title) },
                                onClick = { selectedExamId = exam.id; examExpanded = false }
                            )
                        }
                    }
                }

                // CATEGORY/METRIC DROPDOWN
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedCategory.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Metric") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        availableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 4. Ranking List
            // We sort by the selected category here to ensure the order is correct
            val sortedEntries = remember(entries, selectedCategory) {
                when (selectedCategory) {
                    RankingCategory.HARD_WORK -> entries.sortedByDescending { it.totalStudyTimeMs }
                    RankingCategory.REALITY_GAP -> entries.sortedByDescending { it.avgActualGrade - it.avgExpectedGrade }
                    RankingCategory.STUDY_EFFICIENCY -> entries.sortedByDescending { it.efficiencyScore }
                    RankingCategory.HABIT_WATER -> entries.sortedByDescending { it.totalWater }
                    RankingCategory.HABIT_COFFEE -> entries.sortedByDescending { it.totalCoffee }
                    RankingCategory.HABIT_ENERGY -> entries.sortedByDescending { it.totalEnergy }
                    RankingCategory.HABIT_BATHROOM -> entries.sortedByDescending { it.totalBathroom }
                    RankingCategory.GRADE_ACTUAL -> entries.sortedByDescending { it.avgActualGrade }
                    RankingCategory.GRADE_EXPECTED -> entries.sortedByDescending { it.avgExpectedGrade }
                    RankingCategory.SLEEP -> entries.sortedByDescending { it.avgSleep }
                }
            }

            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No data available for this selection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(sortedEntries) { index, entry ->
                        // NEW: Check friendship and request status
                        val isFriend = friends.any { it.uid == entry.uid }
                        val requestSent = outgoingRequests.any { it.to.uid == entry.uid }

                        RankingRow(
                            rank = index + 1,
                            entry = entry,
                            category = selectedCategory,
                            maxGrade = currentSelectedExam?.maxGrade ?: 10.0,
                            isAllExams = selectedExamId == null,
                            isCurrentUser = entry.uid == currentUserId,
                            isAlreadyFriend = isFriend,
                            isRequestPending = requestSent,
                            onAddFriend = { userToConfirm = entry } // Open confirmation dialog
                        )
                    }
                }
            }
        }
    }

    // --- FRIEND REQUEST CONFIRMATION DIALOG ---
    userToConfirm?.let { entry ->
        AlertDialog(
            onDismissRequest = { userToConfirm = null },
            title = { Text("Add Friend") },
            text = { Text("Do you want to send a friend request to ${entry.userName}?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.sendFriendRequest(entry.uid)
                    userToConfirm = null
                }) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = { userToConfirm = null }) { Text("Cancel") }
            }
        )
    }

    // --- SUBJECT PICKER DIALOG ---
    if (subjectPickerExpanded) {
        AlertDialog(
            onDismissRequest = { subjectPickerExpanded = false },
            title = { Text("Select Subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjects.forEach { s ->
                        OutlinedButton(
                            onClick = {
                                selectedExamId = null
                                selectedCategory = RankingCategory.HARD_WORK
                                viewModel.selectRankingSubject(s.id)
                                subjectPickerExpanded = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(s.name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { subjectPickerExpanded = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RankingRow(
    rank: Int,
    entry: RankingEntryUi,
    category: RankingCategory,
    maxGrade: Double,
    isAllExams: Boolean,
    isCurrentUser: Boolean,
    isAlreadyFriend: Boolean,
    isRequestPending: Boolean,
    onAddFriend: () -> Unit
) {
    // Determine Podium Colors
    val rowColor = when (rank) {
        1 -> Color(0xFFFFD700).copy(alpha = 0.15f) // Gold
        2 -> Color(0xFFC0C0C0).copy(alpha = 0.15f) // Silver
        3 -> Color(0xFFCD7F32).copy(alpha = 0.15f) // Bronze
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = rowColor),
        border = if (rank <= 3) androidx.compose.foundation.BorderStroke(2.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(45.dp),
                color = if (rank <= 3) borderColor else MaterialTheme.colorScheme.onSurface
            )

            Column(modifier = Modifier.weight(1f)) {
                // INSERTED STREAK NEXT TO USERNAME (ALWAYS VISIBLE)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.width(6.dp))

                    // Logic: Orange if streak > 0, LightGray if 0
                    val streakColor = if (entry.currentStreak > 0) Color(0xFFFF9800) else Color.LightGray
                    Text(
                        text = "🔥${entry.currentStreak}",
                        fontWeight = FontWeight.Bold,
                        color = streakColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(category.label, style = MaterialTheme.typography.bodySmall)
            }

            // --- ADD FRIEND BUTTON ---
            if (!isCurrentUser) {
                if (isAlreadyFriend) {
                    // Friend already
                } else if (isRequestPending) {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = "Pending",
                            tint = Color.Gray
                        )
                    }
                } else {
                    IconButton(onClick = onAddFriend) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Friend",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // --- DATA DISPLAY ---
            val realityGapValue = entry.avgActualGrade - entry.avgExpectedGrade

            val displayValue = when (category) {
                RankingCategory.HARD_WORK -> formatMsWithSeconds(entry.totalStudyTimeMs)

                RankingCategory.REALITY_GAP -> {
                    val sign = if (realityGapValue > 0) "+" else ""
                    val displayGap = if (isAllExams) realityGapValue else realityGapValue * (maxGrade / 10.0)
                    "$sign${"%.1f".format(displayGap)} ${category.unit}"
                }

                RankingCategory.STUDY_EFFICIENCY -> {
                    "${"%.2f".format(entry.efficiencyScore)} ${category.unit}"
                }

                RankingCategory.GRADE_ACTUAL -> {
                    if (isAllExams) {
                        "${"%.1f".format(entry.avgActualGrade)} pts"
                    } else {
                        "${"%.1f".format(entry.avgActualGrade * (maxGrade / 10.0))}/$maxGrade"
                    }
                }

                RankingCategory.GRADE_EXPECTED -> {
                    if (isAllExams) {
                        "${"%.1f".format(entry.avgExpectedGrade)} pts"
                    } else {
                        "${"%.1f".format(entry.avgExpectedGrade * (maxGrade / 10.0))}/$maxGrade"
                    }
                }

                RankingCategory.SLEEP -> "${"%.1f".format(entry.avgSleep)} ${category.unit}"

                else -> {
                    val count = when(category) {
                        RankingCategory.HABIT_WATER -> entry.totalWater
                        RankingCategory.HABIT_COFFEE -> entry.totalCoffee
                        RankingCategory.HABIT_ENERGY -> entry.totalEnergy
                        else -> entry.totalBathroom
                    }
                    "$count ${category.unit}"
                }
            }

            val valueColor = when {
                category == RankingCategory.REALITY_GAP && realityGapValue > 0 -> Color(0xFF4CAF50)
                category == RankingCategory.REALITY_GAP && realityGapValue < 0 -> Color(0xFFF44336)
                else -> MaterialTheme.colorScheme.primary
            }

            Text(
                text = displayValue,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor
            )
        }
    }
}

// FORMATTER: Now shows Hours, Minutes, and Seconds (Removed leading zeros for a cleaner look)
private fun formatMsWithSeconds(ms: Long): String {
    val hours = ms / 3_600_000
    val minutes = (ms % 3_600_000) / 60_000
    val seconds = (ms % 60_000) / 1000
    return if (hours > 0) {
        "${hours}h ${minutes}m ${seconds}s"
    } else {
        "${minutes}m ${seconds}s"
    }
}