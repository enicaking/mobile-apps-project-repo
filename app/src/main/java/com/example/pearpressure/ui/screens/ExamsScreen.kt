/* ExamsScreen.kt
Loads the exams, status, due date for a given subject */
package com.example.pearpressure.ui.screens

import com.example.pearpressure.data.UserProfile
import com.example.pearpressure.data.Exam
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.pearpressure.R
import androidx.compose.ui.res.stringResource
import com.example.pearpressure.ui.theme.*

@Composable
fun ExamsScreen(
    subjectName: String,
    exams: List<Exam>,
    isOwner: Boolean,
    friends: List<UserProfile>,
    currentUserId: String, // ID to identify who is saving
    onSearchFriends: (String) -> Unit,
    onUserSelected: (UserProfile) -> Unit,
    onAddMember: () -> Unit,
    onLeaveSubject: () -> Unit,
    onAddExam: (title: String, endsAtMs: Long, maxGrade: Double) -> Unit,
    onUpdateExam: (examId: String, title: String, endsAtMs: Long, maxGrade: Double) -> Unit, // UPDATED TO INCLUDE MAXGRADE
    onOpenInProgressExam: (Exam) -> Unit,
    onDeleteExam: (String) -> Unit,
    onBack: () -> Unit,
    initialPostExamId: String? = null,
    onSaveResults: (examId: String, expected: Double?, sleep: Double?, actual: Double?) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current

    // Update "In Progress / Finished" status every minute
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            nowMs = System.currentTimeMillis()
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var selectedEndsAtMs by remember { mutableStateOf<Long?>(null) }

    // State for editing an existing exam
    var examToEdit by remember { mutableStateOf<Exam?>(null) }
    var examForResults by remember { mutableStateOf<Exam?>(null) }
    LaunchedEffect(initialPostExamId, exams) {
        if (!initialPostExamId.isNullOrBlank()) {
            examForResults = exams.firstOrNull { it.id == initialPostExamId }
        }
    }
    var expectedInput by remember { mutableStateOf("") }
    var sleepInput by remember { mutableStateOf("") }
    var actualInput by remember { mutableStateOf("") }
    // Internal state to toggle between "View/Success" and stringResource(R.string.edit) mode when finished
    var isEditingFinishedExam by remember { mutableStateOf(false) }
    var examToDelete by remember { mutableStateOf<Exam?>(null) }
    // maximum exam grade var so that it can be changed depends on exam, not just out of 10(but its still the base and assumed)
    var maxGradeInput by remember { mutableStateOf("10.0") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                text = subjectName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (isOwner) {
                Button(
                    onClick = {
                        showDialog = true
                        onAddMember()
                    }
                ) {
                    Text(stringResource(R.string.exams_button_add_member))
                }
            }
        }

        Button(
            onClick = {
                examToEdit = null // Fresh state for new exam
                newTitle = ""
                selectedEndsAtMs = null
                maxGradeInput = "10.0"
                showCreateDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(stringResource(R.string.exams_button_add_exam))
        }

        if (exams.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.exams_empty_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(exams) { exam ->
                    ExamCard(
                        exam = exam,
                        nowMs = nowMs,
                        currentUserId = currentUserId,
                        isOwner = isOwner,

                        onOpenInProgressExam = onOpenInProgressExam,
                        onOpenFinishedExam = {
                            examForResults = exam
                            isEditingFinishedExam = false // Start in "View" mode
                            expectedInput = exam.expectedGrades[currentUserId]?.toString() ?: ""
                            sleepInput = exam.sleepHours[currentUserId]?.toString() ?: ""
                            actualInput = exam.actualGrades[currentUserId]?.toString() ?: ""
                        },
                        onDelete = { examToDelete = exam },
                        onEdit = {
                            examToEdit = exam
                            newTitle = exam.title
                            selectedEndsAtMs = exam.endsAtEpochMs
                            maxGradeInput = exam.maxGrade.toString()
                            showCreateDialog = true
                        }
                    )
                }
            }
        }
    }

    // CREATE OR EDIT DIALOG
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (examToEdit == null) stringResource(R.string.exams_dialog_new_title) else stringResource(R.string.exams_dialog_edit_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.exams_dialog_label_exam_title)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // MAX GRADE INPUT
                    OutlinedTextField(
                        value = maxGradeInput,
                        onValueChange = { maxGradeInput = it },
                        label = { Text(stringResource(R.string.exams_dialog_label_max_grade)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateText = selectedEndsAtMs?.let { formatDateTime(it) } ?: stringResource(R.string.exams_dialog_ends_at_not_set)
                        Text(
                            text = stringResource(R.string.exams_dialog_ends_at, dateText),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        TextButton(
                            onClick = {
                                val currentCal = Calendar.getInstance()
                                selectedEndsAtMs?.let { currentCal.timeInMillis = it }

                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                val cal = Calendar.getInstance().apply {
                                                    set(year, month, day, hour, minute, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                selectedEndsAtMs = cal.timeInMillis
                                            },
                                            currentCal.get(Calendar.HOUR_OF_DAY),
                                            currentCal.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                    currentCal.get(Calendar.YEAR),
                                    currentCal.get(Calendar.MONTH),
                                    currentCal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) { Text(stringResource(R.string.exams_dialog_button_set_date)) }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ends = selectedEndsAtMs ?: return@Button
                        val mGrade = maxGradeInput.toDoubleOrNull() ?: 10.0
                        if (examToEdit == null) {
                            onAddExam(newTitle, ends, mGrade)
                        } else {
                            onUpdateExam(examToEdit!!.id, newTitle, ends, mGrade)
                        }
                        showCreateDialog = false
                    },
                    enabled = newTitle.trim().isNotEmpty() && selectedEndsAtMs != null
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Friend selection logic
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.exams_dialog_add_friend_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; onSearchFriends(it) },
                        label = { Text(stringResource(R.string.exams_dialog_search_friends_label)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    friends.forEach { user ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(user.fullName)
                            TextButton(onClick = { onUserSelected(user); showDialog = false; query = "" }) { Text(stringResource(R.string.add)) }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // RESULTS DIALOG: Improved flow with visual check and specific titles
    examForResults?.let { exam ->
        val hasExpected = exam.expectedGrades.containsKey(currentUserId)
        val hasSleep = exam.sleepHours.containsKey(currentUserId)
        val hasReal = exam.actualGrades.containsKey(currentUserId)

        AlertDialog(
            onDismissRequest = { examForResults = null },
            title = {
                Text(
                    when {
                        !hasExpected || !hasSleep -> stringResource(R.string.exams_results_title_post_exam)
                        !hasReal                  -> stringResource(R.string.exams_results_title_final)
                        isEditingFinishedExam     -> stringResource(R.string.exams_results_title_edit, exam.title)
                        else -> exam.title
                    }
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.exams_results_exam_scale, exam.maxGrade), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

                    when {
                        // PHASE 1: Post-Exam (Expected + Sleep)
                        !hasExpected || !hasSleep -> {
                            Text("Please use numeric format (e.g., 8.5 or 7)", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(value = expectedInput, onValueChange = { expectedInput = it }, label = { Text(stringResource(R.string.exams_results_label_expected_grade)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = sleepInput, onValueChange = { sleepInput = it }, label = { Text(stringResource(R.string.exams_results_label_sleep_hours)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        }

                        // PHASE 2: Just Real Grade
                        !hasReal -> {
                            Text(stringResource(R.string.exams_results_stats_saved), style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(value = actualInput, onValueChange = { actualInput = it }, label = { Text(stringResource(R.string.exams_results_label_final_grade)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        }

                        // PHASE 3: View Mode (Finished) - Visual summary
                        !isEditingFinishedExam -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✓", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Text(
                                text = "$actualInput / ${exam.maxGrade}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.exams_results_label_expected), style = MaterialTheme.typography.labelSmall)
                                    Text("$expectedInput", fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.exams_results_label_sleep), style = MaterialTheme.typography.labelSmall)
                                    Text(stringResource(R.string.exams_results_sleep_value, sleepInput), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // PHASE 4: Edit Mode (Finished)
                        else -> {
                            OutlinedTextField(value = expectedInput, onValueChange = { expectedInput = it }, label = { Text(stringResource(R.string.exams_results_label_expected_grade)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = sleepInput, onValueChange = { sleepInput = it }, label = { Text(stringResource(R.string.exams_results_label_sleep_hours)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = actualInput, onValueChange = { actualInput = it }, label = { Text(stringResource(R.string.exams_results_label_final_grade)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (hasReal && !isEditingFinishedExam) {
                        TextButton(onClick = { isEditingFinishedExam = true }) {
                            Text(stringResource(R.string.exams_results_button_edit_stats))
                        }
                        Button(onClick = { examForResults = null }) {
                            Text(stringResource(R.string.exams_results_button_got_it))
                        }
                    } else {
                        // Cancel button for balance and navigation
                        TextButton(onClick = {
                            if (isEditingFinishedExam) isEditingFinishedExam = false else examForResults = null
                        }) {
                            Text(stringResource(R.string.cancel))
                        }

                        Button(
                            onClick = {
                                onSaveResults(
                                    exam.id,
                                    expectedInput.toDoubleOrNull(),
                                    sleepInput.toDoubleOrNull(),
                                    actualInput.toDoubleOrNull()
                                )
                                if (isEditingFinishedExam) isEditingFinishedExam = false else examForResults = null
                            }
                        ) {
                            Text(if (isEditingFinishedExam) stringResource(R.string.update) else stringResource(R.string.exams_results_button_save))
                        }
                    }
                }
            }
        )
    }

    if (examToDelete != null) {
        val exam = examToDelete!!
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            title = { Text(stringResource(R.string.exams_dialog_delete_title)) },
            text = { Text(stringResource(R.string.exams_dialog_delete_text, exam.title)) },
            confirmButton = {
                Button(
                    onClick = { onDeleteExam(exam.id); examToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { OutlinedButton(onClick = { examToDelete = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun ExamCard(
    exam: Exam,
    nowMs: Long,
    currentUserId: String,
    isOwner: Boolean,
    onOpenInProgressExam: (Exam) -> Unit,
    onOpenFinishedExam: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val isPastDeadline = nowMs > exam.endsAtEpochMs
    val hasExpected = exam.expectedGrades.containsKey(currentUserId)
    val hasReal = exam.actualGrades.containsKey(currentUserId)

    val status = when {
        !isPastDeadline -> ExamStatus.IN_PROGRESS
        !hasExpected    -> ExamStatus.WAITING_EXPECTED
        !hasReal        -> ExamStatus.WAITING_FINAL
        else            -> ExamStatus.FINISHED
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!isPastDeadline) onOpenInProgressExam(exam) else onOpenFinishedExam()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = exam.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "Deadline: ${formatDateTime(exam.endsAtEpochMs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(status = status)

                Spacer(modifier = Modifier.width(8.dp))

                if (isOwner) {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.edit), tint = MaterialTheme.colorScheme.primary)
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

enum class ExamStatus {
    IN_PROGRESS, WAITING_EXPECTED, WAITING_FINAL, FINISHED
}

@Composable
private fun StatusPill(status: ExamStatus) {
    val (bg, fg) = when (status) {
        ExamStatus.IN_PROGRESS       -> StatusInProgressBg to StatusInProgressFg
        ExamStatus.WAITING_EXPECTED  -> StatusWaitingExpBg to StatusWaitingExpFg
        ExamStatus.WAITING_FINAL     -> StatusWaitingFinBg to StatusWaitingFinFg
        ExamStatus.FINISHED          -> StatusFinishedBg   to StatusFinishedFg
    }

    Surface(
        color = bg,
        contentColor = fg,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = when (status) {
                ExamStatus.IN_PROGRESS      -> stringResource(R.string.exams_status_in_progress)
                ExamStatus.WAITING_EXPECTED -> stringResource(R.string.exams_status_waiting_expected)
                ExamStatus.WAITING_FINAL    -> stringResource(R.string.exams_status_waiting_final)
                ExamStatus.FINISHED         -> stringResource(R.string.exams_status_finished)
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDateTime(epochMs: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMs))
}