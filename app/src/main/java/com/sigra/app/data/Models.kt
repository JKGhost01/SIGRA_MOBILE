package com.sigra.app.data

import kotlinx.serialization.Serializable

@Serializable
data class RedConfig(
    val min_devices: Int = 3,
    val cluster_window_seconds: Int = 25,
    val min_alert_magnitude: Double = 3.0,
    val min_alert_confidence: Double = 0.45,
    val p_wave_velocity_km_s: Double = 6.0,
    val s_wave_velocity_km_s: Double = 3.5,
    val poll_seconds: Int = 5,
    val heartbeat_seconds: Int = 30,
    val fcm_enabled: Boolean = false,
    val disclaimer: String = "Estimación experimental. No sustituye al SGC.",
)

@Serializable
data class DeviceDto(
    val id: Long = 0,
    val device_uuid: String = "",
    val platform: String = "android",
    val model: String? = null,
    val monitoring_enabled: Boolean = false,
    val notifications_enabled: Boolean = true,
    val alert_radius_km: Int = 250,
    val min_magnitude: Double = 3.0,
    val last_seen_at: String? = null,
)

@Serializable
data class RegistrationRequest(
    val device_uuid: String,
    val platform: String = "android",
    val manufacturer: String? = null,
    val model: String? = null,
    val os_version: String? = null,
    val app_version: String? = null,
    val fcm_token: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy_m: Double? = null,
    val monitoring_enabled: Boolean = false,
    val notifications_enabled: Boolean = true,
    val alert_radius_km: Int = 250,
    val min_magnitude: Double = 3.0,
)

@Serializable
data class RegistrationResponse(
    val status: String = "",
    val device_token: String = "",
    val device: DeviceDto? = null,
    val config: RedConfig? = null,
)

@Serializable
data class HeartbeatRequest(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy_m: Double? = null,
    val monitoring_enabled: Boolean = true,
    val notifications_enabled: Boolean = true,
    val fcm_token: String? = null,
    val app_version: String? = null,
    val battery_level: Int? = null,
    val alert_radius_km: Int = 250,
    val min_magnitude: Double = 3.0,
)

@Serializable
data class HeartbeatResponse(
    val status: String = "",
    val server_time_ms: Long = 0,
    val device: DeviceDto? = null,
    val config: RedConfig? = null,
    val active_alerts: Int = 0,
)

@Serializable
data class DetectionRequest(
    val detected_at_ms: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy_m: Double? = null,
    val sta_lta_ratio: Double,
    val peak_accel_mps2: Double,
    val dominant_freq_hz: Double,
    val duration_ms: Int,
    val confidence: Double,
)

@Serializable
data class SeismicAlert(
    val uuid: String = "",
    val source: String = "crowd",
    val status: String = "active",
    val magnitude: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val depth_km: Double = 10.0,
    val radius_km: Double = 0.0,
    val confidence: Double = 0.0,
    val detection_count: Int = 0,
    val device_count: Int = 0,
    val origin_time_ms: Long = 0,
    val origin_time: String? = null,
    val created_at: String? = null,
    val expires_at: String? = null,
    val distance_km: Double? = null,
    val seconds_to_s_wave: Double? = null,
    val severity: String = "informativa",
    val estimated: Boolean = true,
    val disclaimer: String? = null,
    // Preserve the reference when saving an alert locally; reopening must not restart its ETA.
    @kotlinx.serialization.SerialName("_received_at_ms")
    val receivedAtMs: Long = System.currentTimeMillis(),
)

@Serializable
data class DetectionResponse(
    val status: String = "",
    val accepted: Boolean = false,
    val detection_id: Long? = null,
    val alert: SeismicAlert? = null,
    val message: String? = null,
)

@Serializable
data class AlertsResponse(
    val status: String = "",
    val server_time_ms: Long = 0,
    val alerts: List<SeismicAlert> = emptyList(),
)

@Serializable
data class SismoEvent(
    val id: String = "",
    val source_id: String? = null,
    val fuente: String = "SGC",
    val citizen_reports_count: Int = 0,
    val ubicacion: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val magnitud: Double = 0.0,
    val profundidad: Double = 0.0,
    val fecha: String = "",
    val fecha_iso: String? = null,
    val distancia_km: Double? = null,
    val alerta: String = "informativa",
    val nivel: Int = 0,
    val cdi: Double? = null,
    val mmi: Double? = null,
    val pga: Double? = null,
    val mag_type: String? = null,
    val agency: String? = null,
    val felt: Int? = null,
    val closer_towns: kotlinx.serialization.json.JsonElement? = null,
    val detalle_url: String? = null,
    val official_url: String? = null,
    val mapa_sgc_url: String? = null,
)

@Serializable
data class SismosResponse(
    val status: String = "",
    val count: Int = 0,
    val events: List<SismoEvent> = emptyList(),
)

@Serializable
data class StatsDto(
    val devices_total: Int = 0,
    val devices_online: Int = 0,
    val devices_monitoring: Int = 0,
    val detections_24h: Int = 0,
    val alerts_24h: Int = 0,
)

@Serializable
data class DevicePoint(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val monitoring: Boolean = false,
)

@Serializable
data class DetectionPoint(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val confidence: Double = 0.0,
)

@Serializable
data class PublicStatsResponse(
    val status: String = "",
    val generated_at: String = "",
    val operational: Boolean = false,
    val stats: StatsDto = StatsDto(),
    val last_alert: SeismicAlert? = null,
    val recent_alerts: List<SeismicAlert> = emptyList(),
    val devices: List<DevicePoint> = emptyList(),
    val detections: List<DetectionPoint> = emptyList(),
    val config: RedConfig? = null,
)

@Serializable
data class HealthResponse(
    val status: String = "",
    val service: String = "",
    val version: String = "",
    val api_version: String = "",
    val database: String = "",
)
