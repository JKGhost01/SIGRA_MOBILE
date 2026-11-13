package com.sigra.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME("inicio", "Inicio", Icons.Default.Home),
    MAP("mapa", "Mapa", Icons.Default.Map),
    QUAKES("sismos", "Sismos", Icons.AutoMirrored.Filled.ViewList),
    ALERTS("alertas", "Alertas", Icons.Default.NotificationsActive),
    SETTINGS("ajustes", "Ajustes", Icons.Default.Settings),
}
