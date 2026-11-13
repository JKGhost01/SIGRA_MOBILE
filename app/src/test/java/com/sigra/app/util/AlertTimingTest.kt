package com.sigra.app.util

import com.sigra.app.data.SeismicAlert
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertTimingTest {
    private val alert = SeismicAlert(uuid = "test", seconds_to_s_wave = 20.0, receivedAtMs = 100_000)

    @Test fun countdownUsesElapsedTimeInsteadOfRestarting() {
        assertEquals(20, alert.remainingSeconds(100_000))
        assertEquals(8, alert.remainingSeconds(112_000))
        assertEquals(-10, alert.remainingSeconds(130_000))
    }

    @Test fun fractionsRoundUpAndClockCorrectionsDoNotAddTime() {
        assertEquals(20, alert.remainingSeconds(100_100))
        assertEquals(20, alert.remainingSeconds(90_000))
    }

    @Test fun missingOrNonFiniteEtaNeverBecomesAnImmediateArrival() {
        assertNull(alert.copy(seconds_to_s_wave = null).remainingSeconds(100_000))
        assertNull(alert.copy(seconds_to_s_wave = Double.NaN).remainingSeconds(100_000))
        assertEquals("Tiempo de llegada no disponible", alert.copy(seconds_to_s_wave = null).arrivalLabel(100_000))
    }

    @Test fun inactiveAndExpiredAlertsHaveNoCountdown() {
        assertNull(alert.copy(status = "expired").remainingSeconds(100_000))
        assertNull(alert.copy(expires_at = "1970-01-01T00:01:40Z").remainingSeconds(100_000))
        assertNull(alert.copy(expires_at = "1970-01-01 00:01:40", origin_time = "1970-01-01T00:00:00Z").remainingSeconds(100_000))
        assertEquals("Alerta finalizada", alert.copy(status = "expired").arrivalLabel(100_000))
    }

    @Test fun elapsedArrivalIsNotPresentedAsCurrentGroundMotion() {
        assertEquals("Tiempo estimado de llegada transcurrido", alert.arrivalLabel(130_000))
    }

    @Test fun savingAndReopeningKeepsOriginalReference() {
        val json = Json { encodeDefaults = true }
        val restored = json.decodeFromString<SeismicAlert>(json.encodeToString(alert))
        assertEquals(alert.receivedAtMs, restored.receivedAtMs)
        assertEquals(8, restored.remainingSeconds(112_000))
    }

    @Test fun sqlExpirationUsesServerOffsetRatherThanAssumingUtc() {
        val local = alert.copy(expires_at = "1970-01-01 00:01:40", origin_time = "1970-01-01T00:00:00-05:00")
        assertFalse(local.hasExpired(100_000))
        assertTrue(local.hasExpired(18_100_000))
        assertFalse(local.copy(origin_time = null).hasExpired(100_000))
    }
}
