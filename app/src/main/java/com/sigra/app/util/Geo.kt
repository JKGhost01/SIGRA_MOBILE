package com.sigra.app.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val radioTierra = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        return 2 * radioTierra * atan2(sqrt(a), sqrt(1 - a))
    }

    fun formatoDistancia(km: Double?): String {
        if (km == null) return "Distancia desconocida"
        return if (km < 10) "A ${"%.1f".format(km)} km" else "A ${km.toInt()} km"
    }

    fun formatoMagnitud(m: Double): String = "M ${"%.1f".format(m)}"
}
