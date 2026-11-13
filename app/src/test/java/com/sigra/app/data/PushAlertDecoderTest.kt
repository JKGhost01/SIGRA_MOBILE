package com.sigra.app.data

import com.sigra.app.util.remainingSeconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushAlertDecoderTest {
    private val payload = mapOf("type" to "seismic_alert", "alert_uuid" to "test", "magnitude" to "4.0",
        "latitude" to "4.6", "longitude" to "-74.1", "seconds_to_s_wave" to "20")

    @Test fun delayedPushUsesSendTimeForCountdown() {
        val alert = PushAlertDecoder.decode(payload, 100_000, 112_000)!!
        assertEquals(8, alert.remainingSeconds(112_000))
        assertEquals(100_000L, alert.receivedAtMs)
    }

    @Test fun oldPushIsSavedAsExpiredWithoutCountdown() {
        val now = 100_000 + PushAlertDecoder.MAX_AGE_MS + 1
        val alert = PushAlertDecoder.decode(payload, 100_000, now)!!
        assertEquals("expired", alert.status)
        assertNull(alert.remainingSeconds(now))
    }

    @Test fun missingSourceAndConfidenceAreNotInvented() {
        val alert = PushAlertDecoder.decode(payload, 100_000, 112_000)!!
        assertEquals("unknown", alert.source)
        assertEquals(0.0, alert.confidence, 0.0)
    }

    @Test fun invalidMandatoryDataIsRejected() {
        listOf(payload - "latitude", payload + ("latitude" to "NaN"), payload + ("longitude" to "181"),
            payload + ("magnitude" to "Infinity"), payload + ("alert_uuid" to "  "), payload + ("type" to "other")).forEach {
            assertNull(PushAlertDecoder.decode(it, 100_000, 112_000))
        }
    }

    @Test fun unknownOrFutureSendTimeIsNotTreatedAsNow() {
        assertNull(PushAlertDecoder.decode(payload, 0, 112_000))
        assertNull(PushAlertDecoder.decode(payload, 120_000, 112_000))
    }

    @Test fun invalidOptionalFieldsDoNotBreakTheMessage() {
        val alert = PushAlertDecoder.decode(payload + mapOf("confidence" to "NaN", "device_count" to "-5",
            "distance_km" to "Infinity", "seconds_to_s_wave" to "NaN"), 100_000, 112_000)!!
        assertEquals(0.0, alert.confidence, 0.0)
        assertEquals(0, alert.device_count)
        assertNull(alert.distance_km)
        assertNull(alert.seconds_to_s_wave)
    }

    @Test fun explicitServerExpirationCanShortenButNotExtendClientFreshness() {
        val expired = PushAlertDecoder.decode(payload + ("expires_at" to "1970-01-01T00:01:50Z"), 100_000, 112_000)!!
        assertEquals("expired", expired.status)
        val capped = PushAlertDecoder.decode(payload + ("expires_at" to "2100-01-01T00:00:00Z"), 100_000, 112_000)!!
        assertEquals(java.time.Instant.ofEpochMilli(100_000 + PushAlertDecoder.MAX_AGE_MS).toString(), capped.expires_at)
    }

    @Test fun originTimeFromServerIsKeptSeparateFromDeliveryTime() {
        val alert = PushAlertDecoder.decode(payload + ("origin_time" to "1970-01-01T00:01:30Z"), 100_000, 112_000)!!
        assertEquals(90_000L, alert.origin_time_ms)
        assertEquals(100_000L, alert.receivedAtMs)
    }
}
