package com.example.pearpressure.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StopwatchSession(
    val durationMs: Long,
    val finishedAtEpochMs: Long
)

/** Change icons here later (single place). */
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

@Composable
fun StopwatchScreen(
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

    // -----------------------------
    // Quick actions state
    // -----------------------------
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var poopCount by remember { mutableStateOf(0) }

    val coffeeCounts = remember { mutableStateMapOf<CoffeeType, Int>() }
    val boostCounts = remember { mutableStateMapOf<BoostType, Int>() }
    var waterTotalLiters by remember { mutableStateOf(0.0) }

    val coffeeTotal = coffeeCounts.values.sum()
    val boostTotal = boostCounts.values.sum()

    // Dialog state
    var showPoopConfirm by remember { mutableStateOf(false) }
    var showCoffeeDialog by remember { mutableStateOf(false) }
    var selectedCoffeeType by remember { mutableStateOf(CoffeeType.LATTE) }

    var showBoostDialog by remember { mutableStateOf(false) }
    var selectedBoostType by remember { mutableStateOf(BoostType.RED_BULL) }

    var showWaterDialog by remember { mutableStateOf(false) }
    var waterInput by remember { mutableStateOf("") }
    var waterError by remember { mutableStateOf<String?>(null) }

    // Central handler (Option 2 pattern)
    val onQuickAction: (String) -> Unit = { action ->
        when (action) {
            "poop" -> {
                showPoopConfirm = true
            }
            "coffee" -> showCoffeeDialog = true
            "boost" -> showBoostDialog = true
            "water" -> {
                waterInput = ""
                waterError = null
                showWaterDialog = true
            }
        }
    }

    // -----------------------------
    // UI
    // -----------------------------
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
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
                        text = "Time",
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

            if (!bottomInfoText.isNullOrBlank()) {
                Text(
                    text = bottomInfoText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Start / Pause / Finish
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
                ) { Text(if (displayMs > 0L) "Continue" else "Start") }

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

            // Quick actions (above History)
            Text(
                text = "Quick actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // 2x2 grid (same size, same shape)
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

            // History header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { sessions.clear() }, enabled = sessions.isNotEmpty()) {
                    Text("Delete")
                }
            }

            // History list
            if (sessions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("No records yet", fontWeight = FontWeight.SemiBold)
                        Text("Press Finish to save time and date.")
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
    if (showPoopConfirm) {
        AlertDialog(
            onDismissRequest = { showPoopConfirm = false },
            title = { Text("Confirm") },
            text = { Text("Are you sure you want to add 1 poop?") },
            confirmButton = {
                Button(
                    onClick = {
                        poopCount += 1
                        showPoopConfirm = false
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPoopConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // -----------------------------
    // Coffee dialog (choose type)
    // -----------------------------
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
                            val count = coffeeCounts[type] ?: 0
                            Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = coffeeCounts[selectedCoffeeType] ?: 0
                        coffeeCounts[selectedCoffeeType] = current + 1
                        showCoffeeDialog = false
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCoffeeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // -----------------------------
    // Boost dialog (choose drink)
    // -----------------------------
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
                            val count = boostCounts[type] ?: 0
                            Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = boostCounts[selectedBoostType] ?: 0
                        boostCounts[selectedBoostType] = current + 1
                        showBoostDialog = false
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBoostDialog = false }) { Text("Cancel") }
            }
        )
    }

    // -----------------------------
    // Water dialog (input liters)
    // -----------------------------
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
                Button(
                    onClick = {
                        val parsed = waterInput.trim().replace(",", ".").toDoubleOrNull()
                        if (parsed == null || parsed <= 0.0) {
                            waterError = "Please enter a valid number > 0"
                            return@Button
                        }
                        waterTotalLiters += parsed
                        showWaterDialog = false

                        scope.launch {
                            snackbarHostState.showSnackbar("Added ${formatLiters(parsed)}")
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showWaterDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Fixed icon size + fixed icon slot:
 * every button icon looks identical in size, shape, and alignment.
 */
@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emoji: String? = null,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    val iconBoxSize = 24.dp
    val iconSize = 16.dp
    val emojiSizeSp = 16.sp

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
                modifier = Modifier.size(iconBoxSize),
                contentAlignment = Alignment.Center
            ) {
                when {
                    icon != null -> Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(iconSize)
                    )
                    !emoji.isNullOrBlank() -> Text(
                        text = emoji,
                        style = TextStyle(fontSize = emojiSizeSp, lineHeight = emojiSizeSp)
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

private fun formatLiters(liters: Double): String {
    // 0.5 -> "0.5 L", 1.0 -> "1.0 L"
    return String.format(Locale.getDefault(), "%.1f L", liters)
}