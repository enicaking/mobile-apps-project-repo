package com.example.sofiatest.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
fun StopwatchScreen(modifier: Modifier = Modifier) {
    var isRunning by remember { mutableStateOf(false) }
    var startElapsedMs by remember { mutableStateOf(0L) }
    var accumulatedMs by remember { mutableStateOf(0L) }
    var displayMs by remember { mutableStateOf(0L) }

    val sessions = remember { mutableStateListOf<StopwatchSession>() }

    // Actualiza el tiempo mientras corre
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

    // Texto de estado para que sea más claro visualmente
    val statusText = when {
        isRunning -> "Corriendo"
        displayMs > 0L -> "Pausado"
        else -> "Listo"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Encabezado: título + chip de estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cronómetro",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                AssistChip(
                    onClick = { /* no hace nada, es solo visual */ },
                    label = { Text(statusText) }
                )
            }

            // Barra visual cuando está corriendo
            AnimatedVisibility(visible = isRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Tarjeta principal con el tiempo grande
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

            // Controles (botones grandes y alineados)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    onClick = {
                        isRunning = true
                        startElapsedMs = SystemClock.elapsedRealtime()
                    },
                    enabled = !isRunning
                ) {
                    Text(if (displayMs > 0L) "Reanudar" else "Start")
                }

                FilledTonalButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    onClick = {
                        accumulatedMs = displayMs
                        isRunning = false
                    },
                    enabled = isRunning
                ) {
                    Text("Pause")
                }

                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    onClick = {
                        // Si estaba corriendo, lo pausamos primero
                        if (isRunning) {
                            accumulatedMs = displayMs
                            isRunning = false
                        }

                        // Guardar registro (tiempo + fecha)
                        sessions.add(
                            StopwatchSession(
                                durationMs = displayMs,
                                finishedAtEpochMs = System.currentTimeMillis()
                            )
                        )

                        // Reset
                        accumulatedMs = 0L
                        displayMs = 0L
                    },
                    enabled = displayMs > 0L
                ) {
                    Text("Finish")
                }
            }

            // Sección historial con acción de borrar arriba (más limpio)
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
                TextButton(
                    onClick = { sessions.clear() },
                    enabled = sessions.isNotEmpty()
                ) {
                    Text("Borrar")
                }
            }

            if (sessions.isEmpty()) {
                // Mensaje “bonito” cuando no hay nada
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Aún no hay registros",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Pulsa Finish para guardar el tiempo y la fecha.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sessions.asReversed()) { s ->
                        HistoryItem(session = s)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(session: StopwatchSession) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDuration(session.durationMs),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatDateTime(session.finishedAtEpochMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // “Pill” pequeño para que se vea más UI
            SuggestionChip(
                onClick = { /* solo visual */ },
                label = { Text("Guardado") }
            )
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

@Preview(showBackground = true)
@Composable
private fun StopwatchScreenPreview() {
    StopwatchScreen()
}