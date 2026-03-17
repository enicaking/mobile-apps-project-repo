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

    var showPicker by remember { mutableStateOf(false) }

    val subjectName = subjects.firstOrNull { it.id == selectedSubjectId }?.name ?: "Sin asignatura"

    Scaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ranking",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showPicker = true }, enabled = subjects.isNotEmpty()) {
                    Text("Asignatura")
                }
            }

            Text(
                text = "Mostrando: $subjectName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = { vm.loadRanking() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Recargar") }

            if (entries.isEmpty()) {
                EmptyRankingState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries) { entry ->
                        val rank = entries.indexOf(entry) + 1
                        RankingRow(rank, entry)
                    }
                }
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Elige asignatura") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjects.forEach { s ->
                        OutlinedButton(
                            onClick = {
                                vm.selectRankingSubject(s.id)
                                showPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(s.name) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
private fun RankingRow(rank: Int, entry: RankingEntryUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Tiempo total: ${formatMs(entry.totalStudyTimeMs)}",
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
            Text("Aún no hay ranking", fontWeight = FontWeight.SemiBold)
            Text(
                "Se calcula con users.totalStudyTime de los members/owner de la asignatura.",
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