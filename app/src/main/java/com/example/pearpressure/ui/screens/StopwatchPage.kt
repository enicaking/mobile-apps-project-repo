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
import com.example.pearpressure.notifications.NotificationHelper

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
    var examReminderShown by remember { mutableStateOf(false) }
    val infoText = formatCountdownDaysHours(endsAtEpochMs - nowMs)
    val remainingMs = endsAtEpochMs - nowMs

    LaunchedEffect(endsAtEpochMs) {
        notificationHelper.scheduleExamReminder(
            examTimeMs = endsAtEpochMs,
            title = "Exam Tomorrow 📚",
            message = "Tu examen de $subjectName es en 24 horas."
        )
    }
    // Actualiza cada minuto (días/horas, sin minutos)
    LaunchedEffect(endsAtEpochMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(60_000)
        }
    }
    LaunchedEffect(remainingMs) {
        val oneDayMs = 24 * 60 * 60 * 1000

        if (remainingMs in 1..oneDayMs && !examReminderShown) {
            examReminderShown = true
            notificationHelper.showReminderNotification(
                "Exam Soon 📚",
                "Tu examen de $subjectName es en menos de 24 horas."
            )
        }
    }





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