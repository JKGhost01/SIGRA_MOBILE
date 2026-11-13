package com.sigra.app.util

import com.sigra.app.data.SeismicAlert
import com.sigra.app.data.UserSettings

fun SeismicAlert.matchesNotificationPreferences(settings: UserSettings): Boolean {
    if (!settings.notificationsEnabled || uuid.isBlank() || uuid in settings.seenAlerts) return false
    if (status != "active" || hasExpired() || !magnitude.isFinite() || magnitude < settings.minMagnitude) return false
    val distance = if (settings.hasLocation && latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0) {
        Geo.distanceKm(settings.lastLat!!, settings.lastLng!!, latitude, longitude)
    } else {
        distance_km?.takeIf { it.isFinite() && it >= 0 }
    }
    return distance != null && distance <= settings.alertRadiusKm
}
