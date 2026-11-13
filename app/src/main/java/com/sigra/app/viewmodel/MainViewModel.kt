package com.sigra.app.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sigra.app.data.HealthResponse
import com.sigra.app.data.ApiClient
import com.sigra.app.data.AppAppearance
import com.sigra.app.data.AlertChannel
import com.sigra.app.data.isValidForInbox
import com.sigra.app.data.SyncStatus
import com.sigra.app.data.QuakeFilters
import com.sigra.app.data.QuakeOrder
import com.sigra.app.location.LocationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.sigra.app.data.PublicStatsResponse
import com.sigra.app.data.SeismicAlert
import com.sigra.app.data.SettingsStore
import com.sigra.app.data.SismoEvent
import com.sigra.app.data.UserSettings
import com.sigra.app.service.SeismicMonitorService
import com.sigra.app.service.AlertReceiver
import com.sigra.app.service.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.io.IOException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application)

    val settings: StateFlow<UserSettings> = store.settings.catch {
        if (it is IOException) emit(UserSettings()) else throw it
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        UserSettings(),
    )

    private val _connectionStatus = MutableStateFlow(SyncStatus())
    val connectionStatus = _connectionStatus.asStateFlow()
    private val _serverHealth = MutableStateFlow<HealthResponse?>(null)
    val serverHealth = _serverHealth.asStateFlow()

    fun checkConnection() {
        if (_connectionStatus.value.loading) return
        _connectionStatus.value = SyncStatus(loading = true)
        viewModelScope.launch {
            loadFeed(_connectionStatus) {
                val response = ApiClient.api.health()
                if (!response.isSuccessful) throw HttpException(response)
                val health = response.body() ?: error("El servidor devolvió una respuesta vacía.")
                check(health.service == "sigra" && health.api_version == "v1") { "El servidor no ofrece el contrato SIGRA v1 esperado." }
                check(health.status == "ok" && health.database == "ok") { "SIGRA está disponible, pero su base de datos no está operativa." }
                _serverHealth.value = health
            }
        }
    }

    private val _stats = MutableStateFlow<PublicStatsResponse?>(null)
    val stats: StateFlow<PublicStatsResponse?> = _stats.asStateFlow()

    private val _sismos = MutableStateFlow<List<SismoEvent>>(emptyList())
    val sismos: StateFlow<List<SismoEvent>> = _sismos.asStateFlow()

    private val _alertas = MutableStateFlow<List<SeismicAlert>>(emptyList())
    val alertas: StateFlow<List<SeismicAlert>> = _alertas.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var refreshJob: Job? = null
    private var refreshGeneration = 0
    private var initialized = false
    private var saveFiltersJob: Job? = null
    private val _quakeFilters = MutableStateFlow(QuakeFilters())
    val quakeFilters: StateFlow<QuakeFilters> = _quakeFilters.asStateFlow()

    private val _eventsStatus = MutableStateFlow(SyncStatus())
    val eventsStatus = _eventsStatus.asStateFlow()
    private val _alertsStatus = MutableStateFlow(SyncStatus())
    val alertsStatus = _alertsStatus.asStateFlow()
    private val _statsStatus = MutableStateFlow(SyncStatus())
    val statsStatus = _statsStatus.asStateFlow()
    private val _changingMonitoring = MutableStateFlow(false)
    val changingMonitoring = _changingMonitoring.asStateFlow()
    private val _locating = MutableStateFlow(false)
    val locating = _locating.asStateFlow()
    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()
    private val _startupError = MutableStateFlow<String?>(null)
    val startupError = _startupError.asStateFlow()
    private val _historyError = MutableStateFlow<String?>(null)
    val historyError = _historyError.asStateFlow()
    private var startupJob: Job? = null

    suspend fun awaitRefresh() {
        refresh()
        refreshJob?.join()
    }

    fun reportError(message: String) { _error.value = message }

    private fun ownLocation(settings: UserSettings): Pair<Double, Double>? {
        val lat = settings.lastLat ?: return null
        val lng = settings.lastLng ?: return null
        return if (lat.isFinite() && lng.isFinite() && lat in -90.0..90.0 && lng in -180.0..180.0) lat to lng else null
    }

    fun setMapRadius(km: Int) {
        if (km !in QuakeFilters.radii) return
        if (km > 0 && ownLocation(settings.value) == null) {
            _error.value = "Activa Mi ubicación antes de filtrar por distancia."
            return
        }
        updateQuakeFilters(_quakeFilters.value.copy(radiusKm = km))
    }

    fun setMapSource(source: String?) {
        if (source != null && source !in QuakeFilters.sources) return
        updateQuakeFilters(_quakeFilters.value.copy(source = source))
    }

    fun setMapHours(hours: Int) {
        if (hours in QuakeFilters.periods) updateQuakeFilters(_quakeFilters.value.copy(hours = hours))
    }

    fun setMapMinMagnitude(magnitude: Double?) {
        if (magnitude in QuakeFilters.magnitudes) updateQuakeFilters(_quakeFilters.value.copy(minMagnitude = magnitude))
    }

    fun setMapOrder(order: QuakeOrder) {
        if (order == QuakeOrder.DISTANCE && ownLocation(settings.value) == null) {
            _error.value = "Activa Mi ubicación antes de ordenar por cercanía."
            return
        }
        updateQuakeFilters(_quakeFilters.value.copy(order = order))
    }

    fun resetQuakeFilters() { updateQuakeFilters(QuakeFilters()) }

    private fun updateQuakeFilters(filters: QuakeFilters) {
        if (!initialized || filters == _quakeFilters.value) return
        _quakeFilters.value = filters
        _sismos.value = emptyList()
        _eventsStatus.value = SyncStatus()
        saveFiltersJob?.cancel()
        saveFiltersJob = viewModelScope.launch {
            try {
                store.setQuakeFilters(filters)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                _error.value = "No pudimos guardar los filtros. Vuelve a seleccionarlos al abrir la app."
            }
        }
        refresh(restart = true)
    }

    fun updateLocation(onLocated: ((Double, Double) -> Unit)? = null) {
        if (_locating.value) return
        _locating.value = true
        viewModelScope.launch {
            try {
                val location = LocationHelper.getLocation(getApplication())
                if (location == null) {
                    _error.value = "No pudimos obtener tu ubicación. Revisa los permisos y el GPS."
                    return@launch
                }
                store.saveLocation(location.latitude, location.longitude)
                onLocated?.invoke(location.latitude, location.longitude)
                refresh(restart = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _error.value = mensajeError(exception)
            } finally {
                _locating.value = false
            }
        }
    }

    init {
        initialize()
    }

    fun initialize() {
        if (startupJob?.isActive == true) return
        _ready.value = false
        _startupError.value = null
        startupJob = viewModelScope.launch {
            try {
                store.ensureDeviceUuid()
                val actual = store.snapshot()
                _quakeFilters.value = actual.quakeFilters.forLocation(ownLocation(actual) != null)
                initialized = true
                _ready.value = true
                refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                initialized = false
                _ready.value = true
                _startupError.value = "No pudimos abrir la configuración local. Intenta de nuevo."
            }
        }
    }

    fun refresh(restart: Boolean = false) {
        if (!initialized || (_cargando.value && !restart)) return
        val generation = ++refreshGeneration
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _cargando.value = true
            _eventsStatus.value = _eventsStatus.value.copy(loading = true, error = null)
            _alertsStatus.value = _alertsStatus.value.copy(loading = true, error = null)
            _statsStatus.value = _statsStatus.value.copy(loading = true, error = null)
            try {
                val actual = store.snapshot()
                val origin = ownLocation(actual)
                val filters = _quakeFilters.value.forLocation(origin != null)
                // Each feed owns its error and timestamp. A failed stats request must not hide earthquakes.
                coroutineScope {
                    launch {
                        loadFeed(_statsStatus) {
                            val response = ApiClient.api.red()
                            check(response.status == "ok") { "SIGRA no confirmó la consulta de la red." }
                            _stats.value = response
                        }
                    }
                    launch {
                        if (origin == null) {
                            _alertas.value = emptyList()
                            _alertsStatus.value = SyncStatus()
                        } else {
                            loadFeed(_alertsStatus) {
                                val response = ApiClient.api.alertas(
                                    ApiClient.appKey, actual.deviceToken, origin.first, origin.second,
                                )
                                check(response.status == "ok") { "SIGRA no confirmó la consulta de alertas." }
                                _alertas.value = response.alerts
                                try {
                                    AlertReceiver.receiveAll(getApplication(), store, _alertas.value, AlertChannel.FOREGROUND, actual.deviceUuid)
                                    _historyError.value = null
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    _historyError.value = "Los avisos se recibieron, pero no pudimos guardarlos en el historial."
                                }
                            }
                        }
                    }
                    launch {
                        loadFeed(_eventsStatus) {
                            val response = ApiClient.api.sismos(
                                ApiClient.bearer, hours = filters.hours, minMagnitude = filters.minMagnitude, limit = 1000,
                                lat = origin?.first, lng = origin?.second,
                                radiusKm = filters.radiusKm.takeIf { it > 0 },
                                source = filters.source, order = filters.order.apiValue,
                            )
                            check(response.status == "ok") { "SIGRA no confirmó la consulta del catálogo." }
                            _sismos.value = response.events.distinctBy { it.fuente to it.id }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _eventsStatus.value = _eventsStatus.value.copy(error = mensajeError(exception))
            } finally {
                if (generation == refreshGeneration) {
                    _cargando.value = false
                    _eventsStatus.value = _eventsStatus.value.copy(loading = false)
                    _alertsStatus.value = _alertsStatus.value.copy(loading = false)
                    _statsStatus.value = _statsStatus.value.copy(loading = false)
                }
            }
        }
    }

    private suspend fun loadFeed(status: MutableStateFlow<SyncStatus>, block: suspend () -> Unit) {
        try {
            block()
            status.value = SyncStatus(updatedAt = System.currentTimeMillis())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            status.value = status.value.copy(loading = false, error = mensajeError(exception))
        }
    }

    fun completarOnboarding(activateMonitoring: Boolean = true) {
        saveSettings {
            store.setOnboardingCompleted(true)
            if (activateMonitoring) setMonitoring(true)
            refresh(restart = true)
        }
    }

    fun setMonitoring(activo: Boolean) {
        if (_changingMonitoring.value) return
        _changingMonitoring.value = true
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                if (activo && !LocationHelper.hasPermission(app)) {
                    _error.value = "Concede permiso de ubicación para activar el monitoreo."
                    return@launch
                }
                val sensors = app.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                if (activo && sensors?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) == null &&
                    sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
                    _error.value = "Este teléfono no tiene un acelerómetro disponible. Puedes seguir consultando sismos y alertas."
                    return@launch
                }
                store.setMonitoring(activo)
                if (activo) {
                    SeismicMonitorService.start(app)
                } else {
                    SeismicMonitorService.stop(app)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                runCatching { store.setMonitoring(false) }
                _error.value = "No se pudo iniciar el monitoreo. ${mensajeError(exception)}"
            } finally {
                _changingMonitoring.value = false
            }
        }
    }

    fun setAppearance(appearance: AppAppearance) {
        saveSettings { store.setAppearance(appearance) }
    }

    fun setNotifications(activo: Boolean) {
        saveSettings { store.setNotifications(activo) }
    }

    fun setAlertRadius(km: Int) {
        saveSettings { store.setAlertRadius(km) }
    }

    fun setMinMagnitude(magnitud: Float) {
        saveSettings { store.setMinMagnitude(magnitud) }
    }

    fun markAlertRead(uuid: String) {
        saveSettings {
            store.markAlertRead(uuid)
            NotificationHelper.cancelAlert(getApplication(), uuid)
        }
    }

    fun openAlert(alert: SeismicAlert) {
        saveSettings {
            store.receiveAlert(alert, AlertChannel.OPENED)
            store.markAlertRead(alert.uuid)
        }
    }

    fun markAllAlertsRead() {
        saveSettings {
            val ids = store.snapshot().alertInbox.entries.map { it.alert.uuid }
            store.markAllAlertsRead()
            ids.forEach { NotificationHelper.cancelAlert(getApplication(), it) }
        }
    }

    fun clearAlertHistory() {
        saveSettings {
            val ids = store.snapshot().alertInbox.entries.map { it.alert.uuid }
            store.clearAlertHistory()
            ids.forEach { NotificationHelper.cancelAlert(getApplication(), it) }
            _historyError.value = null
        }
    }

    private fun saveSettings(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _error.value = "No pudimos guardar el cambio. Revisa el almacenamiento del teléfono e inténtalo de nuevo."
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun abandonarRed() {
        if (_changingMonitoring.value) return
        _changingMonitoring.value = true
        viewModelScope.launch {
            try {
                val token = store.snapshot().deviceToken
                SeismicMonitorService.stop(getApplication())
                store.clearRegistration()
                _alertas.value = emptyList()
                refresh(restart = true)
                if (token != null) {
                    try {
                        ApiClient.api.desactivar(ApiClient.appKey, token)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        _error.value = "El monitoreo se detuvo y el registro local se borró. No pudimos confirmar la baja en el servidor."
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _error.value = mensajeError(exception)
            } finally {
                _changingMonitoring.value = false
            }
        }
    }

    fun parsearAlerta(json: String?): SeismicAlert? {
        if (json.isNullOrBlank()) return null

        return try {
            val alert = ApiClient.json.decodeFromString<SeismicAlert>(json)
            if (!alert.isValidForInbox()) return null
            // Old cached payloads have no reliable reference for their relative ETA.
            if ("_received_at_ms" in Json.parseToJsonElement(json).jsonObject) alert
            else alert.copy(seconds_to_s_wave = null)
        } catch (_: Exception) {
            null
        }
    }

    private fun mensajeError(exception: Throwable): String = com.sigra.app.data.apiErrorMessage(exception)
}
