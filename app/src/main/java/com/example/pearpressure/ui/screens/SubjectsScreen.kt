package com.example.pearpressure.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pearpressure.R
import com.example.pearpressure.data.Subject

@Composable
fun SubjectsScreen(
    subjects: List<Subject>,
    currentUserId: String, // Differentiate between owner and member

    onAddSubject: (String) -> Unit, // Callback to add a new subject
    onOpenSubject: (String) -> Unit, // Callback to open a subject
    onActionSubject: (Subject) -> Unit, // Callback to delete or leave a subject
    onUpdateSubject: (String, String) -> Unit // Callback to update subject name
) {
    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // State to track which subject we are editing
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }

    // State to track the dialog for confirm action: delete or leave a subject
    var subjectToAction by remember { mutableStateOf<Subject?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.subjects),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                subjectToEdit = null // Clear edit state for a fresh "Add"
                newName = ""
                showDialog = true
            }) {
                Text(text = stringResource(R.string.add_subject))
            }
        }

        if (subjects.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = stringResource(R.string.no_subjects), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(text = stringResource(R.string.click_add_subject))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(subjects) { s ->
                    val isOwner = s.ownerId == currentUserId

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSubject(s.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = s.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = if (isOwner) stringResource(R.string.owner_view_exams)
                                    else stringResource(R.string.member_view_exams),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // Button Row: Edit and Action (Leave or delete)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isOwner) {
                                    IconButton(onClick = {
                                        subjectToEdit = s
                                        newName = s.name // Pre-fill the current name
                                        showDialog = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(onClick = { subjectToAction = s }) {
                                    Icon(
                                        imageVector = if (isOwner)
                                            Icons.Default.Delete
                                        else
                                            Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = if (isOwner) stringResource(R.string.delete) else stringResource(R.string.leave),
                                        tint = if (isOwner)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog to create or edit an existing subject
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (subjectToEdit == null) stringResource(R.string.add_subject) else stringResource(R.string.edit_subject)) },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text(text=stringResource(R.string.name)) }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subjectToEdit == null) {
                            onAddSubject(newName)
                        } else {
                            onUpdateSubject(subjectToEdit!!.id, newName)
                        }
                        newName = ""
                        showDialog = false
                    },
                    enabled = newName.trim().isNotEmpty()
                ) { Text(text = stringResource(R.string.save)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) { Text(text = stringResource(R.string.cancel)) }
            }
        )
    }

    // Dialog to confirm leaving or deleting a subject
    subjectToAction?.let { subject ->
        val isOwner = subject.ownerId == currentUserId
        val message = if (isOwner) {
            stringResource(R.string.dialog_delete_subject, subject.name)
        } else {
            stringResource(R.string.dialog_leave_subject, subject.name)
        }
        AlertDialog(
            onDismissRequest = { subjectToAction = null },
            title = {
                Text(text = if (isOwner) stringResource(R.string.delete_subject) else stringResource(R.string.leave_subject))
            },
            text = {
                Text(text = message)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onActionSubject(subject)
                        subjectToAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOwner) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = if (isOwner) stringResource(R.string.delete) else stringResource(R.string.leave))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { subjectToAction = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}