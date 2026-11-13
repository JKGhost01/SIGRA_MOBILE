package com.sigra.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigra.app.data.SismoEvent
import com.sigra.app.ui.components.EmptyState
import com.sigra.app.ui.components.Panel
import com.sigra.app.ui.components.QuakeDetailSheet
import com.sigra.app.ui.components.RefreshAction
import com.sigra.app.ui.components.ScreenHeader
import com.sigra.app.ui.components.SectionTitle
import com.sigra.app.ui.components.SismoRow
import com.sigra.app.ui.components.StatTile
import com.sigra.app.ui.components.SyncNotice
import com.sigra.app.ui.components.rememberMonitoringAction
import com.sigra.app.ui.theme.DataFont
import com.sigra.app.util.Geo
import com.sigra.app.viewmodel.MainViewModel

@Composable
fun HomeScreen(vm: MainViewModel, onOpenAlerts: () -> Unit, onOpenMap: () -> Unit, onOpenQuakes: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val filters by vm.quakeFilters.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val events by vm.sismos.collectAsStateWithLifecycle()
    val loading by vm.cargando.collectAsStateWithLifecycle()
    val eventsStatus by vm.eventsStatus.collectAsStateWithLifecycle()
    val statsStatus by vm.statsStatus.collectAsStateWithLifecycle()
    val changingMonitoring by vm.changingMonitoring.collectAsStateWithLifecycle()
    val monitoringAction = rememberMonitoringAction(vm)
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val strongest = events.maxByOrNull { it.magnitud }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            ScreenHeader("SIGRA", "OBSERVATORIO SÍSMICO") { RefreshAction(loading) { vm.refresh() } }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Actividad oficial", filters.summary)
                SyncNotice(eventsStatus) { vm.refresh() }
                if (strongest != null) {
                    FeaturedQuake(strongest, events.size) { selectedKey = "${strongest.fuente}:${strongest.id}" }
                } else {
                    EmptyState(if (eventsStatus.loading) "Consultando actividad" else if (eventsStatus.error != null)
                        "Catálogo no disponible" else "Sin eventos en esta selección",
                        "Consulta Sismos para cambiar el periodo, la magnitud o las fuentes.")
                }
            }
        }
        item { HomeShortcuts(onOpenMap, onOpenQuakes) }
        item {
            MonitorCard(settings.monitoringEnabled, changingMonitoring, settings.monitoringIssue) {
                monitoringAction(!settings.monitoringEnabled)
            }
        }
        item {
            Card(onClick = onOpenAlerts, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(if (settings.alertInbox.unreadCount > 0) "${settings.alertInbox.unreadCount} avisos sin leer" else "Tu historial de alertas",
                            style = MaterialTheme.typography.titleSmall)
                        Text("Avisos recibidos y guardados en este teléfono", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Red ciudadana", "Teléfonos conectados a SIGRA")
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("En línea", if (statsStatus.updatedAt == null) "—" else "${stats?.stats?.devices_online ?: 0}", Modifier.weight(1f).fillMaxHeight())
                    StatTile("Monitoreando", if (statsStatus.updatedAt == null) "—" else "${stats?.stats?.devices_monitoring ?: 0}", Modifier.weight(1f).fillMaxHeight())
                }
                SyncNotice(statsStatus) { vm.refresh() }
            }
        }
        if (events.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Explora tu selección", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onOpenQuakes) { Text("Ver todos") }
                }
            }
            items(events.take(3), key = { "${it.fuente}:${it.id}" }) { event ->
                SismoRow(event) { selectedKey = "${event.fuente}:${event.id}" }
            }
        }
        item {
            Panel {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Prepárate, mantén la calma", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(8.dp))
                Text("Si sientes movimiento: agáchate, cúbrete y sujétate. Sigue las indicaciones de las autoridades.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("SIGRA es experimental y no garantiza una alerta anticipada.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    events.firstOrNull { "${it.fuente}:${it.id}" == selectedKey }?.let { event ->
        QuakeDetailSheet(event, onDismiss = { selectedKey = null })
    }
}

@Composable
internal fun FeaturedQuake(event: SismoEvent, count: Int, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("MAYOR MAGNITUD RECIBIDA", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(Geo.formatoMagnitud(event.magnitud), style = MaterialTheme.typography.displaySmall,
                fontFamily = DataFont, fontWeight = FontWeight.Medium)
            Text(event.ubicacion, style = MaterialTheme.typography.titleMedium)
            Text("${event.fuente} · ${com.sigra.app.ui.components.formatearFechaHora(event.fecha_iso ?: event.fecha)}",
                style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$count eventos en tu selección", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Consultar evento", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun HomeShortcuts(onOpenMap: () -> Unit, onOpenQuakes: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stack = maxWidth < 340.dp || LocalDensity.current.fontScale > 1.2f
        if (stack) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickLink("Mapa", "Explora epicentros", Icons.Default.Map, Modifier.fillMaxWidth(), onOpenMap)
                QuickLink("Sismos", "Catálogo oficial", Icons.AutoMirrored.Filled.ViewList, Modifier.fillMaxWidth(), onOpenQuakes)
            }
        } else {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickLink("Mapa", "Explora epicentros", Icons.Default.Map, Modifier.weight(1f).fillMaxHeight(), onOpenMap)
                QuickLink("Sismos", "Catálogo oficial", Icons.AutoMirrored.Filled.ViewList, Modifier.weight(1f).fillMaxHeight(), onOpenQuakes)
            }
        }
    }
}

@Composable
internal fun QuickLink(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun MonitorCard(active: Boolean, changing: Boolean, issue: String?, onToggle: () -> Unit) {
    Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp).size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sensor del teléfono", style = MaterialTheme.typography.titleMedium)
                Text(if (active) "Monitoreo habilitado" else "Monitoreo desactivado", style = MaterialTheme.typography.bodySmall,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(issue ?: if (active) "Deja el teléfono quieto y con batería para aportar detecciones."
            else "Participa con el acelerómetro de tu teléfono. Puedes explorar el catálogo sin activarlo.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (issue != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onToggle, enabled = !changing, modifier = Modifier.fillMaxWidth()) {
            Text(if (changing) "Aplicando cambio…" else if (active) "Detener monitoreo" else "Activar monitoreo")
        }
    }
}
