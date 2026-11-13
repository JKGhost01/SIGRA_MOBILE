package com.sigra.app.util

import com.sigra.app.data.SeismicAlert
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlin.math.ceil

fun SeismicAlert.hasExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
    val expires = expires_at?.let { value ->
        runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching {
                // SQL timestamps use the server's timezone, which is carried by origin_time.
                // Without an offset, do not guess based on the phone's timezone.
                val origin = origin_time ?: return@runCatching null
                val offset = OffsetDateTime.parse(origin).offset
                LocalDateTime.parse(value.replace(' ', 'T')).toInstant(offset).toEpochMilli()
            }.getOrNull()
    }
    return expires != null && expires <= nowMs
}

fun SeismicAlert.remainingSeconds(nowMs: Long = System.currentTimeMillis()): Int? {
    if (status != "active" || hasExpired(nowMs)) return null
    val seconds = seconds_to_s_wave?.takeIf { it.isFinite() } ?: return null
    val elapsed = (nowMs - receivedAtMs).coerceAtLeast(0) / 1000.0
    return ceil(seconds - elapsed).coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()).toInt()
}

fun SeismicAlert.arrivalLabel(nowMs: Long = System.currentTimeMillis()): String {
    val seconds = remainingSeconds(nowMs)
    return when {
        status != "active" || hasExpired(nowMs) -> "Alerta finalizada"
        seconds == null -> "Tiempo de llegada no disponible"
        seconds > 0 -> "Onda S estimada en $seconds s"
        else -> "Tiempo estimado de llegada transcurrido"
    }
}
