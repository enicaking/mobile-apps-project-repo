package com.example.pearpressure.ui

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
import com.example.pearpressure.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StopwatchScreen(
    viewModel: MainViewModel,
    examId: String,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    bottomInfoText: String? = null
) {
    // -----------------------------
    // Stopwatch state
    // -----------------------------
    var isRunning by remember { mutableStateOf(false) }
    var startElapsedMs by remember { mutableStateOf(0L) }
    var accumulatedMs by remember { mutableStateOf(0L) }
    var displayMs by remember { mutableStateOf(0L) }

    val allSessions by viewModel.sessions.collectAsState()

    val sessions = remember(allSessions, examId) {
        allSessions.filter { it.examId == examId }
    }

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

    // -----------------------------
    // UI
    // -----------------------------
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        if (showTitle) {
            Text(
                text = "Stopwatch",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Time")
                Text(
                    text = formatDuration(displayMs),
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (!bottomInfoText.isNullOrBlank()) {
            Text(bottomInfoText)
        }

        // -----------------------------
        // Controls
        // -----------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    isRunning = true
                    startElapsedMs = SystemClock.elapsedRealtime()
                },
                enabled = !isRunning
            ) {
                Text(if (displayMs > 0) "Continue" else "Start")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    accumulatedMs = displayMs
                    isRunning = false
                },
                enabled = isRunning
            ) {
                Text("Pause")
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (isRunning) {
                        accumulatedMs = displayMs
                        isRunning = false
                    }

                    // 🔥 SAVE TO FIRESTORE
                    viewModel.saveSession(
                        examId = examId,
                        durationMs = displayMs
                    )

                    // Reset
                    accumulatedMs = 0L
                    displayMs = 0L
                },
                enabled = displayMs > 0
            ) {
                Text("Finish")
            }
        }

        // -----------------------------
        // History
        // -----------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (sessions.isEmpty()) {
            Text("No sessions yet")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    sessions.sortedByDescending { it.createdAtEpochMs }
                ) { s ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = formatDuration(s.durationMs),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formatDateTime(s.createdAtEpochMs),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------
// Helpers
// -----------------------------

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

private fun formatDateTime(epochMs: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMs))
}