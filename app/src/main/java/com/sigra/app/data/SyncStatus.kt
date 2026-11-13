package com.sigra.app.data

data class SyncStatus(
    val loading: Boolean = false,
    val error: String? = null,
    val updatedAt: Long? = null,
)

enum class AppAppearance(val label: String) {
    SYSTEM("Automático"), LIGHT("Claro"), DARK("Oscuro"),
}
