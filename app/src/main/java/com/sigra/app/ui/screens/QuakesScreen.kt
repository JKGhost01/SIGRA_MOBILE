package com.sigra.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigra.app.ui.components.EmptyState
import com.sigra.app.ui.components.RefreshAction
import com.sigra.app.ui.components.Panel
import com.sigra.app.ui.components.QuakeDetailSheet
import com.sigra.app.ui.components.QuakeFiltersSheet
import com.sigra.app.ui.components.ScreenHeader
import com.sigra.app.ui.components.SectionTitle
import com.sigra.app.ui.components.SismoRow
import com.sigra.app.ui.components.SyncNotice
import com.sigra.app.ui.components.rememberLocationAction
import com.sigra.app.util.matchesSearch
import com.sigra.app.viewmodel.MainViewModel

@Composable
fun QuakesScreen(vm: MainViewModel, onOpenMap: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val filters by vm.quakeFilters.collectAsStateWithLifecycle()
    val allEvents by vm.sismos.collectAsStateWithLifecycle()
    val status by vm.eventsStatus.collectAsStateWithLifecycle()
    val locating by vm.locating.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val events = remember(allEvents, query) { allEvents.filter { it.matchesSearch(query) } }
    val requestLocation = rememberLocationAction(vm)

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ScreenHeader("Sismos", "Catálogo de seis fuentes oficiales") {
                Row {
                    RefreshAction(status.loading) { vm.refresh() }
                    IconButton(onClick = onOpenMap) { Icon(Icons.Default.Map, contentDescription = "Ver los eventos en el mapa") }
                }
            }
        }
        item {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar en esta lista") }, placeholder = { Text("Ciudad, región o fuente") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                } }, singleLine = true, shape = MaterialTheme.shapes.medium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(filters.order.label, style = MaterialTheme.typography.titleSmall)
                    Text(filters.summary,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = { filtersOpen = true }) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Filtros")
                }
            }
        }
        item { SyncNotice(status) { vm.refresh() } }
        item {
            SectionTitle(if (status.updatedAt == null && status.error != null) "Consulta no disponible" else "${events.size} eventos en esta lista",
                if (query.isBlank()) "Toca un evento para consultar sus detalles" else "Búsqueda dentro de los eventos recibidos")
        }
        if (allEvents.size >= 1000) {
            item { Text("Se alcanzó el límite de 1000 eventos. Usa fuente y radio para precisar la consulta.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        when {
            events.isEmpty() && status.loading -> item { EmptyState("Consultando actividad", "Estamos buscando eventos con los filtros seleccionados.") }
            events.isEmpty() && status.error != null -> item { EmptyState("No pudimos cargar los eventos", "Usa Reintentar en el aviso de conexión.") }
            events.isEmpty() && query.isNotBlank() -> item {
                Panel {
                    SectionTitle("Sin coincidencias", "Prueba otro nombre de ciudad o región.")
                    TextButton(onClick = { query = "" }) { Text("Limpiar búsqueda") }
                }
            }
            events.isEmpty() -> item {
                Panel {
                    SectionTitle("Sin eventos con estos filtros", "Prueba todas las fuentes o amplía el radio.")
                    TextButton(onClick = vm::resetQuakeFilters) { Text("Restablecer filtros") }
                }
            }
            else -> items(events, key = { "${it.fuente}:${it.id}" }) { event ->
                SismoRow(event) { selectedKey = "${event.fuente}:${event.id}" }
            }
        }
    }
    if (filtersOpen) {
        QuakeFiltersSheet("Filtrar sismos", filters, settings.hasLocation, locating, requestLocation,
            vm::setMapRadius, vm::setMapSource, vm::setMapOrder, vm::resetQuakeFilters, onDismiss = { filtersOpen = false },
            onHours = vm::setMapHours, onMinMagnitude = vm::setMapMinMagnitude)
    }
    allEvents.firstOrNull { "${it.fuente}:${it.id}" == selectedKey }?.let { event ->
        QuakeDetailSheet(event, onDismiss = { selectedKey = null })
    }
}
