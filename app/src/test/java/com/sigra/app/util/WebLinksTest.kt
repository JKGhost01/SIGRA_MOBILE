package com.sigra.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebLinksTest {
    private val base = "https://sigra.example/"

    @Test fun absoluteOfficialLinksArePreserved() {
        assertEquals("https://earthquake.usgs.gov/earthquakes/eventpage/test", webUrl("https://earthquake.usgs.gov/earthquakes/eventpage/test", base))
    }

    @Test fun backendRelativeLinksResolveAgainstConfiguredServer() {
        assertEquals("https://sigra.example/sismos/test", webUrl("/sismos/test", base))
        assertEquals("https://sigra.example/mobile/sismos/test", webUrl("sismos/test", "https://sigra.example/mobile/"))
    }

    @Test fun unsafeSchemesAndEmbeddedCredentialsAreRejected() {
        listOf("javascript:alert(1)", "file:///etc/passwd", "intent://test", "https://user:secret@example.com/").forEach {
            assertNull(webUrl(it, base))
        }
    }

    @Test fun emptyAndMalformedLinksHaveNoAction() {
        assertNull(webUrl(null, base))
        assertNull(webUrl("   ", base))
        assertNull(webUrl("https://[invalid", base))
    }
}
