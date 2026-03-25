package com.example.pearpressure.ui.screens
import com.example.pearpressure.data.UserProfile
import com.example.pearpressure.data.Exam

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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

@Composable
fun ExamsScreen(
    subjectName: String,
    exams: List<Exam>,
    isOwner: Boolean,
    friends: List<UserProfile>,
    currentUserId: String, // ✅ ADDED THIS to identify who is saving
    onSearchFriends: (String) -> Unit,
    onUserSelected: (UserProfile) -> Unit,
    onAddMember: () -> Unit,
    onLeaveSubject: () -> Unit,
    onAddExam: (title: String, endsAtMs: Long) -> Unit,
    onOpenInProgressExam: (Exam) -> Unit,
    onDeleteExam: (String) -> Unit,
    onBack: () -> Unit,
    // NEW: Callback to save the results
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

    // Changed to store the specific exam being edited
    var examForResults by remember { mutableStateOf<Exam?>(null) }
    var expectedInput by remember { mutableStateOf("") }
    var sleepInput by remember { mutableStateOf("") }
    var actualInput by remember { mutableStateOf("") }

    // Estado para controlar qué examen se quiere borrar y mostrar el diálogo
    var examToDelete by remember { mutableStateOf<Exam?>(null) }

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
            Button(
                onClick = {
                    if (isOwner) {
                        showDialog = true
                        onAddMember()
                    } else {
                        onLeaveSubject()
                    }
                }
            ) {
                Text(if (isOwner) "Add Member" else "Leave Subject")
            }
        }

        Button(
            onClick = {
                newTitle = ""
                selectedEndsAtMs = null
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
            // Clean LazyColumn (fixes the blue scrollbar bug)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(exams) { exam ->
                    ExamCard(
                        exam = exam,
                        nowMs = nowMs,
                        onOpenInProgressExam = onOpenInProgressExam,
                        onOpenFinishedExam = {
                            // Check if exam finished less than 2 days ago
                            val twoDaysInMs = 2 * 24 * 60 * 60 * 1000L
                            if (nowMs - exam.endsAtEpochMs < twoDaysInMs) {
                                examForResults = exam
                                // Reset inputs or pre-fill if you have existing data
                                expectedInput = ""
                                sleepInput = ""
                                actualInput = ""
                            } else {
                                // If more than 2 days, just show alert (optional)
                            }
                        },
                        onDelete = { examToDelete = exam } // Cambiado para abrir diálogo
                    )
                }
            }
        }
    }


    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Exam") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        singleLine = true,
                        label = { Text("Exam Title") },
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
                                val now = Calendar.getInstance()
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
                                            now.get(Calendar.HOUR_OF_DAY),
                                            now.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                    now.get(Calendar.YEAR),
                                    now.get(Calendar.MONTH),
                                    now.get(Calendar.DAY_OF_MONTH)
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
                        onAddExam(newTitle, ends)
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
    // Search and add member from friends
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Friend to Subject") },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            onSearchFriends(it)
                        },
                        label = { Text("Search friends") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    friends.forEach { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(user.fullName)

                            TextButton(
                                onClick = {
                                    onUserSelected(user)
                                    showDialog = false
                                    query = ""
                                }
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    // MODIFIED: Post-Exam Results Dialog with Sequential Phase Logic
    examForResults?.let { exam ->
        // Check if the current user has already saved stats in the database
        val hasExpected = exam.expectedGrades.containsKey(currentUserId)
        val hasSleep = exam.sleepHours.containsKey(currentUserId)
        val hasReal = exam.actualGrades.containsKey(currentUserId)

        AlertDialog(
            onDismissRequest = { examForResults = null },
            title = {
                Text(if (!hasExpected || !hasSleep) "Post-Exam Info" else "Final Result")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!hasExpected || !hasSleep) {
                        Text("Please use numeric format (e.g., 8.5 or 7)", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = expectedInput,
                            onValueChange = { expectedInput = it },
                            label = { Text("Expected Grade (0.0-10)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = sleepInput,
                            onValueChange = { sleepInput = it },
                            label = { Text("Sleep Hours (Night before)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (!hasReal) {
                        Text("Stats saved! Now, enter your real grade (0.0-10):", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = actualInput,
                            onValueChange = { actualInput = it },
                            label = { Text("Real Grade") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // All steps finished
                        Text("This exam is completed. Great job!")
                    }
                }
            },
            confirmButton = {
                if (!hasReal) {
                    Button(onClick = {
                        if (!hasExpected || !hasSleep) {
                            // Phase 1: Save Expected & Sleep
                            onSaveResults(exam.id, expectedInput.toDoubleOrNull(), sleepInput.toDoubleOrNull(), null)
                        } else {
                            // Phase 2: Save Real Grade
                            onSaveResults(exam.id, null, null, actualInput.toDoubleOrNull())
                        }
                        examForResults = null // Close dialog
                    }) {
                        Text("Save Information")
                    }
                } else {
                    Button(onClick = { examForResults = null }) { Text("Got it") }
                }
            },
            dismissButton = {
                TextButton(onClick = { examForResults = null }) { Text("Close") }
            }
        )
    }

    // Confirmation dialog for deleting an exam
    examToDelete?.let { exam ->
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            title = { Text("Delete Exam") },
            text = { Text("Are you sure you want to delete '${exam.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExam(exam.id)
                        examToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { examToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExamCard(
    exam: Exam,
    nowMs: Long,
    onOpenInProgressExam: (Exam) -> Unit,
    onOpenFinishedExam: () -> Unit,
    onDelete: () -> Unit //to delete exam
) {
    val inProgress = nowMs < exam.endsAtEpochMs

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (inProgress) onOpenInProgressExam(exam) else onOpenFinishedExam()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exam.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Deadline: ${formatDateTime(exam.endsAtEpochMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Fila para el status pill y el botón de borrar
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    text = if (inProgress) "In Progress" else "Finished",
                    isPositive = inProgress
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Exam",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}


@Composable
private fun StatusPill(text: String, isPositive: Boolean) {
    val bg = if (isPositive) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant

    val fg = if (isPositive) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = bg,
        contentColor = fg,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = text,
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