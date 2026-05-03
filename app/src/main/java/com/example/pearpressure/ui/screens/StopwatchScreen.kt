package com.example.pearpressure.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pearpressure.MainViewModel
import com.example.pearpressure.notifications.CounterNotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private object QuickIcons {
    val Water: ImageVector = Icons.Filled.WaterDrop
    val Coffee: ImageVector = Icons.Filled.LocalCafe
    val Boost: ImageVector = Icons.Filled.Bolt
}

private enum class CoffeeType(val label: String) {
    DECAF("Decaf"),
    LATTE("Latte"),
    CAPPUCCINO("Cappuccino"),
    MACCHIATO("Macchiato"),
    ESPRESSO("Espresso")
}

private enum class BoostType(val label: String) {
    RED_BULL("Red Bull"),
    MONSTER("Monster"),
    ENERGETI("Energeti")
}

// Public entry point (previously StopwatchPage) ────────────────────────────

@Composable
fun StopwatchPage(
    viewModel: MainViewModel,
    subjectName: String,
    examTitle: String,
    examId: String,
    endsAtEpochMs: Long,
    onBack: () -> Unit,
    onStudyStarted: () -> Unit = {}
) {
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val infoText = formatCountdownDaysHours(endsAtEpochMs - nowMs)

    LaunchedEffect(endsAtEpochMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(60_000)
        }
    }

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
                    TextButton(onClick = onBack) { Text("← Go back") }
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
                HorizontalDivider()
            }
        }

        // Stopwatch body
        StopwatchScreen(
            viewModel = viewModel,
            examId = examId,
            showTitle = false,
            bottomInfoText = infoText,
            onBack = onBack,
            onStudyStarted = onStudyStarted
        )
    }
}

//Internal stopwatch body (previously StopwatchScreen in the same package) ─

