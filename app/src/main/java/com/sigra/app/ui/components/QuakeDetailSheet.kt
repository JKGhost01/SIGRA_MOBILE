package com.sigra.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sigra.app.BuildConfig
import com.sigra.app.data.SismoEvent
import com.sigra.app.util.Geo
import com.sigra.app.util.webUrl
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuakeDetailSheet(event: SismoEvent, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var actionError by remember(event.fuente, event.id) { mutableStateOf<String?>(null) }
    val link = webUrl(event.official_url, BuildConfig.SIGRA_BASE_URL)
        ?: webUrl(event.detalle_url, BuildConfig.SIGRA_BASE_URL)
    val date = formatearFechaHora(event.fecha_iso ?: event.fecha)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("SISMO OFICIAL · ${event.fuente}", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(Geo.formatoMagnitud(event.magnitud), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(event.ubicacion.ifBlank { "Ubicación sin descripción" }, style = MaterialTheme.typography.titleLarge)
            Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            DetailLine("Profundidad", "${"%.1f".format(event.profundidad)} km")
            DetailLine("Distancia a tu ubicación", event.distancia_km?.let { "${"%.1f".format(it)} km" } ?: "Sin ubicación disponible")
            DetailLine("Coordenadas del epicentro", String.format(Locale.ROOT, "%.3f, %.3f", event.latitud, event.longitud))
            event.agency?.takeIf { it.isNotBlank() }?.let { DetailLine("Agencia", it) }
            event.mag_type?.takeIf { it.isNotBlank() }?.let { DetailLine("Tipo de magnitud", it) }
            if (event.citizen_reports_count > 0) {
                Text("${event.citizen_reports_count} participantes con reportes contrastados con el evento oficial.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text("Los datos pueden cambiar cuando la fuente oficial revise el evento.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            actionError?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if (link != null) {
                Button(onClick = {
                    actionError = null
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                        .onFailure { actionError = "No encontramos una aplicación para abrir el enlace." }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Consultar detalles en la web")
                }
            }
            OutlinedButton(onClick = {
                actionError = null
                val text = buildString {
                    append("${Geo.formatoMagnitud(event.magnitud)} · ${event.ubicacion}\n$date\n")
                    append("Profundidad: ${"%.1f".format(event.profundidad)} km · Fuente: ${event.fuente}\n")
                    link?.let { append("$it\n") }
                    append("Consultado en SIGRA. Sigue la información de las autoridades.")
                }
                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                runCatching { context.startActivity(Intent.createChooser(intent, "Compartir sismo")) }
                    .onFailure { actionError = "No se pudo abrir el menú para compartir." }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("Compartir evento")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cerrar detalles") }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
