package com.sigra.app.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SismosContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun worldwideResponseWithoutLocationAcceptsNullDistance() {
        val response = json.decodeFromString<SismosResponse>(
            """{
                "status":"ok","count":1,"hours":120,"origin":null,
                "events":[{"id":"us-test","fuente":"USGS","distancia_km":null,"magnitud":3.2}]
            }""",
        )
        assertNull(response.events.single().distancia_km)
        assertEquals("USGS", response.events.single().fuente)
        assertEquals(0, response.events.single().citizen_reports_count)
    }

    @Test
    fun confirmedCitizenCountIsSeparateFromOfficialFeltReports() {
        val event = json.decodeFromString<SismoEvent>(
            """{"id":"SGC-test","fuente":"SGC","felt":12,"citizen_reports_count":3,"distancia_km":25.1}""",
        )
        assertEquals(12, event.felt)
        assertEquals(3, event.citizen_reports_count)
        assertEquals(25.1, event.distancia_km!!, 0.001)
    }

    @Test
    fun previousServerResponseKeepsCompatibleDefaults() {
        val event = json.decodeFromString<SismoEvent>("""{"id":"old-server","ubicacion":"Colombia"}""")
        assertEquals("SGC", event.fuente)
        assertNull(event.distancia_km)
        assertEquals(0, event.citizen_reports_count)
    }
}
