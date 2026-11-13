package com.sigra.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class QuakeFiltersTest {
    @Test
    fun missingPreferencesUseWorldwideDefaults() {
        assertEquals(QuakeFilters(), QuakeFilters.fromStoredValues(null, null, null))
    }

    @Test
    fun savedPreferencesRestoreApiValues() {
        assertEquals(
            QuakeFilters(500, "USGS", QuakeOrder.MAGNITUDE),
            QuakeFilters.fromStoredValues(500, "USGS", "magnitud"),
        )
    }

    @Test
    fun unsupportedPreferencesFallBackToDefaults() {
        assertEquals(QuakeFilters(), QuakeFilters.fromStoredValues(-50, "unknown", "unknown"))
        assertEquals(0, QuakeFilters.fromStoredValues(123, null, null).radiusKm)
    }

    @Test
    fun restoringWithoutLocationRemovesDistanceRequirementsAndKeepsSource() {
        val saved = QuakeFilters(250, "SGC", QuakeOrder.DISTANCE)
        assertEquals(QuakeFilters(0, "SGC", QuakeOrder.DATE), saved.forLocation(false))
        assertEquals(saved, saved.forLocation(true))
    }

    @Test
    fun magnitudeOrderDoesNotRequireLocation() {
        val saved = QuakeFilters(1000, "USGS", QuakeOrder.MAGNITUDE)
        assertEquals(QuakeFilters(0, "USGS", QuakeOrder.MAGNITUDE), saved.forLocation(false))
    }
    @Test fun allSixOfficialSourcesSurviveRestoration() {
        listOf("SGC", "USGS", "EMSC", "GEONET", "GEOFON", "INGV").forEach { source ->
            assertEquals(source, QuakeFilters.fromStoredValues(500, source, "fecha").source)
        }
    }

    @Test fun periodAndMagnitudeSurviveLocationFallback() {
        val filters = QuakeFilters.fromStoredValues(250, "EMSC", "distancia", 336, 4.0)
        assertEquals(336, filters.hours)
        assertEquals(4.0, filters.minMagnitude!!, 0.0)
        assertEquals(filters.copy(radiusKm = 0, order = QuakeOrder.DATE), filters.forLocation(false))
    }

    @Test fun invalidStoredPeriodAndMagnitudeUseSupportedDefaults() {
        val filters = QuakeFilters.fromStoredValues(null, null, null, 999, Double.NaN)
        assertEquals(120, filters.hours)
        assertEquals(null, filters.minMagnitude)
    }

}
