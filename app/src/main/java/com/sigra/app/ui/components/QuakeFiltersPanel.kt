package com.sigra.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sigra.app.data.QuakeFilters
import com.sigra.app.data.QuakeOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuakeFiltersSheet(
    title: String,
    filters: QuakeFilters,
    hasLocation: Boolean,
    locating: Boolean,
    onLocation: () -> Unit,
    onRadius: (Int) -> Unit,
    onSource: (String?) -> Unit,
    onOrder: (QuakeOrder) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    showOrder: Boolean = true,
    onHours: (Int) -> Unit = {},
    onMinMagnitude: (Double?) -> Unit = {},
    extraContent: @Composable () -> Unit = {},
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle(title, "Los cambios se aplican al instante al mapa y al listado.")
            QuakeFiltersPanel(filters, hasLocation, locating, onLocation, onRadius, onSource, onOrder, showOrder)
            FilterMenu("Periodo", QuakeFilters.periods[filters.hours].orEmpty()) { close ->
                QuakeFilters.periods.forEach { (hours, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { onHours(hours); close() })
                }
            }
            FilterMenu("Magnitud mínima", filters.minMagnitude?.let { "M $it" } ?: "Todas") { close ->
                QuakeFilters.magnitudes.forEach { magnitude ->
                    DropdownMenuItem(text = { Text(magnitude?.let { "M $it o más" } ?: "Todas las magnitudes") },
                        onClick = { onMinMagnitude(magnitude); close() })
                }
            }
            extraContent()
            if (filters != QuakeFilters()) {
                TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Restablecer filtros") }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Listo") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuakeFiltersPanel(
    filters: QuakeFilters,
    hasLocation: Boolean,
    locating: Boolean = false,
    onLocation: () -> Unit,
    onRadius: (Int) -> Unit,
    onSource: (String?) -> Unit,
    onOrder: (QuakeOrder) -> Unit,
    showOrder: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Fuente oficial", style = MaterialTheme.typography.titleSmall)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf<String?>(null) + QuakeFilters.sources).forEach { source ->
                FilterChip(selected = filters.source == source, onClick = { onSource(source) }, label = { Text(source ?: "Todas") })
            }
        }
        FilterMenu("Radio", if (filters.radiusKm == 0) "Todo el mundo" else "${filters.radiusKm} km") { close ->
            QuakeFilters.radii.forEach { radius ->
                DropdownMenuItem(text = { Text(if (radius == 0) "Todo el mundo" else "$radius km") },
                    enabled = radius == 0 || hasLocation, onClick = { onRadius(radius); close() })
            }
        }
        if (showOrder) {
            FilterMenu("Orden", filters.order.label) { close ->
                QuakeOrder.entries.forEach { order ->
                    DropdownMenuItem(text = { Text(order.label) }, enabled = order != QuakeOrder.DISTANCE || hasLocation,
                        onClick = { onOrder(order); close() })
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        OutlinedButton(onClick = onLocation, enabled = !locating, modifier = Modifier.fillMaxWidth()) {
            if (locating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(if (locating) "Buscando ubicación…" else if (hasLocation) "Actualizar mi ubicación" else "Usar mi ubicación")
        }
        Text(if (hasLocation) "Las distancias parten de la ubicación guardada de este teléfono."
            else "Permite la ubicación para filtrar por radio y ordenar por cercanía.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FilterMenu(label: String, selected: String, items: @Composable (() -> Unit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: $selected") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { items { expanded = false } }
    }
}
