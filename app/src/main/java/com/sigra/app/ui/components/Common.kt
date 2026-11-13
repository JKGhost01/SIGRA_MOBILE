package com.sigra.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sigra.app.data.SeismicAlert
import com.sigra.app.data.SismoEvent
import com.sigra.app.ui.theme.Amber
import com.sigra.app.ui.theme.Teal
import com.sigra.app.util.Geo
import com.sigra.app.util.arrivalLabel
import com.sigra.app.util.alertSourceLabel
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontFamily = com.sigra.app.ui.theme.DataFont, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SeverityBadge(severity: String, modifier: Modifier = Modifier) {
    val color = colorSeveridad(severity)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            etiquetaSeveridad(severity),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun AlertCard(alerta: SeismicAlert, unread: Boolean = false, receivedAtMs: Long? = null, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics {
            if (receivedAtMs != null) stateDescription = if (unread) "Sin leer" else "Leída"
        },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text((if (unread) "SIN LEER · " else "") + fuente(alerta.source), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${Geo.formatoMagnitud(alerta.magnitude)} · ${etiquetaSeveridad(alerta.severity)}",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(Geo.formatoDistancia(alerta.distance_km), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(alerta.arrivalLabel(), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            val time = tiempoRelativo(alerta.origin_time ?: alerta.created_at)
            if (time.isNotBlank()) Text(time, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            receivedAtMs?.takeIf { it > 0 }?.let {
                val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale("es", "CO")))
                Text("Recibida · $date", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SismoRow(evento: SismoEvent, onClick: () -> Unit) {
    val magnitudeColor = if (evento.nivel == 1) MaterialTheme.colorScheme.primary else colorPorNivel(evento.nivel)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(evento.fuente, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                Text(formatearFechaHora(evento.fecha_iso ?: evento.fecha), modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = MaterialTheme.shapes.small, color = magnitudeColor.copy(alpha = 0.12f)) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(String.format(Locale.ROOT, "%.1f", evento.magnitud), style = MaterialTheme.typography.titleLarge,
                            fontFamily = com.sigra.app.ui.theme.DataFont, color = magnitudeColor, fontWeight = FontWeight.Medium)
                        Text("MAGNITUD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(evento.ubicacion.ifBlank { "Ubicación sin descripción" }, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text("${evento.profundidad.toInt()} km de profundidad", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    evento.distancia_km?.let { distance ->
                        Text("A ${Geo.formatoDistancia(distance)} de ti", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (evento.citizen_reports_count > 0) {
                Text("Reportado por ${evento.citizen_reports_count} participantes", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text(
                message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
fun EmptyState(title: String, text: String) {
    Panel {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun colorSeveridad(severity: String): Color = when (severity) {
    "roja" -> Color(0xFFDC2626)
    "naranja" -> Color(0xFFEA580C)
    "amarilla" -> Amber
    else -> Teal
}

fun colorPorNivel(nivel: Int): Color = when {
    nivel >= 4 -> Color(0xFFFF4D3D)
    nivel == 3 -> Color(0xFFFF7A3D)
    nivel == 2 -> Amber
    nivel == 1 -> Color(0xFF4BE3D8)
    else -> Color(0xFF8495AB)
}

fun etiquetaSeveridad(severity: String): String = when (severity) {
    "roja" -> "Muy alta"
    "naranja" -> "Alta"
    "amarilla" -> "Moderada"
    else -> "Informativa"
}

fun fuente(source: String): String = alertSourceLabel(source)

fun tiempoRelativo(valor: String?): String {
    if (valor.isNullOrBlank()) return ""
    val instante = parsearInstante(valor) ?: return valor
    val minutos = Duration.between(instante, Instant.now()).toMinutes()
    return when {
        minutos < 1 -> "Ahora"
        minutos < 60 -> "Hace $minutos min"
        minutos < 1440 -> "Hace ${minutos / 60} h"
        else -> "Hace ${minutos / 1440} d"
    }
}

fun formatearFechaHora(valor: String): String {
    val instante = parsearInstante(valor) ?: return valor
    val formato = DateTimeFormatter.ofPattern("dd MMM · HH:mm", Locale("es", "CO"))
    return instante.atZone(ZoneId.systemDefault()).format(formato)
}

private fun parsearInstante(valor: String): Instant? {
    return try {
        OffsetDateTime.parse(valor).toInstant()
    } catch (_: Exception) {
        try {
            Instant.parse(valor)
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(valor.replace(' ', 'T'))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            } catch (_: Exception) {
                null
            }
        }
    }
}
