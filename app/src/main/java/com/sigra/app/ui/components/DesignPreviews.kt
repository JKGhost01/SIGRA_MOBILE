package com.sigra.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sigra.app.data.QuakeFilters
import com.sigra.app.data.SismoEvent
import com.sigra.app.data.SyncStatus
import com.sigra.app.data.SeismicAlert
import com.sigra.app.ui.theme.SigraTheme

@Preview(name = "Claro", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Oscuro", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Texto grande", showBackground = true, widthDp = 360, heightDp = 1000, fontScale = 1.5f)
@Composable
private fun SigraComponentsPreview() {
    SigraTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ScreenHeader("Pulso sísmico", "Información para estar preparado.") { RefreshAction(false) {} }
                QuakeFiltersPanel(QuakeFilters(), hasLocation = false, onLocation = {}, onRadius = {}, onSource = {}, onOrder = {})
                SyncNotice(SyncStatus(error = "Sin conexión con el servidor SIGRA. Revisa tu red.")) {}
                SectionTitle("Actividad oficial", "Vista de diseño · datos de ejemplo")
                SismoRow(SismoEvent(id = "preview", ubicacion = "Los Santos, Santander, Colombia",
                    magnitud = 3.4, profundidad = 145.0, fecha_iso = "2026-11-12T10:00:00-05:00")) {}
                SectionTitle("Historial de avisos", "Vista de diseño · datos de ejemplo")
                AlertCard(SeismicAlert(uuid = "preview-alert", source = "simulation", magnitude = 4.0,
                    status = "expired", receivedAtMs = 1794495600000), unread = true, receivedAtMs = 1794495600000) {}
            }
        }
    }
}

@Preview(name = "Inicio · oscuro", showBackground = true, widthDp = 360, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Inicio · 320 dp", showBackground = true, widthDp = 320, heightDp = 900)
@Preview(name = "Inicio · texto doble", showBackground = true, widthDp = 360, heightDp = 1100, fontScale = 2f)
@Preview(name = "Inicio · ancho", showBackground = true, widthDp = 720, heightDp = 800)
@Composable
private fun HomeLayoutPreview() {
    SigraTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ScreenHeader("SIGRA", "OBSERVATORIO SÍSMICO") { RefreshAction(false) {} }
                com.sigra.app.ui.screens.FeaturedQuake(SismoEvent(id = "preview", fuente = "EMSC",
                    ubicacion = "Los Santos, Santander, Colombia", magnitud = 4.3,
                    fecha_iso = "2026-11-13T08:30:00-05:00"), 284) {}
                com.sigra.app.ui.screens.HomeShortcuts({}, {})
                com.sigra.app.ui.screens.MonitorCard(false, false, null) {}
            }
        }
    }
}

@Preview(name = "Controles mapa · 320 dp", showBackground = true, widthDp = 320)
@Preview(name = "Controles mapa · texto doble", showBackground = true, widthDp = 320, fontScale = 2f)
@Preview(name = "Controles mapa · oscuro", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MapControlsPreview() {
    SigraTheme {
        MapControls(locating = false, hasEvents = true,
            onZoomOut = {}, onZoomIn = {}, onLocation = {}, onFitEvents = {})
    }
}
