package com.sigra.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsTest {
    @Test fun coordinatesMustBePresentTogether() {
        assertFalse(UserSettings().hasLocation)
        assertFalse(UserSettings(lastLat = 4.6).hasLocation)
        assertFalse(UserSettings(lastLng = -74.1).hasLocation)
    }

    @Test fun zeroAndWorldBoundariesAreValidLocations() {
        assertTrue(UserSettings(lastLat = 0.0, lastLng = 0.0).hasLocation)
        assertTrue(UserSettings(lastLat = -90.0, lastLng = 180.0).hasLocation)
    }

    @Test fun nonFiniteAndOutOfRangeCoordinatesAreNotLocations() {
        assertFalse(UserSettings(lastLat = Double.NaN, lastLng = -74.1).hasLocation)
        assertFalse(UserSettings(lastLat = 4.6, lastLng = Double.POSITIVE_INFINITY).hasLocation)
        assertFalse(UserSettings(lastLat = 91.0, lastLng = -74.1).hasLocation)
        assertFalse(UserSettings(lastLat = 4.6, lastLng = -181.0).hasLocation)
    }
}
