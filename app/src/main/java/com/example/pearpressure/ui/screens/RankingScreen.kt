package com.example.pearpressure.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pearpressure.MainViewModel
import com.example.pearpressure.RankingEntryUi

@Composable
fun RankingScreen() {
    val vm: MainViewModel = viewModel()

    val subjects by vm.subjects.collectAsState()
    val selectedSubjectId by vm.selectedRankingSubjectId.collectAsState()
    val entries by vm.rankingEntries.collectAsState()

    var showSubjectPicker by remember { mutableStateOf(false) }

    val subjectName = subjects.firstOrNull { it.id == selectedSubjectId }?.name ?: "No subject selected"

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ranking",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))

                TextButton(
                    onClick = { showSubjectPicker = true },
                    enabled = subjects.isNotEmpty()
                ) {
                    Text("Subject")
                }
            }

            Text(
                text = "Showing: $subjectName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Optional filters (template only; connect later to sessions)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { /* TODO later */ },
                    label = { Text("This week") }
                )
                AssistChip(
                    onClick = { /* TODO later */ },
                    label = { Text("All time") }
                )
            }

            OutlinedButton(
                onClick = { vm.loadRanking() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh")
            }

            if (entries.isEmpty()) {
                EmptyRankingState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries) { entry ->
                        val rank = entries.indexOf(entry) + 1
                        RankingRow(rank = rank, entry = entry)
                    }
                }
            }
        }
    }

    // Subject picker dialog
    if (showSubjectPicker) {
        AlertDialog(
            onDismissRequest = { showSubjectPicker = false },
            title = { Text("Choose a subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjects.forEach { s ->
                        OutlinedButton(
                            onClick = {
                                vm.selectRankingSubject(s.id)
                                showSubjectPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(s.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubjectPicker = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun RankingRow(rank: Int, entry: RankingEntryUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Total study time: ${formatMs(entry.totalStudyTimeMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatMs(entry.totalStudyTimeMs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyRankingState() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("No ranking yet", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Ranking is based on users.totalStudyTime for the subject owner + members.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}