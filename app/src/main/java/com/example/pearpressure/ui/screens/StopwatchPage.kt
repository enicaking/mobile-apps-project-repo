package com.example.pearpressure.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pearpressure.ui.StopwatchScreen
import kotlinx.coroutines.delay

@Composable
fun StopwatchPage(
    subjectName: String,
    examTitle: String,
    endsAtEpochMs: Long,
    onBack: () -> Unit
) {
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }

    // Update every minute (days/hours only)
    LaunchedEffect(endsAtEpochMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(60_000)
        }
    }

    val countdownText = formatCountdownDaysHours(endsAtEpochMs - nowMs)

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(tonalElevation = 2.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) { Text("← Back") }
                    Spacer(Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = examTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Divider()
            }
        }

        // Stopwatch (no title, countdown under the timer card)
        StopwatchScreen(
            showTitle = false,
            bottomInfoText = countdownText
        )
    }
}

private fun formatCountdownDaysHours(diffMs: Long): String {
    if (diffMs <= 0L) return "The exam has already ended."

    val totalHours = diffMs / (1000L * 60 * 60)
    val days = totalHours / 24
    val hours = totalHours % 24

    return when {
        days > 0 && hours > 0 -> "Remaining: $days day(s) and $hours hour(s)."
        days > 0 -> "Remaining: $days day(s)."
        else -> "Remaining: $hours hour(s)."
    }
}