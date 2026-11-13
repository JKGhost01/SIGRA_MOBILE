package com.sigra.app.util

import com.sigra.app.data.SeismicAlert
import kotlin.math.roundToInt
import java.util.Locale

fun alertSourceLabel(source: String): String = when (source.lowercase(Locale.ROOT)) {
    "sgc" -> "SGC oficial"
    "usgs" -> "USGS oficial"
    "emsc" -> "EMSC oficial"
    "geonet" -> "GeoNet oficial"
    "geofon" -> "GEOFON oficial"
    "ingv" -> "INGV oficial"
    "crowd" -> "Red de teléfonos"
    "simulation" -> "Prueba"
    else -> "Fuente no indicada"
}

fun SeismicAlert.confidenceDescription(): String =
    if (confidence.isFinite() && confidence > 0 && confidence <= 1) "Confianza ${(confidence * 100).roundToInt()}%"
    else "Confianza no disponible"
