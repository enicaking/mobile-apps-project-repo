package com.example.sofiatest.ui.screens

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
import com.example.sofiatest.ui.StopwatchScreen
import kotlinx.coroutines.delay
import com.example.sofiatest.notifications.NotificationHelper

@Composable
fun StopwatchPage(
    subjectName: String,
    examTitle: String,
    endsAtEpochMs: Long,
    onBack: () -> Unit
) {
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationHelper = remember { NotificationHelper(context) }
    // Actualiza cada minuto (días/horas, sin minutos)
    LaunchedEffect(endsAtEpochMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(60_000)
        }
    }
    LaunchedEffect(Unit) {
        notificationHelper.showGeneralNotification(
            "Study Started 📚",
            "Has comenzado a estudiar $subjectName"
        )
    }

    val infoText = formatCountdownDaysHours(endsAtEpochMs - nowMs)

    Column(modifier = Modifier.fillMaxSize()) {
        // Cabecera
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
                    TextButton(onClick = onBack) { Text("← Volver") }
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

        // Cronómetro + texto debajo del cronómetro
        StopwatchScreen(
            showTitle = false,
            bottomInfoText = infoText
        )
    }
}

private fun formatCountdownDaysHours(diffMs: Long): String {
    if (diffMs <= 0L) return "El examen ya ha finalizado."

    val totalHours = diffMs / (1000L * 60 * 60)
    val days = totalHours / 24
    val hours = totalHours % 24

    return when {
        days > 0 && hours > 0 -> "Quedan $days día(s) y $hours hora(s) para el examen."
        days > 0 -> "Quedan $days día(s) para el examen."
        else -> "Quedan $hours hora(s) para el examen."
    }
}