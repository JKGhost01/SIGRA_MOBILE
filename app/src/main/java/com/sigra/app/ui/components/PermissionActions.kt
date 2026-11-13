package com.sigra.app.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sigra.app.location.LocationHelper
import com.sigra.app.viewmodel.MainViewModel

data class PermissionStatus(val location: Boolean, val notifications: Boolean)

@Composable
fun rememberLocationAction(vm: MainViewModel, onLocated: (Double, Double) -> Unit = { _, _ -> }): () -> Unit {
    val context = LocalContext.current
    val currentOnLocated by rememberUpdatedState(onLocated)
    val requestLocation = {
        vm.updateLocation { lat, lng -> currentOnLocated(lat, lng) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (LocationHelper.hasPermission(context)) requestLocation()
        else vm.reportError("Permite la ubicación desde Ajustes para centrar el mapa y calcular distancias.")
    }
    return {
        if (LocationHelper.hasPermission(context)) requestLocation()
        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}

@Composable
fun rememberPermissionStatus(): PermissionStatus {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) revision++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return remember(context, revision) {
        PermissionStatus(LocationHelper.hasPermission(context), NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
}

@Composable
fun rememberMonitoringAction(vm: MainViewModel): (Boolean) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (LocationHelper.hasPermission(context)) vm.setMonitoring(true)
        else vm.reportError("Para monitorear, permite la ubicación desde los ajustes de la app. Puedes seguir explorando sismos.")
    }
    return { enabled ->
        if (!enabled || LocationHelper.hasPermission(context)) {
            vm.setMonitoring(enabled)
        } else {
            launcher.launch(buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            }.toTypedArray())
        }
    }
}

@Composable
fun rememberNotificationAction(vm: MainViewModel): (Boolean) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setNotifications(granted)
        if (!granted) vm.reportError("Las notificaciones están bloqueadas. Puedes habilitarlas desde Permisos en Ajustes.")
    }
    return { enabled ->
        when {
            !enabled -> vm.setNotifications(false)
            NotificationManagerCompat.from(context).areNotificationsEnabled() -> vm.setNotifications(true)
            Build.VERSION.SDK_INT >= 33 -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> vm.reportError("Activa las notificaciones de SIGRA desde los ajustes del sistema.")
        }
    }
}
