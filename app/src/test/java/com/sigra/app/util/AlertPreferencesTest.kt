package com.sigra.app.util

import com.sigra.app.data.SeismicAlert
import com.sigra.app.data.UserSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertPreferencesTest {
    private val settings = UserSettings(alertRadiusKm = 250, minMagnitude = 3f)
    private val alert = SeismicAlert(uuid = "test", magnitude = 3.0, distance_km = 250.0)

    @Test fun configuredBoundariesAreInclusive() {
        assertTrue(alert.matchesNotificationPreferences(settings))
        assertFalse(alert.copy(magnitude = 2.9).matchesNotificationPreferences(settings))
        assertFalse(alert.copy(distance_km = 250.1).matchesNotificationPreferences(settings))
    }

    @Test fun disabledDuplicateAndExpiredAlertsDoNotNotify() {
        assertFalse(alert.matchesNotificationPreferences(settings.copy(notificationsEnabled = false)))
        assertFalse(alert.matchesNotificationPreferences(settings.copy(seenAlerts = setOf("test"))))
        assertFalse(alert.copy(status = "expired").matchesNotificationPreferences(settings))
        assertFalse(alert.copy(expires_at = "2000-01-01T00:00:00Z").matchesNotificationPreferences(settings))
    }

    @Test fun ownLocationTakesPrecedenceOverStalePushDistance() {
        val located = settings.copy(lastLat = 4.6, lastLng = -74.1)
        assertTrue(alert.copy(latitude = 4.6, longitude = -74.1, distance_km = 1000.0).matchesNotificationPreferences(located))
        assertFalse(alert.copy(latitude = 40.0, longitude = 10.0, distance_km = 0.0).matchesNotificationPreferences(located))
    }

    @Test fun unknownOrInvalidDistanceCannotSatisfyRadius() {
        assertFalse(alert.copy(distance_km = null).matchesNotificationPreferences(settings))
        assertFalse(alert.copy(distance_km = Double.NaN).matchesNotificationPreferences(settings))
        assertFalse(alert.copy(distance_km = -1.0).matchesNotificationPreferences(settings))
    }
}
