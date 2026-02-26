package com.example.sofiatest.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StopwatchSession(
    val durationMs: Long,
    val finishedAtEpochMs: Long
)

@Composable
fun StopwatchScreen(
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    bottomInfoText: String? = null
) {
    var isRunning by remember { mutableStateOf(false) }
    var startElapsedMs by remember { mutableStateOf(0L) }
    var accumulatedMs by remember { mutableStateOf(0L) }
    var displayMs by remember { mutableStateOf(0L) }

    val sessions = remember { mutableStateListOf<StopwatchSession>() }

    LaunchedEffect(isRunning, startElapsedMs, accumulatedMs) {
        if (isRunning) {
            while (true) {
                val now = SystemClock.elapsedRealtime()
                displayMs = accumulatedMs + (now - startElapsedMs)
                delay(50)
            }
        } else {
            displayMs = accumulatedMs
        }
    }

    Scaffold { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (showTitle) {
                Text(
                    text = "Cronómetro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Tarjeta principal con el tiempo grande
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tiempo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = formatDuration(displayMs),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // ✅ Texto debajo del cronómetro (lo que quieres)
            if (!bottomInfoText.isNullOrBlank()) {
                Text(
                    text = bottomInfoText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier.weight(1f).height(52.dp),
                    onClick = {
                        isRunning = true
                        startElapsedMs = SystemClock.elapsedRealtime()
                    },
                    enabled = !isRunning
                ) { Text(if (displayMs > 0L) "Reanudar" else "Start") }

                FilledTonalButton(
                    modifier = Modifier.weight(1f).height(52.dp),
                    onClick = {
                        accumulatedMs = displayMs
                        isRunning = false
                    },
                    enabled = isRunning
                ) { Text("Pause") }

                OutlinedButton(
                    modifier = Modifier.weight(1f).height(52.dp),
                    onClick = {
                        if (isRunning) {
                            accumulatedMs = displayMs
                            isRunning = false
                        }
                        sessions.add(
                            StopwatchSession(
                                durationMs = displayMs,
                                finishedAtEpochMs = System.currentTimeMillis()
                            )
                        )
                        accumulatedMs = 0L
                        displayMs = 0L
                    },
                    enabled = displayMs > 0L
                ) { Text("Finish") }
            }

            // Historial
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historial",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { sessions.clear() }, enabled = sessions.isNotEmpty()) {
                    Text("Borrar")
                }
            }

            if (sessions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Aún no hay registros", fontWeight = FontWeight.SemiBold)
                        Text("Pulsa Finish para guardar el tiempo y la fecha.")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sessions.asReversed()) { s ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = formatDuration(s.durationMs),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formatDateTime(s.finishedAtEpochMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centis = (ms % 1000) / 10
    return String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", hours, minutes, seconds, centis)
}

private fun formatDateTime(epochMs: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMs))
}