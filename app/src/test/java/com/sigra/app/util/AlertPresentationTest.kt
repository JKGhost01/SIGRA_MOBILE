package com.sigra.app.util

import com.sigra.app.data.SeismicAlert
import org.junit.Assert.assertEquals
import org.junit.Test

class AlertPresentationTest {
    @Test fun unknownConfidenceIsPresentedWithoutAnInventedPercentage() {
        assertEquals("Confianza no disponible", SeismicAlert().confidenceDescription())
        assertEquals("Confianza no disponible", SeismicAlert(confidence = Double.NaN).confidenceDescription())
        assertEquals("Confianza 84%", SeismicAlert(confidence = 0.84).confidenceDescription())
    }

    @Test fun unknownSourceDoesNotImpersonateAnOfficialSourceOrTheCitizenNetwork() {
        assertEquals("Fuente no indicada", alertSourceLabel("unknown"))
        assertEquals("SGC oficial", alertSourceLabel("SGC"))
        assertEquals("Red de teléfonos", alertSourceLabel("crowd"))
    }
}
