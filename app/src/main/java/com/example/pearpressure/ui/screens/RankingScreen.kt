package com.example.pearpressure.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
    REALITY_GAP("Reality Gap", "pts"), // REPLACED Accuracy with Reality Gap
    EFFICIENCY("Efficiency", "pts/hr"),
    HABIT_WATER("Water Intake", "glasses"),
    HABIT_COFFEE("Coffee Consumed", "cups"),
    HABIT_ENERGY("Energy Drinks", "cans"),
    HABIT_BATHROOM("Bathroom Breaks", "breaks"),
    GRADE_ACTUAL("Actual Grade", ""),
    GRADE_EXPECTED("Expected Grade", ""),
    SLEEP("Sleep", "hrs")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(viewModel: MainViewModel = viewModel()) {
    val subjects by viewModel.subjects.collectAsState()
    val exams by viewModel.exams.collectAsState()
    val selectedSubjectId by viewModel.selectedRankingSubjectId.collectAsState()
    val entries by viewModel.rankingEntries.collectAsState()

    var selectedScope by remember { mutableStateOf(RankingScope.TOTAL) }
    var selectedCategory by remember { mutableStateOf(RankingCategory.HARD_WORK) }
    var selectedExamId by remember { mutableStateOf<String?>(null) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var examExpanded by remember { mutableStateOf(false) }
    var subjectPickerExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSubjectId, selectedExamId, selectedScope) {
        viewModel.loadRanking(examId = selectedExamId, scope = selectedScope)
    }

    val currentSubject = subjects.firstOrNull { it.id == selectedSubjectId }
    val currentSelectedExam = exams.find { it.id == selectedExamId }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            onClick = { selectedExamId = null; examExpanded = false }
                        )
                        exams.forEach { exam ->
                            DropdownMenuItem(
                                text = { Text(exam.title) },
                                onClick = { selectedExamId = exam.id; examExpanded = false }
                            )
                        }
                    }
                }

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
                        RankingCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label) },
                                onClick = { selectedCategory = cat; categoryExpanded = false }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            val sortedEntries = remember(entries, selectedCategory) {
                when (selectedCategory) {
                    RankingCategory.HARD_WORK -> entries.sortedByDescending { it.totalStudyTimeMs }
                    RankingCategory.REALITY_GAP -> entries.sortedByDescending { it.avgActualGrade - it.avgExpectedGrade }
                    RankingCategory.EFFICIENCY -> entries.sortedByDescending { it.efficiencyScore }
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
                        RankingRow(
                            rank = index + 1,
                            entry = entry,
                            category = selectedCategory,
                            maxGrade = currentSelectedExam?.maxGrade ?: 10.0
                        )
                    }
                }
            }
        }
    }

    if (subjectPickerExpanded) {
        AlertDialog(
            onDismissRequest = { subjectPickerExpanded = false },
            title = { Text("Select Subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjects.forEach { s ->
                        OutlinedButton(
                            onClick = {
                                viewModel.selectRankingSubject(s.id)
                                viewModel.loadExams(s.id)
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
private fun RankingRow(rank: Int, entry: RankingEntryUi, category: RankingCategory, maxGrade: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rank == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(45.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(category.label, style = MaterialTheme.typography.bodySmall)
            }

            // --- REALITY GAP MATH ---
            val realityGapValue = entry.avgActualGrade - entry.avgExpectedGrade

            val displayValue = when (category) {
                RankingCategory.HARD_WORK -> formatMsWithSeconds(entry.totalStudyTimeMs)
                RankingCategory.REALITY_GAP -> {
                    val sign = if (realityGapValue > 0) "+" else ""
                    "$sign${"%.1f".format(realityGapValue)} ${category.unit}"
                }
                RankingCategory.EFFICIENCY -> "${"%.2f".format(entry.efficiencyScore)} ${category.unit}"
                RankingCategory.GRADE_ACTUAL -> "${"%.1f".format(entry.avgActualGrade)}/${"%.1f".format(maxGrade)}"
                RankingCategory.GRADE_EXPECTED -> "${"%.1f".format(entry.avgExpectedGrade)}/${"%.1f".format(maxGrade)}"
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
                category == RankingCategory.REALITY_GAP && realityGapValue > 0 -> Color(0xFF4CAF50) // Positive Green
                category == RankingCategory.REALITY_GAP && realityGapValue < 0 -> Color(0xFFF44336) // Negative Red
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
    return if (hours > 0) {
        "%02dh %02dm %02ds".format(hours, minutes, seconds)
    } else {
        "%02dm %02ds".format(minutes, seconds)
    }
}