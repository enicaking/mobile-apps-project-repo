package com.example.pearpressure.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

// Main tabs of the app
enum class MainTab { HOME, RANKING, FRIENDS, PROFILE }

// Bottom menu item: tab + label + icon
data class TabItem(
    val tab: MainTab,
    val label: String,
    val icon: ImageVector
)

// Edit icons and labels
val MainTabs = listOf(
    TabItem(MainTab.HOME, "Home", Icons.Filled.Home),
    TabItem(MainTab.RANKING, "Ranking", Icons.Filled.Star),
    TabItem(MainTab.FRIENDS, "Friends", Icons.Filled.People),
    TabItem(MainTab.PROFILE, "Profile", Icons.Filled.Person),
)