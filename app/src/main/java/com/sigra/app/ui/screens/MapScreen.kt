package com.sigra.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigra.app.ui.components.MapControls
import com.sigra.app.ui.components.QuakeDetailSheet
import com.sigra.app.ui.components.QuakeFiltersSheet
import com.sigra.app.ui.components.SigraMap
import com.sigra.app.ui.components.SyncNotice
import com.sigra.app.ui.components.rememberLocationAction
import com.sigra.app.ui.components.rememberSigraMapState
import com.sigra.app.util.hasExpired
import com.sigra.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(vm: MainViewModel, onOpenEvents: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val filters by vm.quakeFilters.collectAsStateWithLifecycle()
    val events by vm.sismos.collectAsStateWithLifecycle()
    val alerts by vm.alertas.collectAsStateWithLifecycle()
    val status by vm.eventsStatus.collectAsStateWithLifecycle()
    val locating by vm.locating.collectAsStateWithLifecycle()
    val mapState = rememberSigraMapState()
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var showAlerts by rememberSaveable { mutableStateOf(false) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val requestLocation = rememberLocationAction(vm) { lat, lng -> mapState.centerAt(lat, lng) }
    val visibleAlerts = if (showAlerts) alerts.filter { it.status == "active" && !it.hasExpired() } else emptyList()

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Mapa sísmico", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onOpenEvents) {
                        Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = "Abrir listado de sismos")
                    }
                    TextButton(onClick = { filtersOpen = true }) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Text("Filtros", Modifier.padding(start = 4.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(when {
                            status.updatedAt == null && status.loading -> "Consultando epicentros…"
                            status.error != null -> "Error al actualizar · ver estado"
                            else -> "${events.size} epicentros · ${filters.periodLabel}"
                        }, style = MaterialTheme.typography.labelMedium,
                            color = if (status.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        Text(filters.summary,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { infoOpen = true }) { Text("Estado") }
                }
            }
        }
        HorizontalDivider()
        SigraMap(
            modifier = Modifier.fillMaxWidth().weight(1f),
            userLat = settings.lastLat.takeIf { settings.hasLocation },
            userLng = settings.lastLng.takeIf { settings.hasLocation },
            alertas = visibleAlerts, sismos = events, radiusKm = filters.radiusKm,
            onEventClick = { selectedKey = "${it.fuente}:${it.id}" }, state = mapState,
        )
        HorizontalDivider()
        MapControls(locating = locating, hasEvents = events.isNotEmpty(),
            onZoomOut = mapState::zoomOut, onZoomIn = mapState::zoomIn,
            onLocation = requestLocation, onFitEvents = { mapState.showEvents(events) })
    }
    if (infoOpen) {
        ModalBottomSheet(onDismissRequest = { infoOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Estado del mapa", style = MaterialTheme.typography.titleLarge)
                SyncNotice(status) { vm.refresh() }
                Text("Cada círculo muestra la magnitud oficial. El color distingue intervalos de magnitud; no representa una predicción de riesgo en tu ubicación.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Gris: M < 3 · Turquesa: M 3–3.9 · Amarillo: M 4–4.9 · Naranja: M 5–5.9 · Rojo: M ≥ 6",
                    style = MaterialTheme.typography.bodySmall)
                Text("Toca un epicentro para consultar su magnitud, profundidad y fecha.",
                    style = MaterialTheme.typography.bodyMedium)
                if (events.isEmpty() && status.updatedAt != null && !status.loading && status.error == null) {
                    Text("No hay eventos con estos filtros. Prueba ampliar el radio.")
                }
                if (events.size >= 1000) Text("Límite de 1000 eventos. Usa los filtros para precisar la búsqueda.")
                Text(if (showAlerts) "Los avisos de la red están visibles." else "Los avisos de la red están ocultos. Puedes activarlos en Filtros y capas.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { infoOpen = false }, modifier = Modifier.fillMaxWidth()) { Text("Volver al mapa") }
            }
        }
    }
    if (filtersOpen) {
        QuakeFiltersSheet("Filtros y capas", filters, settings.hasLocation, locating, requestLocation,
            vm::setMapRadius, vm::setMapSource, vm::setMapOrder, vm::resetQuakeFilters,
            onDismiss = { filtersOpen = false }, showOrder = false,
            onHours = vm::setMapHours, onMinMagnitude = vm::setMapMinMagnitude) {
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth().toggleable(value = showAlerts, role = Role.Switch, onValueChange = { showAlerts = it }),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Avisos de la red", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Mostrar los avisos cercanos aún vigentes junto a los sismos oficiales.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = showAlerts, onCheckedChange = null)
            }
        }
    }
    events.firstOrNull { "${it.fuente}:${it.id}" == selectedKey }?.let { event ->
        QuakeDetailSheet(event, onDismiss = { selectedKey = null })
    }
}
