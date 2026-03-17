package com.example.pearpressure.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

// Las pestañas principales de la app
enum class MainTab { HOME, RANKING, FRIENDS, PROFILE }

// Cada item del menú: pestaña + texto + icono (editable)
data class TabItem(
    val tab: MainTab,
    val label: String,
    val icon: ImageVector
)

// EDITAR AQUI ICONOS Y NOMBRES CUANDO QUERAMOS
val MainTabs = listOf(
    TabItem(MainTab.HOME, "Inicio", Icons.Filled.Home),
    TabItem(MainTab.RANKING, "Ranking", Icons.Filled.Star),
    TabItem(MainTab.FRIENDS, "Amigos", Icons.Filled.People),
    TabItem(MainTab.PROFILE, "Perfil", Icons.Filled.Person),
)