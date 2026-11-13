package com.sigra.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigra.app.data.SeismicAlert
import com.sigra.app.data.SyncStatus
import com.sigra.app.data.isValidForInbox
import com.sigra.app.ui.components.AlertCard
import com.sigra.app.ui.components.EmptyState
import com.sigra.app.ui.components.Panel
import com.sigra.app.ui.components.RefreshAction
import com.sigra.app.ui.components.ScreenHeader
import com.sigra.app.ui.components.SectionTitle
import com.sigra.app.ui.components.SyncNotice
import com.sigra.app.ui.components.etiquetaSeveridad
import com.sigra.app.ui.components.formatearFechaHora
import com.sigra.app.ui.components.fuente
import com.sigra.app.ui.components.rememberAlertTime
import com.sigra.app.util.Geo
import com.sigra.app.util.arrivalLabel
import com.sigra.app.util.confidenceDescription
import com.sigra.app.viewmodel.MainViewModel

private data class AlertItem(val alert: SeismicAlert, val read: Boolean = true, val receivedAtMs: Long? = null, val temporary: Boolean = false)

@Composable
fun AlertsScreen(vm: MainViewModel) {
    val stats by vm.stats.collectAsStateWithLifecycle()
    val liveAlerts by vm.alertas.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val loading by vm.cargando.collectAsStateWithLifecycle()
    val alertsStatus by vm.alertsStatus.collectAsStateWithLifecycle()
    val statsStatus by vm.statsStatus.collectAsStateWithLifecycle()
    val historyError by vm.historyError.collectAsStateWithLifecycle()
    var selectedUuid by rememberSaveable { mutableStateOf<String?>(null) }
    var history by rememberSaveable { mutableStateOf(true) }
    var unreadOnly by rememberSaveable { mutableStateOf(false) }
    var source by rememberSaveable { mutableStateOf<String?>(null) }
    var actionsOpen by rememberSaveable { mutableStateOf(false) }
    var sourceOpen by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val inbox = settings.alertInbox
    val all = remember(inbox, stats, history, liveAlerts, historyError) {
        if (history) {
            val known = inbox.entries.map { it.alert.uuid }.toSet()
            val temporary = if (historyError == null) emptyList() else liveAlerts
                .filter { it.isValidForInbox() && it.uuid !in known && it.uuid !in inbox.processedIds }
                .distinctBy { it.uuid }.map { AlertItem(it, temporary = true) }
            temporary + inbox.entries.map { AlertItem(it.alert, it.read, it.firstReceivedAtMs) }
        }
        else (stats?.recent_alerts ?: emptyList()).filter { it.uuid.isNotBlank() }.distinctBy { it.uuid }
            .sortedByDescending { it.origin_time_ms }.map { AlertItem(it) }
    }
    val visible = remember(all, source, history, unreadOnly) {
        all.filter { (source == null || it.alert.source.equals(source, ignoreCase = true)) && (!history || !unreadOnly || !it.read) }
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHeader("Alertas", "Tus avisos y los de la red ciudadana.") {
                Row {
                    RefreshAction(loading) { vm.refresh() }
                    if (history) Box {
                        IconButton(onClick = { actionsOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Acciones del historial") }
                        DropdownMenu(expanded = actionsOpen, onDismissRequest = { actionsOpen = false }) {
                            DropdownMenuItem(text = { Text("Marcar todas como leídas") }, enabled = inbox.unreadCount > 0,
                                onClick = { actionsOpen = false; vm.markAllAlertsRead() })
                            DropdownMenuItem(text = { Text("Vaciar historial") }, enabled = inbox.entries.isNotEmpty(),
                                onClick = { actionsOpen = false; confirmClear = true })
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = history, onClick = { history = true }, label = { Text("Recibidas") })
                FilterChip(selected = !history, onClick = { history = false }, label = { Text("Red pública") })
            }
        }
        item {
            Panel {
                if (history) {
                    SectionTitle("${inbox.unreadCount} sin leer", "Se conservan hasta 50 avisos en este teléfono")
                    Spacer(Modifier.height(8.dp))
                    if (!settings.hasLocation) Text("Usa Mi ubicación en el Mapa para consultar avisos cercanos.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !unreadOnly, onClick = { unreadOnly = false }, label = { Text("Todas") })
                        FilterChip(selected = unreadOnly, onClick = { unreadOnly = true }, label = { Text("Sin leer") })
                    }
                } else {
                    SectionTitle("Avisos públicos de la red")
                    Spacer(Modifier.height(8.dp))
                    Text("Estos avisos pueden corresponder a otros lugares. No indican por sí solos que haya riesgo en tu ubicación y no se añaden a tu historial personal.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (history && historyError != null) {
            item { SyncNotice(SyncStatus(error = historyError)) { vm.refresh() } }
        }
        if (history && settings.hasLocation) {
            item { SyncNotice(alertsStatus) { vm.refresh() } }
        }
        if (!history) {
            item { SyncNotice(statsStatus) { vm.refresh() } }
        }
        item {
            val sources = listOf(null to "Todas las fuentes", "crowd" to "Red ciudadana", "sgc" to "SGC", "usgs" to "USGS",
                "emsc" to "EMSC", "geonet" to "GeoNet", "geofon" to "GEOFON", "ingv" to "INGV",
                "simulation" to "Pruebas", "unknown" to "Sin fuente")
            Box {
                OutlinedButton(onClick = { sourceOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Fuente: ${sources.firstOrNull { it.first == source }?.second ?: "Todas"}")
                }
                DropdownMenu(expanded = sourceOpen, onDismissRequest = { sourceOpen = false }) {
                    sources.forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { source = value; sourceOpen = false })
                    }
                }
            }
        }
        item { SectionTitle("${visible.size} avisos", "Revisa la hora del evento y la fecha de recepción") }
        if (visible.isEmpty()) {
            item {
                when {
                    history && unreadOnly && inbox.entries.isNotEmpty() -> EmptyState("Estás al día", "No hay avisos sin leer con estos filtros.")
                    source != null -> EmptyState("Sin avisos con esta fuente", "Selecciona todas las fuentes para ver más avisos.")
                    history -> EmptyState("Tu historial empieza aquí", "Los avisos recibidos se guardarán en este teléfono. La ausencia de avisos no indica ausencia de riesgo sísmico.")
                    statsStatus.loading -> EmptyState("Consultando avisos públicos", "La información aparecerá cuando termine la consulta.")
                    statsStatus.error != null -> EmptyState("No pudimos consultar la red", "Puedes volver a intentarlo desde el aviso de conexión.")
                    else -> EmptyState("Sin avisos públicos", "Vuelve a consultar más tarde.")
                }
            }
        } else {
            items(visible, key = { it.alert.uuid }) { item ->
                if (item.temporary) Text("Este aviso aún no se guardó en el teléfono.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
                AlertCard(item.alert, unread = !item.read, receivedAtMs = item.receivedAtMs) {
                    if (history) {
                        if (item.temporary) vm.openAlert(item.alert) else vm.markAlertRead(item.alert.uuid)
                    }
                    selectedUuid = item.alert.uuid
                }
            }
        }
        item {
            Panel {
                SectionTitle("¿Qué significa la estimación?")
                Spacer(Modifier.height(8.dp))
                Text("Los tiempos y magnitudes pueden cambiar. Un aviso puede llegar después del movimiento; SIGRA no garantiza una alerta anticipada.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Si sientes movimiento, protégete y sigue las indicaciones oficiales. No esperes una notificación.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    all.firstOrNull { it.alert.uuid == selectedUuid }?.let { item ->
        AlertDetailDialog(item) { selectedUuid = null }
    }
    if (confirmClear) {
        AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("¿Vaciar el historial?") },
            text = { Text("Se borrarán los avisos guardados en este teléfono. Los datos del servidor y tus preferencias se conservan. Los mismos avisos no volverán a notificarse por vaciar el historial.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearAlertHistory() }) { Text("Vaciar historial") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } })
    }
}

@Composable
private fun AlertDetailDialog(item: AlertItem, onDismiss: () -> Unit) {
    val alert = item.alert
    val now = rememberAlertTime(alert.uuid)
    AlertDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar detalles") } },
        title = { Text("${Geo.formatoMagnitud(alert.magnitude)} · ${etiquetaSeveridad(alert.severity)}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(alert.arrivalLabel(now), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text("Fuente: ${fuente(alert.source)}")
                (alert.origin_time ?: alert.created_at)?.let { Text("Evento: ${formatearFechaHora(it)}") }
                item.receivedAtMs?.takeIf { it > 0 }?.let {
                    Text("Recibida: ${formatearFechaHora(java.time.Instant.ofEpochMilli(it).toString())}")
                }
                Text("${Geo.formatoDistancia(alert.distance_km)} · ${alert.confidenceDescription()}")
                if (alert.device_count > 0) Text("${alert.device_count} teléfonos participantes")
                Text("Agáchate, protege cabeza y cuello y sujétate bajo una mesa resistente. Aléjate de ventanas y objetos que puedan caer.")
                Text("Estimación experimental. Sigue las instrucciones de las autoridades.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        })
}
