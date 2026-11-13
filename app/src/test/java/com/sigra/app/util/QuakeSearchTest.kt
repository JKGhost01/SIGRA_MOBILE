package com.sigra.app.util

import com.sigra.app.data.SismoEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuakeSearchTest {
    private val event = SismoEvent(ubicacion = "Los Santos, Santander, Colombia", fuente = "SGC", agency = "Servicio Geológico Colombiano")

    @Test fun whitespaceAndEmptySearchKeepTheEvent() {
        assertTrue(event.matchesSearch(""))
        assertTrue(event.matchesSearch("   "))
    }

    @Test fun searchIgnoresCaseAccentsAndExtraSpaces() {
        assertTrue(event.matchesSearch("  geologico   COLOMBIANO "))
        assertTrue(event.matchesSearch("SÁNTANDER"))
    }

    @Test fun termsCanMatchBothLocationAndSource() {
        assertTrue(event.matchesSearch("santos sgc"))
        assertFalse(event.matchesSearch("santos usgs"))
    }
}
