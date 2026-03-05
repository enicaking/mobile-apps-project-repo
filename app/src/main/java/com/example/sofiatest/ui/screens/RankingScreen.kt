package com.example.sofiatest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class RankingEntry(
    val userName: String,
    val score: Int // for now: points (we'll define what this means later)
)

private enum class RankingFilter { WEEK, ALL_TIME }

@Composable
fun RankingScreen() {
    // ✅ Dummy data for now (later we'll replace it with real ranking data)
    val sampleEntries = remember {
        listOf(
            RankingEntry("Sofía", 120),
            RankingEntry("Alex", 95),
            RankingEntry("María", 80),
            RankingEntry("Diego", 70),
            RankingEntry("Lucía", 60)
        )
    }

    var filter by remember { mutableStateOf(RankingFilter.WEEK) }

    // Later you can change this to show different lists based on filter
    val entries = when (filter) {
        RankingFilter.WEEK -> sampleEntries
        RankingFilter.ALL_TIME -> sampleEntries // placeholder for now
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header (same style as your other screens)
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

                // Simple filter toggle (no experimental components)
                TextButton(
                    onClick = { filter = RankingFilter.WEEK },
                    enabled = filter != RankingFilter.WEEK
                ) { Text("Semana") }

                TextButton(
                    onClick = { filter = RankingFilter.ALL_TIME },
                    enabled = filter != RankingFilter.ALL_TIME
                ) { Text("Total") }
            }

            // Optional action button placeholder (future: invite friends, refresh, etc.)
            OutlinedButton(
                onClick = { /* TODO: later */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Acción (próximamente)")
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
}

@Composable
private fun RankingRow(rank: Int, entry: RankingEntry) {
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
                    text = "Puntos: ${entry.score}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // You can later replace "score" with hours studied, streak, etc.
            Text(
                text = entry.score.toString(),
                style = MaterialTheme.typography.titleLarge,
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
                "Cuando tengamos datos (amigos, puntos, horas, etc.) aparecerán aquí.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}