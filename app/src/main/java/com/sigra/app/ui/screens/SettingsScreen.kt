package com.sigra.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigra.app.BuildConfig
import com.sigra.app.data.AppAppearance
import com.sigra.app.ui.components.Panel
import com.sigra.app.ui.components.ScreenHeader
import com.sigra.app.ui.components.SyncNotice
import com.sigra.app.ui.components.SectionTitle
import com.sigra.app.ui.components.rememberMonitoringAction
import com.sigra.app.ui.components.rememberNotificationAction
import com.sigra.app.ui.components.rememberPermissionStatus
import com.sigra.app.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val connectionStatus by vm.connectionStatus.collectAsStateWithLifecycle()
    val serverHealth by vm.serverHealth.collectAsStateWithLifecycle()
    val eventsStatus by vm.eventsStatus.collectAsStateWithLifecycle()
    val statsStatus by vm.statsStatus.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.checkConnection() }
    val changingMonitoring by vm.changingMonitoring.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissions = rememberPermissionStatus()
    val monitoringAction = rememberMonitoringAction(vm)
    val notificationAction = rememberNotificationAction(vm)
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var licensesOpen by rememberSaveable { mutableStateOf(false) }
    var diagnostics by rememberSaveable { mutableStateOf(false) }
    var radius by remember(settings.alertRadiusKm) { mutableFloatStateOf(settings.alertRadiusKm.toFloat().coerceIn(25f, 1000f)) }
    var magnitude by remember(settings.minMagnitude) { mutableFloatStateOf(settings.minMagnitude.coerceIn(1.5f, 8f)) }
    fun openSettings(intent: Intent) {
        runCatching { context.startActivity(intent) }.onFailure { vm.reportError("No se pudo abrir esta opción del sistema.") }
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenHeader("Ajustes", "Apariencia, alertas y participación.") }
        item {
            Panel {
                SectionTitle("Conexión con SIGRA", "Servidor de eventos y red ciudadana")
                Spacer(Modifier.height(10.dp))
                Text(BuildConfig.SIGRA_BASE_URL, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                if (connectionStatus.updatedAt != null && connectionStatus.error == null) {
                    Text("Servidor disponible · SIGRA ${serverHealth?.version} · API ${serverHealth?.api_version}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                SyncNotice(connectionStatus, vm::checkConnection)
                Spacer(Modifier.height(8.dp))
                Text("Catálogo: " + when {
                    eventsStatus.error != null -> eventsStatus.error
                    eventsStatus.updatedAt != null -> "eventos recibidos correctamente"
                    eventsStatus.loading -> "consultando…"
                    else -> "pendiente de consulta"
                }, style = MaterialTheme.typography.bodySmall)
                Text("Red ciudadana: " + when {
                    statsStatus.error != null -> statsStatus.error
                    statsStatus.updatedAt != null -> "estadísticas recibidas correctamente"
                    statsStatus.loading -> "consultando…"
                    else -> "pendiente de consulta"
                }, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { vm.checkConnection(); vm.refresh() },
                    enabled = !connectionStatus.loading, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Comprobar conexión")
                }
            }
        }
        item {
            Panel {
                SectionTitle("Apariencia", "Elige cómo quieres ver SIGRA")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppAppearance.entries.forEach { appearance ->
                        FilterChip(selected = settings.appearance == appearance,
                            onClick = { vm.setAppearance(appearance) }, label = { Text(appearance.label) })
                    }
                }
                Text("Automático sigue el tema de tu teléfono.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Panel {
                SectionTitle("Tu participación")
                Spacer(Modifier.height(8.dp))
                SettingSwitch("Monitoreo sísmico", "Usa el acelerómetro con una notificación permanente. Requiere ubicación y consume batería.",
                    settings.monitoringEnabled, enabled = !changingMonitoring, onChange = monitoringAction)
                settings.monitoringIssue?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                SettingSwitch("Notificaciones de alertas", "Avisos de eventos cercanos según tu radio y magnitud mínima.",
                    settings.notificationsEnabled && permissions.notifications, onChange = notificationAction)
                if (settings.notificationsEnabled && !permissions.notifications) {
                    Text("Las notificaciones están bloqueadas por el sistema. Actívalas en Permisos.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            Panel {
                SectionTitle("Personaliza las alertas", "Estos valores no modifican los filtros del mapa")
                Spacer(Modifier.height(18.dp))
                SettingValue("Radio de alerta", "${radius.roundToInt()} km")
                Slider(value = radius, onValueChange = { radius = it }, valueRange = 25f..1000f, steps = 38,
                    onValueChangeFinished = { vm.setAlertRadius(radius.roundToInt()) })
                Text("Distancia máxima entre tu ubicación y el epicentro para recibir una notificación.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))
                SettingValue("Magnitud mínima", "M ${"%.1f".format(magnitude)}")
                Slider(value = magnitude, onValueChange = { magnitude = it }, valueRange = 1.5f..8f, steps = 64,
                    onValueChangeFinished = { vm.setMinMagnitude(magnitude) })
                Text("Una magnitud mínima más baja puede producir más avisos.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Panel {
                SectionTitle("Permisos del teléfono")
                Spacer(Modifier.height(12.dp))
                SettingValue("Ubicación", if (permissions.location) "Permitida" else "Pendiente")
                SettingValue("Notificaciones", if (permissions.notifications) "Permitidas" else "Bloqueadas")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                }, modifier = Modifier.fillMaxWidth()) { Text("Administrar permisos") }
                OutlinedButton(onClick = { openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth()) { Text("Opciones de batería") }
                Text("Si el sistema detiene el monitoreo, revisa las restricciones de batería. Déjalo quieto y cargando cuando puedas.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Panel {
                SectionTitle("Participación y privacidad")
                Spacer(Modifier.height(10.dp))
                Text("Al monitorear se envían ubicación, estado del teléfono y posibles detecciones al servidor SIGRA. El panel público muestra posiciones aproximadas.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Puedes detener el monitoreo sin borrar tu registro, o abandonar la red para eliminar la identificación guardada en este teléfono.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { confirmLeave = true }, enabled = !changingMonitoring,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Abandonar la red")
                }
            }
        }
        item {
            Panel {
                SectionTitle("Acerca de SIGRA", "v${BuildConfig.VERSION_NAME.substringBefore("-")}")
                Spacer(Modifier.height(8.dp))
                Text("Red ciudadana experimental para estudiar movimiento sísmico. Catálogo oficial: SGC, USGS, EMSC, GeoNet, GEOFON e INGV.",
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("SIGRA no garantiza avisos anticipados ni sustituye al SGC, la UNGRD o las autoridades locales.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { openSettings(Intent(Intent.ACTION_VIEW, Uri.parse("https://sigra.azyrondev.com/"))) }) {
                    Text("Abrir SIGRA web")
                }
                TextButton(onClick = { licensesOpen = true }) { Text("Licencias de tipografías") }
                TextButton(onClick = { diagnostics = !diagnostics }) {
                    Text(if (diagnostics) "Ocultar información técnica" else "Información técnica")
                }
                AnimatedVisibility(diagnostics) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider()
                        Text("Contrato: API v1 · rutas /api/sismos y /api/red", style = MaterialTheme.typography.bodySmall)
                        Text("Clave de catálogo: ${if (BuildConfig.SIGRA_EVENTS_API_KEY.isBlank()) "No configurada" else "Configurada"}", style = MaterialTheme.typography.bodySmall)
                        Text("Clave de red: ${if (BuildConfig.SIGRA_API_KEY.isBlank()) "No configurada" else "Configurada"}", style = MaterialTheme.typography.bodySmall)
                        Text("Dispositivo: ${settings.deviceUuid}", style = MaterialTheme.typography.bodySmall)
                        Text("Registro: ${if (settings.deviceToken != null) "Registrado" else "Sin registrar"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if (licensesOpen) {
        val licenseText = remember {
            listOf("chakra-petch", "ibm-plex-sans", "ibm-plex-mono").joinToString("\n\n") { name ->
                context.assets.open("licenses/$name-OFL.txt").bufferedReader().use { it.readText() }
            }
        }
        AlertDialog(onDismissRequest = { licensesOpen = false },
            title = { Text("Tipografías · SIL OFL 1.1") },
            text = { Text(licenseText, Modifier.verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall) },
            confirmButton = { TextButton(onClick = { licensesOpen = false }) { Text("Cerrar") } })
    }
    if (confirmLeave) {
        AlertDialog(onDismissRequest = { confirmLeave = false },
            title = { Text("¿Abandonar la red?") },
            text = { Text("Se detendrá el monitoreo y se borrará el registro local. El historial y tus preferencias se conservarán; puedes vaciar el historial desde Alertas. Esta acción no elimina datos ya enviados al servidor.") },
            confirmButton = { TextButton(onClick = { confirmLeave = false; vm.abandonarRed() }) { Text("Abandonar") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Cancelar") } })
    }
}

@Composable
private fun SettingSwitch(title: String, detail: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun SettingValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
