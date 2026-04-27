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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    // Internal state to toggle between "View/Success" and "Edit" mode when finished
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
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    Text("Add Member")
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
            Text("Add Exam")
        }

        if (exams.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No exams found.\nTap 'Add Exam' to create one.",
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
            title = { Text(if (examToEdit == null) "New Exam" else "Edit Exam") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        singleLine = true,
                        label = { Text("Exam Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // MAX GRADE INPUT
                    OutlinedTextField(
                        value = maxGradeInput,
                        onValueChange = { maxGradeInput = it },
                        label = { Text("Max Grade (Scale)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateText = selectedEndsAtMs?.let { formatDateTime(it) } ?: "Not set"
                        Text(
                            text = "Ends at: $dateText",
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
                        ) { Text("Set Date") }
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
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Friend selection logic
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Friend to Subject") },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; onSearchFriends(it) },
                        label = { Text("Search friends") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    friends.forEach { user ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(user.fullName)
                            TextButton(onClick = { onUserSelected(user); showDialog = false; query = "" }) { Text("Add") }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
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
                        !hasExpected || !hasSleep -> "Post-Exam Info"
                        !hasReal -> "Final Result"
                        isEditingFinishedExam -> "Edit ${exam.title}"
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
                    Text("Exam Scale: /${exam.maxGrade}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

                    when {
                        // PHASE 1: Post-Exam (Expected + Sleep)
                        !hasExpected || !hasSleep -> {
                            Text("Please use numeric format (e.g., 8.5 or 7)", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(value = expectedInput, onValueChange = { expectedInput = it }, label = { Text("Expected Grade") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = sleepInput, onValueChange = { sleepInput = it }, label = { Text("Sleep Hours") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        }

                        // PHASE 2: Just Real Grade
                        !hasReal -> {
                            Text("Stats saved! Now, enter your real grade:", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(value = actualInput, onValueChange = { actualInput = it }, label = { Text("Real Grade") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
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
                                    Text("Expected", style = MaterialTheme.typography.labelSmall)
                                    Text("$expectedInput", fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Sleep", style = MaterialTheme.typography.labelSmall)
                                    Text("${sleepInput}h", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // PHASE 4: Edit Mode (Finished)
                        else -> {
                            OutlinedTextField(value = expectedInput, onValueChange = { expectedInput = it }, label = { Text("Expected Grade") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = sleepInput, onValueChange = { sleepInput = it }, label = { Text("Sleep Hours") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = actualInput, onValueChange = { actualInput = it }, label = { Text("Real Grade") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (hasReal && !isEditingFinishedExam) {
                        TextButton(onClick = { isEditingFinishedExam = true }) {
                            Text("Edit Stats")
                        }
                        Button(onClick = { examForResults = null }) {
                            Text("Got it!")
                        }
                    } else {
                        // Cancel button for balance and navigation
                        TextButton(onClick = {
                            if (isEditingFinishedExam) isEditingFinishedExam = false else examForResults = null
                        }) {
                            Text("Cancel")
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
                            Text(if (isEditingFinishedExam) "Update" else "Save Information")
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
            title = { Text("Delete Exam") },
            text = { Text("Are you sure you want to delete '${exam.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { onDeleteExam(exam.id); examToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { examToDelete = null }) { Text("Cancel") } }
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

    val statusText = when {
        !isPastDeadline -> "In Progress"
        !hasExpected -> "Waiting for Expected"
        !hasReal -> "Waiting for Final"
        else -> "Finished"
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
                StatusPill(text = statusText, isPositive = !isPastDeadline || (hasExpected && hasReal))

                Spacer(modifier = Modifier.width(8.dp))

                if (isOwner) {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, isPositive: Boolean) {
    val bg = if (isPositive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val fg = if (isPositive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.medium) {
        Text(text = text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

private fun formatDateTime(epochMs: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMs))
}