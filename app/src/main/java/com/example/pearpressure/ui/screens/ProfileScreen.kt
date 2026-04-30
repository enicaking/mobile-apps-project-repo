package com.example.pearpressure.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Whatshot //FOR STREAK
import androidx.compose.material.icons.filled.Star //FOR BADGES
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // FOR COLOR
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.pearpressure.MainViewModel
import com.example.pearpressure.RankingScope

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    // Load profile when entering this screen
    LaunchedEffect(Unit) {
        viewModel.loadCurrentUserProfile()
        // Cargamos ranking para tener los datos de notas actualizados
        viewModel.loadRanking(scope = RankingScope.TOTAL)
    }

    // Observe profile so UI recomposes when data arrives
    val currentUserProfile by viewModel.currentUserProfile.collectAsState()
    val rankingEntries by viewModel.rankingEntries.collectAsState()

    val email = currentUserProfile?.email ?: viewModel.getCurrentUserEmail()
    val username = currentUserProfile?.username ?: "No username"
    val totalStudyTime = currentUserProfile?.totalStudyTime ?: 0L
    // Recuperamos la racha del perfil
    val streak = currentUserProfile?.currentStreak ?: 0

    // LOGICA PARA REALITY GAP (Diferencia Real vs Esperada)
    val myRanking = rankingEntries.find { it.uid == viewModel.getCurrentUserId() }
    val realityGap = myRanking?.avgAccuracy ?: 0.0
    val gapValueColor = if (realityGap >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)

    // LOGICA PARA EL BADGE (Nivel según horas)
    val totalHours = totalStudyTime / 3600000.0
    val (levelBadge, badgeColor) = when {
        totalHours < 1 -> "Mini Pear 🍐" to Color(0xFFF44336)
        totalHours < 10 -> "Focus Pear 🍐" to Color(0xFFFF9800)
        totalHours < 50 -> "Master Pear 🍐" to Color(0xFF8BC34A)
        else -> "Gold Pear 🍐" to Color(0xFFDAA520)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "User Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // PERFIL AHORA ES CIRCULAR CON INICIAL
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = username.take(1).uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // MOSTRAR BADGE DE NIVEL
        Surface(
            color = badgeColor.copy(alpha = 0.2f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = levelBadge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = badgeColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Logged in as:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        Text(
            text = username,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(24.dp))

        // MOSTRAR RACHA, TIEMPO Y REALITY GAP
        val streakColor = if (streak > 0) Color(0xFFFF9800) else Color.Gray

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = streakColor.copy(alpha = 0.05f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Whatshot, contentDescription = null, tint = streakColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (streak > 0) "Study Streak: $streak days 🔥" else "No active streak ❄️",
                        color = streakColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                // ESTADISTICA DE TIEMPO
                Text(
                    text = "Total time studied: ${formatStudyTime(totalStudyTime)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // SECCION ALL TIME REALITY GAP (Label negro, Valor en color)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)) {
                            append("All Time Reality Gap: ")
                        }
                        withStyle(style = SpanStyle(color = gapValueColor, fontWeight = FontWeight.Bold)) {
                            val sign = if (realityGap >= 0) "+" else ""
                            append("$sign${String.format("%.2f", realityGap)} pts")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.signOut { onLogout() }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatStudyTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes} min"
        else -> "Less than 1 min"
    }
}