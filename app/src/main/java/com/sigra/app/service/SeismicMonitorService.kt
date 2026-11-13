package com.sigra.app.service

import android.app.Service
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.sigra.app.data.ApiClient
import com.sigra.app.data.AlertChannel
import com.sigra.app.data.DetectionRequest
import com.sigra.app.data.DeviceInfo
import com.sigra.app.data.HeartbeatRequest
import com.sigra.app.data.Registration
import com.sigra.app.data.SettingsStore
import com.sigra.app.location.LocationHelper
import com.sigra.app.sensor.PwaveDetection
import com.sigra.app.sensor.PwaveDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SeismicMonitorService : LifecycleService(), SensorEventListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var store: SettingsStore
    private lateinit var detector: PwaveDetector

    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null
    private var usaAceleracionLineal = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var ultimaUbicacion: Location? = null
    private var heartbeatJob: Job? = null
    private var pollJob: Job? = null
    private var enviandoDeteccion = false

    @Volatile
    private var activo = false

    override fun onCreate() {
        super.onCreate()
        store = SettingsStore(applicationContext)
        NotificationHelper.createChannels(this)
        detector = PwaveDetector { deteccion -> onPwave(deteccion) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            lifecycleScope.launch {
                try { store.setMonitoring(false) } catch (_: java.io.IOException) { /* Still stop the sensor. */ }
                finally { detenerServicio() }
            }
            return Service.START_NOT_STICKY
        }

        if (!activo) {
            activo = true
            try {
                if (!LocationHelper.hasPermission(this)) {
                    detenerConMotivo("El monitoreo se detuvo porque falta el permiso de ubicación. Actívalo desde Ajustes.")
                    return Service.START_NOT_STICKY
                }
                iniciarPrimerPlano()
                lifecycleScope.launch {
                    try {
                        if (!store.snapshot().monitoringEnabled) {
                            detenerServicio()
                            return@launch
                        }
                        if (!iniciarSensor()) {
                            detenerConMotivo("No se pudo iniciar el acelerómetro. Puedes seguir explorando sismos.")
                            return@launch
                        }
                        adquirirWakeLock()
                        iniciarLatidos()
                        iniciarPolling()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        detenerConMotivo("El monitoreo no pudo iniciarse. Revisa los permisos y vuelve a activarlo.")
                    }
                }
            } catch (_: RuntimeException) {
                detenerConMotivo("Android impidió iniciar el monitoreo. Abre SIGRA y vuelve a activarlo.")
                return Service.START_NOT_STICKY
            }
        }

        return Service.START_STICKY
    }

    override fun onBind(intent: Intent) = super.onBind(intent)

    override fun onDestroy() {
        detenerServicio()
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!activo || event.sensor.type != sensor?.type) return
        detector.process(
            event.values.getOrNull(0) ?: 0f,
            event.values.getOrNull(1) ?: 0f,
            event.values.getOrNull(2) ?: 0f,
            event.timestamp,
            usaAceleracionLineal,
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun iniciarPrimerPlano() {
        val notificacion = NotificationHelper.monitoringNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val tieneUbicacion = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

            startForeground(
                NotificationHelper.MONITORING_ID,
                notificacion,
                if (tieneUbicacion) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE,
            )
        } else {
            startForeground(NotificationHelper.MONITORING_ID, notificacion)
        }
    }

    private fun adquirirWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SIGRA::SeismicMonitor").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun iniciarSensor(): Boolean {
        val manager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
        sensorManager = manager
        val lineal = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        sensor = lineal ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        usaAceleracionLineal = lineal != null
        return sensor?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) } ?: false
    }

    private fun detenerConMotivo(message: String) {
        lifecycleScope.launch {
            try {
                store.stopMonitoringWithIssue(message)
            } catch (_: java.io.IOException) {
                // A storage failure must not leave the sensor or wake lock running.
            } finally {
                detenerServicio()
            }
        }
    }

    private fun detenerServicio() {
        activo = false
        heartbeatJob?.cancel()
        pollJob?.cancel()
        sensorManager?.unregisterListener(this)
        sensorManager = null
        detector.reset()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onPwave(deteccion: PwaveDetection) {
        if (!activo || enviandoDeteccion) return
        enviandoDeteccion = true
        lifecycleScope.launch {
            try {
                enviarDeteccion(deteccion)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                actualizarNotificacion("Detección no enviada. Revisa la conexión con SIGRA.")
            } finally {
                enviandoDeteccion = false
            }
        }
    }

    private suspend fun enviarDeteccion(deteccion: PwaveDetection) {
        var settings = store.snapshot()
        if (!settings.monitoringEnabled) return
        if (settings.deviceToken == null) {
            settings = Registration.ensure(this, store)
        }

        val ubicacion = obtenerUbicacion() ?: return
        val token = settings.deviceToken ?: return

        try {
            val respuesta = ApiClient.api.deteccion(
                ApiClient.appKey,
                token,
                DetectionRequest(
                    detected_at_ms = deteccion.timestampMs,
                    latitude = ubicacion.latitude,
                    longitude = ubicacion.longitude,
                    accuracy_m = ubicacion.accuracy.toDouble(),
                    sta_lta_ratio = deteccion.staLtaRatio,
                    peak_accel_mps2 = deteccion.peakAccelMps2,
                    dominant_freq_hz = deteccion.dominantFreqHz,
                    duration_ms = deteccion.durationMs,
                    confidence = deteccion.confidence,
                ),
            )

            val mensaje = when {
                respuesta.alert != null -> "Evento confirmado por la red. Alerta emitida."
                respuesta.accepted -> "Detección enviada. Esperando confirmación de otros teléfonos."
                else -> "Detección recibida por el servidor."
            }
            actualizarNotificacion(mensaje)

            respuesta.alert?.let { alerta ->
                AlertReceiver.receive(this, store, alerta, AlertChannel.DETECTION, settings.deviceUuid)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            actualizarNotificacion("No se pudo enviar la detección: ${exception.message ?: "sin conexión"}")
        }
    }

    private fun iniciarLatidos() {
        heartbeatJob = scope.launch {
            while (activo) {
                try {
                    var settings = store.snapshot()
                    if (!settings.monitoringEnabled) return@launch
                    if (settings.deviceToken == null) {
                        settings = Registration.ensure(this@SeismicMonitorService, store)
                    }

                    val ubicacion = obtenerUbicacion()
                    val token = settings.deviceToken
                    if (token != null) {
                        ApiClient.api.estado(
                            ApiClient.appKey,
                            token,
                            HeartbeatRequest(
                                latitude = ubicacion?.latitude ?: settings.lastLat,
                                longitude = ubicacion?.longitude ?: settings.lastLng,
                                accuracy_m = ubicacion?.accuracy?.toDouble(),
                                monitoring_enabled = true,
                                notifications_enabled = settings.notificationsEnabled,
                                fcm_token = settings.fcmToken,
                                app_version = DeviceInfo.appVersion(),
                                battery_level = DeviceInfo.batteryLevel(this@SeismicMonitorService),
                                alert_radius_km = settings.alertRadiusKm,
                                min_magnitude = settings.minMagnitude.toDouble(),
                            ),
                        )
                    }
                    delay(30_000)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    delay(15_000)
                }
            }
        }
    }

    private fun iniciarPolling() {
        pollJob = scope.launch {
            while (activo) {
                try {
                    val settings = store.snapshot()
                    if (!settings.monitoringEnabled) return@launch
                    val ubicacion = obtenerUbicacion()
                    val lat = ubicacion?.latitude ?: settings.lastLat
                    val lng = ubicacion?.longitude ?: settings.lastLng

                    if (lat != null && lng != null) {
                        val respuesta = ApiClient.api.alertas(
                            ApiClient.appKey,
                            settings.deviceToken,
                            lat,
                            lng,
                        )
                        AlertReceiver.receiveAll(this@SeismicMonitorService, store, respuesta.alerts, AlertChannel.POLLING, settings.deviceUuid)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Sin red: el siguiente ciclo reintenta.
                }
                delay(5_000)
            }
        }
    }

    private suspend fun obtenerUbicacion(): Location? {
        val ubicacion = LocationHelper.getLocation(this, 6_000)
        if (ubicacion != null) {
            ultimaUbicacion = ubicacion
            store.saveLocation(ubicacion.latitude, ubicacion.longitude)
            return ubicacion
        }

        return ultimaUbicacion?.takeIf(LocationHelper::isRecent)
    }

    private fun actualizarNotificacion(texto: String) {
        try {
            NotificationManagerCompat.from(this).notify(
                NotificationHelper.MONITORING_ID,
                NotificationHelper.monitoringNotification(this, texto),
            )
        } catch (_: SecurityException) {
            // Sin permiso de notificaciones; el servicio sigue activo.
        }
    }

    companion object {
        const val ACTION_START = "com.sigra.app.action.START_MONITOR"
        const val ACTION_STOP = "com.sigra.app.action.STOP_MONITOR"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SeismicMonitorService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SeismicMonitorService::class.java))
        }
    }
}