@Composable
fun StopwatchScreen(
    viewModel: MainViewModel,
    examId: String,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    bottomInfoText: String? = null,
    onBack: () -> Unit = {},
    onStudyStarted: () -> Unit = {}
) {
    var isRunning by remember { mutableStateOf(false) }
    var startElapsedMs by remember { mutableStateOf(0L) }
    var accumulatedMs by remember { mutableStateOf(0L) }
    var displayMs by remember { mutableStateOf(0L) }

    var showSummary by remember { mutableStateOf(false) }
    var lastSavedTime by remember { mutableStateOf(0L) }
    var lastSavedCoffee by remember { mutableStateOf(0) }
    var lastSavedWater by remember { mutableStateOf(0.0) }
    var lastSavedBoost by remember { mutableStateOf(0) }
    var lastSavedPoop by remember { mutableStateOf(0) }

    val allSessions by viewModel.sessions.collectAsState()
    val examParticipants by viewModel.examParticipants.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val outgoingRequests by viewModel.outgoingRequests.collectAsState()

    val sessions = remember(allSessions, examId) {
        allSessions.filter { it.examId == examId }
    }

    val currentUserId = viewModel.getCurrentUserId()
    val friendIds = remember(friends) { friends.map { it.uid }.toSet() }
    val pendingRequestIds = remember(outgoingRequests) { outgoingRequests.map { it.to.uid }.toSet() }

    LaunchedEffect(examId, allSessions.size) {
        viewModel.loadExamParticipants(examId)
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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var poopCount by remember { mutableStateOf(0) }

    val coffeeCounts = remember { mutableStateMapOf<CoffeeType, Int>() }
    val boostCounts = remember { mutableStateMapOf<BoostType, Int>() }
    var waterTotalLiters by remember { mutableStateOf(0.0) }

    val coffeeTotal = coffeeCounts.values.sum()
    val boostTotal = boostCounts.values.sum()

    var showPoopConfirm by remember { mutableStateOf(false) }
    var showCoffeeDialog by remember { mutableStateOf(false) }
    var selectedCoffeeType by remember { mutableStateOf(CoffeeType.LATTE) }

    var showBoostDialog by remember { mutableStateOf(false) }
    var selectedBoostType by remember { mutableStateOf(BoostType.RED_BULL) }

    var showWaterDialog by remember { mutableStateOf(false) }
    var waterInput by remember { mutableStateOf("") }
    var waterError by remember { mutableStateOf<String?>(null) }

    val onQuickAction: (String) -> Unit = { action ->
        when (action) {
            "poop" -> showPoopConfirm = true
            "coffee" -> showCoffeeDialog = true
            "boost" -> showBoostDialog = true
            "water" -> {
                waterInput = ""
                waterError = null
                showWaterDialog = true
            }
        }
    }

    val context = LocalContext.current

    var coffeeWarningShown by rememberSaveable { mutableStateOf(false) }
    var boostWarningShown by rememberSaveable { mutableStateOf(false) }

    val subjectNameForNotification = "Mobile Applications"
    val examTitleForNotification = "Project"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (showTitle) {
                Text(
                    text = "Stopwatch",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    onClick = {
                        val isFreshStart = !isRunning && displayMs == 0L
                        if (isFreshStart) onStudyStarted()

                        isRunning = true
                        startElapsedMs = SystemClock.elapsedRealtime()

                        CounterNotificationHelper.scheduleWaterReminder(
                            context = context.applicationContext,
                            subjectName = subjectNameForNotification,
                            examTitle = examTitleForNotification
                        )
                    },
                    enabled = !isRunning
                ) {
                    Text(if (displayMs > 0L) "Resume" else "Start")
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

                        CounterNotificationHelper.cancelWaterReminder(context.applicationContext)

                        lastSavedTime = displayMs
                        lastSavedCoffee = coffeeCounts.values.sum()
                        lastSavedWater = waterTotalLiters
                        lastSavedBoost = boostCounts.values.sum()
                        lastSavedPoop = poopCount

                        val waterGlasses = (waterTotalLiters / 0.25).toInt()

                        viewModel.saveSession(
                            examId = examId,
                            durationMs = displayMs,
                            water = waterGlasses,
                            coffee = lastSavedCoffee,
                            energy = lastSavedBoost,
                            bathroom = lastSavedPoop
                        )

                        showSummary = true

                        accumulatedMs = 0L
                        displayMs = 0L
                        poopCount = 0
                        waterTotalLiters = 0.0
                        coffeeCounts.clear()
                        boostCounts.clear()

                        coffeeWarningShown = false
                        boostWarningShown = false
                    },
                    enabled = displayMs > 0
                ) {
                    Text("Finish")
                }
            }

            Text(
                text = "Quick actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    emoji = "💩",
                    title = "Poop",
                    subtitle = poopCount.toString(),
                    onClick = { onQuickAction("poop") }
                )
                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    icon = QuickIcons.Water,
                    title = "Water",
                    subtitle = formatLiters(waterTotalLiters),
                    onClick = { onQuickAction("water") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    icon = QuickIcons.Coffee,
                    title = "Coffee",
                    subtitle = coffeeTotal.toString(),
                    onClick = { onQuickAction("coffee") }
                )
                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    icon = QuickIcons.Boost,
                    title = "Boost",
                    subtitle = boostTotal.toString(),
                    onClick = { onQuickAction("boost") }
                )
            }

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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sessions
                        .sortedByDescending { it.createdAtEpochMs }
                        .forEach { s ->
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

            Text(
                text = "Participants",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (examParticipants.isEmpty()) {
                Text("No participants found")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    examParticipants.forEach { participant ->
                        val isSelf = participant.uid == currentUserId
                        val isFriend = participant.uid in friendIds
                        val isPending = participant.uid in pendingRequestIds

                        ParticipantStudyCard(
                            name = participant.displayName,
                            studiedTimeText = formatStudyTimeCompact(participant.studiedTimeMs),
                            showAddFriend = !isSelf && !isFriend && !isPending,
                            showPending = !isSelf && !isFriend && isPending,
                            onAddFriend = { viewModel.sendFriendRequest(participant.uid) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showSummary) {
        AlertDialog(
            onDismissRequest = { showSummary = false },
            title = { Text("Session Saved!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Time studied: ${formatDuration(lastSavedTime)}")
                    Text("☕ Coffee: $lastSavedCoffee cups")
                    Text("💧 Water: ${formatLiters(lastSavedWater)}")
                    Text("⚡ Boost: $lastSavedBoost drinks")
                    Text("💩 Poop: $lastSavedPoop times")
                }
            },
            confirmButton = {
                Button(onClick = { showSummary = false }) { Text("Keep studying") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSummary = false
                    onBack()
                }) { Text("Go to Exams") }
            }
        )
    }

    if (showPoopConfirm) {
        AlertDialog(
            onDismissRequest = { showPoopConfirm = false },
            title = { Text("Confirm") },
            text = { Text("Are you sure you want to add 1 poop?") },
            confirmButton = {
                Button(onClick = {
                    poopCount += 1
                    showPoopConfirm = false
                }) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPoopConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showCoffeeDialog) {
        AlertDialog(
            onDismissRequest = { showCoffeeDialog = false },
            title = { Text("Coffee type") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CoffeeType.entries.forEach { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedCoffeeType == type),
                                onClick = { selectedCoffeeType = type }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(type.label)
                            Spacer(Modifier.weight(1f))
                            Text(
                                (coffeeCounts[type] ?: 0).toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    coffeeCounts[selectedCoffeeType] = (coffeeCounts[selectedCoffeeType] ?: 0) + 1

                    val newCoffeeTotal = coffeeCounts.values.sum()
                    if (newCoffeeTotal > 2 && !coffeeWarningShown) {
                        CounterNotificationHelper.showCoffeeWarning(
                            context = context.applicationContext,
                            coffeeTotal = newCoffeeTotal
                        )
                        coffeeWarningShown = true
                    }

                    showCoffeeDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCoffeeDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBoostDialog) {
        AlertDialog(
            onDismissRequest = { showBoostDialog = false },
            title = { Text("Boost drink") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BoostType.entries.forEach { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedBoostType == type),
                                onClick = { selectedBoostType = type }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(type.label)
                            Spacer(Modifier.weight(1f))
                            Text(
                                (boostCounts[type] ?: 0).toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    boostCounts[selectedBoostType] = (boostCounts[selectedBoostType] ?: 0) + 1

                    val newBoostTotal = boostCounts.values.sum()
                    if (newBoostTotal > 1 && !boostWarningShown) {
                        CounterNotificationHelper.showBoostWarning(
                            context = context.applicationContext,
                            boostTotal = newBoostTotal
                        )
                        boostWarningShown = true
                    }

                    showBoostDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBoostDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showWaterDialog) {
        AlertDialog(
            onDismissRequest = { showWaterDialog = false },
            title = { Text("Add water (liters)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = waterInput,
                        onValueChange = {
                            waterInput = it
                            waterError = null
                        },
                        label = { Text("Liters (e.g. 0.5)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    if (waterError != null) {
                        Text(
                            text = waterError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = waterInput.trim().replace(",", ".").toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) {
                        waterError = "Please enter a valid number > 0"
                        return@Button
                    }

                    waterTotalLiters += parsed

                    CounterNotificationHelper.scheduleWaterReminder(
                        context = context.applicationContext,
                        subjectName = subjectNameForNotification,
                        examTitle = examTitleForNotification
                    )

                    showWaterDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Added ${formatLiters(parsed)}")
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showWaterDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emoji: String? = null,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    FilledTonalButton(
        modifier = modifier.height(56.dp),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(16.dp)
                    )
                } else if (!emoji.isNullOrBlank()) {
                    Text(
                        text = emoji,
                        style = TextStyle(fontSize = 16.sp, lineHeight = 16.sp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ParticipantStudyCard(
    name: String,
    studiedTimeText: String,
    showAddFriend: Boolean,
    showPending: Boolean,
    onAddFriend: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = studiedTimeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                showPending -> {
                    OutlinedButton(onClick = {}, enabled = false) { Text("Pending") }
                }
                showAddFriend -> {
                    FilledTonalButton(onClick = onAddFriend) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add friend")
                        Spacer(Modifier.width(6.dp))
                        Text("Add friend")
                    }
                }
            }
        }
    }
}

//  Private helpers ───────────────────────────────────────────────────────────

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun formatDateTime(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))

private fun formatLiters(liters: Double): String =
    String.format(Locale.getDefault(), "%.1f L", liters)

private fun formatStudyTimeCompact(ms: Long): String {
    val totalMinutes = ms / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m studied"
        minutes > 0 -> "${minutes}m studied"
        else -> "Less than 1 min studied"
    }
}

private fun formatCountdownDaysHours(diffMs: Long): String {
    if (diffMs <= 0L) return "The exam has finished."
    val totalHours = diffMs / (1000L * 60 * 60)
    val days = totalHours / 24
    val hours = totalHours % 24
    return when {
        days > 0 && hours > 0 -> "$days days and $hours hours for the exam."
        days > 0 -> "$days days until the exam."
        else -> "$hours hours until the exam."
    }
}