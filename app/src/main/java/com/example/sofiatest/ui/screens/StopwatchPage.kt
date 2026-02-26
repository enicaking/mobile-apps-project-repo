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

@Composable
fun StopwatchPage(
    subjectName: String,
    examTitle: String,
    endsAtEpochMs: Long,
    onBack: () -> Unit
) {
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }

    // Actualiza cada 1 minuto (no necesitamos segundos para días/horas)
    LaunchedEffect(endsAtEpochMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(60_000)
        }
    }

    val diffMs = endsAtEpochMs - nowMs
    val infoText = formatCountdownDaysHours(diffMs)

    Column(modifier = Modifier.fillMaxSize()) {

        // Cabecera con asignatura/examen (sin el countdown aquí)
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

        // Cronómetro + texto debajo
        StopwatchScreen(
            showTitle = false,              // ya tenemos cabecera arriba
            bottomInfoText = infoText       // ✅ debajo del cronómetro
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