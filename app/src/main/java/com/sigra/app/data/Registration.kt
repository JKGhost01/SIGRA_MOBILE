package com.sigra.app.data

import android.content.Context
import com.sigra.app.location.LocationHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException

object Registration {
    private val mutex = Mutex()
    /**
     * Registra el teléfono si aún no tiene token. Devuelve el estado
     * actualizado de la configuración local.
     */
    suspend fun ensure(context: Context, store: SettingsStore, force: Boolean = false): UserSettings = mutex.withLock {
        if (store.snapshot().deviceUuid.isBlank()) {
            store.ensureDeviceUuid()
        }

        var actual = store.snapshot()
        if (!actual.monitoringEnabled || (!force && actual.deviceToken != null)) {
            return@withLock actual
        }

        val ubicacion = LocationHelper.getLocation(context)
        if (ubicacion != null) {
            store.saveLocation(ubicacion.latitude, ubicacion.longitude)
        }

        actual = store.snapshot()
        if (!actual.monitoringEnabled) return@withLock actual
        val respuesta = ApiClient.api.registro(
            ApiClient.appKey,
            RegistrationRequest(
                device_uuid = actual.deviceUuid,
                manufacturer = DeviceInfo.manufacturer(),
                model = DeviceInfo.model(),
                os_version = DeviceInfo.osVersion(),
                app_version = DeviceInfo.appVersion(),
                fcm_token = actual.fcmToken,
                latitude = ubicacion?.latitude ?: actual.lastLat,
                longitude = ubicacion?.longitude ?: actual.lastLng,
                accuracy_m = ubicacion?.accuracy?.toDouble(),
                monitoring_enabled = actual.monitoringEnabled,
                notifications_enabled = actual.notificationsEnabled,
                alert_radius_km = actual.alertRadiusKm,
                min_magnitude = actual.minMagnitude.toDouble(),
            ),
            currentToken = actual.deviceToken,
        )

        if (respuesta.status != "ok" || respuesta.device_token.isBlank()) {
            throw IllegalStateException("El servidor no entregó un token de dispositivo.")
        }

        if (!store.saveRegistration(respuesta.device_token, actual.deviceUuid)) {
            try {
                ApiClient.api.desactivar(ApiClient.appKey, respuesta.device_token)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The local identity has already changed; never restore an obsolete token.
            }
        }

        store.snapshot()
    }
}
