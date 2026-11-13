package com.sigra.app.data

import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale

object PushAlertDecoder {
    // A transport message can outlive a useful seismic warning. This is a client freshness limit.
    const val MAX_AGE_MS = 5 * 60_000L

    fun decode(data: Map<String, String>, sentAtMs: Long, nowMs: Long): SeismicAlert? {
        if (data["type"] != "seismic_alert" || sentAtMs <= 0 || sentAtMs > nowMs) return null
        val uuid = data["alert_uuid"]?.trim()?.takeIf { it.isNotEmpty() && it.length <= 200 } ?: return null
        val magnitude = data["magnitude"]?.toDoubleOrNull()?.takeIf { it.isFinite() && it in -2.0..10.0 } ?: return null
        val lat = data["latitude"]?.toDoubleOrNull()?.takeIf { it.isFinite() && it in -90.0..90.0 } ?: return null
        val lng = data["longitude"]?.toDoubleOrNull()?.takeIf { it.isFinite() && it in -180.0..180.0 } ?: return null
        val origin = data["origin_time"]?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
        val originMs = data["origin_time_ms"]?.toLongOrNull()?.takeIf { it > 0 && it <= nowMs }
            ?: origin?.toInstant()?.toEpochMilli() ?: 0L
        val serverExpiry = data["expires_at"]?.let {
            runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
        }
        val expiresAt = minOf(sentAtMs + MAX_AGE_MS, serverExpiry ?: Long.MAX_VALUE)
        return SeismicAlert(
            uuid = uuid,
            source = data["source"]?.lowercase(Locale.ROOT)?.takeIf { it in setOf("crowd", "sgc", "usgs", "simulation") } ?: "unknown",
            status = if (nowMs < expiresAt) "active" else "expired",
            magnitude = magnitude,
            latitude = lat,
            longitude = lng,
            distance_km = data["distance_km"]?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 },
            seconds_to_s_wave = data["seconds_to_s_wave"]?.toDoubleOrNull()?.takeIf { it.isFinite() },
            severity = data["severity"]?.takeIf { it in setOf("informativa", "amarilla", "naranja", "roja") } ?: "informativa",
            confidence = data["confidence"]?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..1.0 } ?: 0.0,
            device_count = data["device_count"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            origin_time_ms = originMs,
            origin_time = origin?.toString() ?: originMs.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).toString() },
            expires_at = Instant.ofEpochMilli(expiresAt).toString(),
            receivedAtMs = sentAtMs,
        )
    }
}
