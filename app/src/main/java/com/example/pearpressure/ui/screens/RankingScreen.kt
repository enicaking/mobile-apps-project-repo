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
import androidx.annotation.StringRes
import com.example.pearpressure.R
import androidx.compose.ui.res.stringResource
import com.example.pearpressure.ui.theme.*

// Dropdown menus that filter the ranking lists

@StringRes
fun RankingCategory.labelRes(): Int = when (this) {
    RankingCategory.HARD_WORK -> R.string.cat_hard_work
    RankingCategory.REALITY_GAP -> R.string.cat_reality_gap
    RankingCategory.STUDY_EFFICIENCY -> R.string.cat_study_efficiency
    RankingCategory.HABIT_WATER -> R.string.cat_water
    RankingCategory.HABIT_COFFEE -> R.string.cat_coffee
    RankingCategory.HABIT_ENERGY -> R.string.cat_energy
    RankingCategory.HABIT_BATHROOM -> R.string.cat_bathroom
    RankingCategory.GRADE_ACTUAL -> R.string.cat_grade_actual
    RankingCategory.GRADE_EXPECTED -> R.string.cat_grade_expected
    RankingCategory.SLEEP -> R.string.cat_sleep
}

@StringRes
fun RankingCategory.unitRes(): Int? = when (this) {
    RankingCategory.REALITY_GAP -> R.string.unit_pts
    RankingCategory.STUDY_EFFICIENCY -> R.string.unit_pts_hr
    RankingCategory.HABIT_WATER -> R.string.unit_glasses
    RankingCategory.HABIT_COFFEE -> R.string.unit_cups
    RankingCategory.HABIT_ENERGY -> R.string.unit_cans
    RankingCategory.HABIT_BATHROOM -> R.string.unit_breaks
    RankingCategory.SLEEP -> R.string.unit_hours
    else -> null
}

enum class RankingCategory {
    HARD_WORK, REALITY_GAP, STUDY_EFFICIENCY, HABIT_WATER, HABIT_COFFEE,
    HABIT_ENERGY, HABIT_BATHROOM, GRADE_ACTUAL, GRADE_EXPECTED, SLEEP
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

    // Loading exams for a selected subject
    LaunchedEffect(selectedSubjectId) {
        selectedSubjectId?.let { id ->
            viewModel.loadExams(id)
        }
    }

    // Refresh: Triggers whenever Subject, Exam, or Scope changes
    LaunchedEffect(selectedSubjectId, selectedExamId, selectedScope) {
        viewModel.loadRanking(examId = selectedExamId, scope = selectedScope)
    }

    val currentSubject = subjects.firstOrNull { it.id == selectedSubjectId }

    // Find selected exam to know the max scale for display
    val currentSelectedExam = exams.find { it.id == selectedExamId }

    val hasMyExpectedGrade = currentSelectedExam?.expectedGrades?.containsKey(currentUserId) == true
    val hasMyFinalGrade = currentSelectedExam?.actualGrades?.containsKey(currentUserId) == true
    val hasMySleepHours = currentSelectedExam?.sleepHours?.containsKey(currentUserId) == true

    // Scope filter that changes based on available data
    val availableCategories = RankingCategory.entries.filter { category ->
        when (category) {
            RankingCategory.HARD_WORK,
            RankingCategory.STUDY_EFFICIENCY,
            RankingCategory.HABIT_WATER,
            RankingCategory.HABIT_COFFEE,
            RankingCategory.HABIT_ENERGY,
            RankingCategory.HABIT_BATHROOM -> true

            RankingCategory.REALITY_GAP ->
                if (selectedExamId != null) hasMyExpectedGrade && hasMyFinalGrade
                else entries.any { it.uid == currentUserId && it.avgAccuracy != 0.0 }

            RankingCategory.GRADE_EXPECTED ->
                if (selectedExamId != null) hasMyExpectedGrade
                else entries.any { it.uid == currentUserId && it.avgExpectedGrade > 0 }

            RankingCategory.GRADE_ACTUAL ->
                if (selectedExamId != null) hasMyFinalGrade
                else entries.any { it.uid == currentUserId && it.avgActualGrade > 0 }

            RankingCategory.SLEEP ->
                if (selectedExamId != null) hasMySleepHours
                else entries.any { it.uid == currentUserId && it.avgSleep > 0 }
        }
    }

    LaunchedEffect(
        selectedExamId,
        hasMyExpectedGrade,
        hasMyFinalGrade,
        hasMySleepHours,
        entries // Add entries to check if averaged out data appears
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
                        Text(stringResource(R.string.subject),
                            style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = currentSubject?.name ?: stringResource(R.string.select_subject),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(text = stringResource(R.string.change),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge)
                }
            }

            // Time Scope Segmented Toggle
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

