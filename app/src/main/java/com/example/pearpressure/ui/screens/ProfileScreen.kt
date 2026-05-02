package com.example.pearpressure.ui.screens

import android.annotation.SuppressLint
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.pearpressure.MainViewModel
import com.example.pearpressure.RankingScope
import com.example.pearpressure.R
import androidx.compose.ui.res.stringResource

@SuppressLint("DefaultLocale")
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    // Load profile when entering this screen
    LaunchedEffect(Unit) {
        viewModel.loadCurrentUserProfile()
        // Load Ranking function to have updated information
        viewModel.loadRanking(scope = RankingScope.TOTAL)
    }

    // Observe profile so UI recomposes when data arrives
    val currentUserProfile by viewModel.currentUserProfile.collectAsState()
    val rankingEntries by viewModel.rankingEntries.collectAsState()

    val email = currentUserProfile?.email ?: viewModel.getCurrentUserEmail()
    val username = currentUserProfile?.username ?: stringResource(R.string.profile_no_username)
    val totalStudyTime = currentUserProfile?.totalStudyTime ?: 0L
    // Streak
    val streak = currentUserProfile?.currentStreak ?: 0

    // Reality Gap Logic (Calculated with expected vs final grade)
    val myRanking = rankingEntries.find { it.uid == viewModel.getCurrentUserId() }
    val realityGap = myRanking?.avgAccuracy ?: 0.0
    val gapValueColor = if (realityGap >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)

    // Badge Logic (Calculated according to hours studied)
    val totalHours = totalStudyTime / 3600000.0
    val (levelBadge, badgeColor) = when {
        totalHours < 1  -> stringResource(R.string.profile_badge_mini_pear)   to Color(0xFFF44336)
        totalHours < 10 -> stringResource(R.string.profile_badge_focus_pear)  to Color(0xFFFF9800)
        totalHours < 50 -> stringResource(R.string.profile_badge_master_pear) to Color(0xFF8BC34A)
        else            -> stringResource(R.string.profile_badge_gold_pear)   to Color(0xFFDAA520)
    }

    // Study time formatting
    val totalSeconds = totalStudyTime / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    val timeFormatted = when {
        hours > 0   -> stringResource(R.string.profile_time_hours_minutes, hours, minutes)
        minutes > 0 -> stringResource(R.string.profile_time_minutes, minutes)
        else        -> stringResource(R.string.profile_time_less_than_one_min)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.profile_screen_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Circular Profile Display
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

        // Badge
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
            text = stringResource(R.string.profile_logged_in_as),
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

        // Streak, Total time studied, alltime reality gap
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
                        text = if (streak > 0) stringResource(R.string.profile_streak_active, streak)
                        else stringResource(R.string.profile_streak_inactive),
                        color = streakColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                // Total time studied
                Text(
                    text = stringResource(R.string.profile_total_time_studied, timeFormatted),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // All Time Reality Gap
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildAnnotatedString {
                        val sign = if (realityGap >= 0) "+" else ""
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)) {
                                    append(stringResource(R.string.profile_reality_gap_label))
                                }
                                withStyle(style = SpanStyle(color = gapValueColor, fontWeight = FontWeight.Bold)) {
                                    append("$sign${String.format("%.2f", realityGap)} pts")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
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
            Text(stringResource(R.string.profile_button_sign_out), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}