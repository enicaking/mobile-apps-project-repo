package com.example.pearpressure.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pearpressure.data.Subject

@Composable
fun SubjectsScreen(
    subjects: List<Subject>,
    currentUserId: String, // Necesario para saber si eres el owner
    onAddSubject: (String) -> Unit,
    onOpenSubject: (String) -> Unit,
    onActionSubject: (Subject) -> Unit // Maneja borrar o salir
) {
    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // Estado para controlar el diálogo de confirmación de borrado/abandono
    var subjectToAction by remember { mutableStateOf<Subject?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Subjects",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { showDialog = true }) {
                Text("Add Subject")
            }
        }

        if (subjects.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("No subjects yet!", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Click 'Add Subject' to add your first one.")
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
                                    text = if (isOwner) "Owner • Tap to view exams" else "Member • Tap to view exams",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // Botón dinámico: Abre el diálogo de confirmación
                            IconButton(onClick = { subjectToAction = s }) {
                                Icon(
                                    imageVector = if (isOwner)
                                        Icons.Default.Delete
                                    else
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = if (isOwner) "Delete" else "Leave",
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

    // Diálogo para crear nueva asignatura
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New subject") },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddSubject(newName)
                        newName = ""
                        showDialog = false
                    },
                    enabled = newName.trim().isNotEmpty()
                ) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Diálogo de confirmación de Borrado o Salida
    subjectToAction?.let { subject ->
        val isOwner = subject.ownerId == currentUserId
        AlertDialog(
            onDismissRequest = { subjectToAction = null },
            title = {
                Text(text = if (isOwner) "Delete Subject" else "Leave Subject")
            },
            text = {
                Text(
                    text = if (isOwner)
                        "Are you sure you want to delete '${subject.name}'? This action will permanently remove the subject and all associated exams."
                    else
                        "Are you sure you want to leave '${subject.name}'? You will no longer have access to this subject's data."
                )
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
                    Text(text = if (isOwner) "Delete" else "Leave")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { subjectToAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}