            // Dual Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Exam filter Dropdown
                ExposedDropdownMenuBox(
                    expanded = examExpanded,
                    onExpandedChange = { examExpanded = !examExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = exams.find { it.id == selectedExamId }?.title ?: stringResource(R.string.all_exams),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = stringResource(R.string.exam)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = examExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = examExpanded,
                        onDismissRequest = { examExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.all_exams_total)) },
                            onClick = {
                                selectedExamId = null
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

                // Category or metric dropdown menu
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = stringResource(selectedCategory.labelRes()),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = stringResource(R.string.metric)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        availableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(stringResource(cat.labelRes())) },
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

            // Ranking List
            val sortedEntries = remember(entries, selectedCategory) {
                when (selectedCategory) {
                    RankingCategory.HARD_WORK -> entries.sortedByDescending { it.totalStudyTimeMs }
                    RankingCategory.REALITY_GAP -> entries.sortedByDescending { it.avgAccuracy }
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
                    Text(text = stringResource(R.string.cat_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(sortedEntries) { index, entry ->
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
                            onAddFriend = { userToConfirm = entry }
                        )
                    }
                }
            }
        }
    }

    // Dialog for Friend Requests within Ranking page
    userToConfirm?.let { entry ->
        AlertDialog(
            onDismissRequest = { userToConfirm = null },
            title = { Text(stringResource(R.string.add_friend_title)) },
            text = {
                Text(stringResource(R.string.send_request_message, entry.userName))
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.sendFriendRequest(entry.uid)
                    userToConfirm = null
                }) { Text(stringResource(R.string.send)) }
            },
            dismissButton = {
                TextButton(onClick = { userToConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog Subject Picker
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
            confirmButton = { TextButton(onClick = { subjectPickerExpanded = false })
            { Text(stringResource(R.string.cancel)) } }
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
    val unit = category.unitRes()?.let { stringResource(it) }

    val rowColor = when (rank) {
        1 -> RankOneColor.copy(alpha = 0.15f)
        2 -> RankTwoColor.copy(alpha = 0.15f)
        3 -> RankThreeColor.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = when (rank) {
        1 -> RankOneColor
        2 -> RankTwoColor
        3 -> RankThreeColor
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(6.dp))
                    val streakColor = if (entry.currentStreak > 0) StreakColor else Color.LightGray
                    Text(
                        text = stringResource(R.string.streak, entry.currentStreak),
                        fontWeight = FontWeight.Bold,
                        color = streakColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(stringResource(category.labelRes()), style = MaterialTheme.typography.bodySmall)
            }

            // Functionality: Add non-friend member to friend list
            if (!isCurrentUser) {
                if (!isAlreadyFriend && !isRequestPending) {
                    IconButton(onClick = onAddFriend) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                } else if (isRequestPending) {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = null,
                            tint = Color.Gray)
                    }
                }
            }

            val realityGapValue = entry.avgAccuracy
            val displayValue = when (category) {

                RankingCategory.HARD_WORK ->
                    formatMsWithSeconds(entry.totalStudyTimeMs)

                RankingCategory.REALITY_GAP -> {
                    val sign = if (realityGapValue > 0) "+" else ""
                    val displayGap =
                        if (isAllExams) realityGapValue
                        else realityGapValue * (maxGrade / 10.0)

                    "$sign${"%.1f".format(displayGap)} ${unit ?: ""}"
                }

                RankingCategory.STUDY_EFFICIENCY ->
                    "${"%.2f".format(entry.efficiencyScore)} ${unit ?: ""}"

                RankingCategory.GRADE_ACTUAL -> {
                    if (isAllExams) {
                        val pts = stringResource(R.string.unit_pts)
                        "${"%.1f".format(entry.avgActualGrade)} $pts"
                    } else {
                        "${"%.1f".format(entry.avgActualGrade * (maxGrade / 10.0))}/$maxGrade"
                    }
                }

                RankingCategory.GRADE_EXPECTED -> {
                    if (isAllExams) {
                        val pts = stringResource(R.string.unit_pts)
                        "${"%.1f".format(entry.avgExpectedGrade)} $pts"
                    } else {
                        "${"%.1f".format(entry.avgExpectedGrade * (maxGrade / 10.0))}/$maxGrade"
                    }
                }

                RankingCategory.SLEEP ->
                    "${"%.1f".format(entry.avgSleep)} ${unit ?: ""}"

                else -> {
                    val count = when (category) {
                        RankingCategory.HABIT_WATER -> entry.totalWater
                        RankingCategory.HABIT_COFFEE -> entry.totalCoffee
                        RankingCategory.HABIT_ENERGY -> entry.totalEnergy
                        else -> entry.totalBathroom
                    }
                    "$count ${unit ?: ""}"
                }
            }

            val valueColor = when {
                category == RankingCategory.REALITY_GAP && realityGapValue > 0 -> ProfileGreenColor
                category == RankingCategory.REALITY_GAP && realityGapValue < 0 -> ProfileRedColor
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

private fun formatMsWithSeconds(ms: Long): String {
    val hours = ms / 3_600_000
    val minutes = (ms % 3_600_000) / 60_000
    val seconds = (ms % 60_000) / 1000
    return if (hours > 0) "${hours}h ${minutes}m ${seconds}s"
    else "${minutes}m ${seconds}s"
}