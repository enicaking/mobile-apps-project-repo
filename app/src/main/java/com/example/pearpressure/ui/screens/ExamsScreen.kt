package com.example.pearpressure.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pearpressure.data.Exam
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ExamsScreen(
    subjectName: String,
    exams: List<Exam>,
    onAddExam: (title: String, endsAtMs: Long) -> Unit,
    onOpenInProgressExam: (Exam) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Para que “En proceso/Terminado” se actualice con el tiempo.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000) // cada minuto
            nowMs = System.currentTimeMillis()
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var selectedEndsAtMs by remember { mutableStateOf<Long?>(null) }
    var showFinishedDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Asignaturas") }
            Spacer(Modifier.width(8.dp))
            Text(
                text = subjectName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Button(onClick = { showCreateDialog = true }) { Text("Nuevo examen") }
        }

        if (exams.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("No hay exámenes todavía.", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Pulsa “Nuevo examen” para crear el primero.")
                }
            }
        } else {
            // If there are only a few exams, we show a simple Column (no scrolling => no big scrollbar).
// If there are many exams, we switch to LazyColumn (scrollable).
            val listModifier = Modifier.fillMaxWidth()

            if (exams.size <= 6) {
                Column(
                    modifier = listModifier,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    exams.forEach { exam ->
                        ExamCard(
                            exam = exam,
                            nowMs = nowMs,
                            onOpenInProgressExam = onOpenInProgressExam,
                            onOpenFinishedExam = { showFinishedDialog = true }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = listModifier,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(exams) { exam ->
                        ExamCard(
                            exam = exam,
                            nowMs = nowMs,
                            onOpenInProgressExam = onOpenInProgressExam,
                            onOpenFinishedExam = { showFinishedDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nuevo examen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        singleLine = true,
                        label = { Text("Nombre del examen") }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateText = selectedEndsAtMs?.let { formatDateTime(it) } ?: "Sin fecha/hora"
                        Text("Finaliza: $dateText", modifier = Modifier.weight(1f))

                        OutlinedButton(
                            onClick = {
                                // 1) Elegir FECHA
                                val now = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        // 2) Después de elegir fecha, elegimos HORA
                                        val defaultHour = now.get(Calendar.HOUR_OF_DAY)
                                        val defaultMinute = now.get(Calendar.MINUTE)

                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                val cal = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, year)
                                                    set(Calendar.MONTH, month)
                                                    set(Calendar.DAY_OF_MONTH, day)
                                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                    set(Calendar.MINUTE, minute)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                selectedEndsAtMs = cal.timeInMillis
                                            },
                                            defaultHour,
                                            defaultMinute,
                                            true // 24h
                                        ).show()
                                    },
                                    now.get(Calendar.YEAR),
                                    now.get(Calendar.MONTH),
                                    now.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) { Text("Elegir") }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ends = selectedEndsAtMs ?: return@Button
                        onAddExam(newTitle, ends)
                        newTitle = ""
                        selectedEndsAtMs = null
                        showCreateDialog = false
                    },
                    enabled = newTitle.trim().isNotEmpty() && selectedEndsAtMs != null
                ) { Text("Guardar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showFinishedDialog) {
        AlertDialog(
            onDismissRequest = { showFinishedDialog = false },
            title = { Text("Examen terminado") },
            text = { Text("Este examen ya pasó. Solo se puede abrir el cronómetro si está “En proceso”.") },
            confirmButton = { Button(onClick = { showFinishedDialog = false }) { Text("OK") } }
        )
    }
}

@Composable
private fun ExamCard(
    exam: Exam,
    nowMs: Long,
    onOpenInProgressExam: (Exam) -> Unit,
    onOpenFinishedExam: () -> Unit
) {
    val inProgress = nowMs < exam.endsAtEpochMs

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (inProgress) onOpenInProgressExam(exam) else onOpenFinishedExam()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exam.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Finaliza: ${formatDateTime(exam.endsAtEpochMs)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            StatusPill(
                text = if (inProgress) "En proceso" else "Terminado",
                isPositive = inProgress
            )
        }
    }
}
@Composable
private fun StatusPill(text: String, isPositive: Boolean) {
    val bg = if (isPositive) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceVariant

    val fg = if (isPositive) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = bg,
        contentColor = fg,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatDateTime(epochMs: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMs))
}