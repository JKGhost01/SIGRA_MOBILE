package com.sigra.app.data

import java.net.UnknownHostException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ApiErrorsTest {
    private fun http(code: Int, body: String) = HttpException(
        Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    @Test fun understandsLegacyAndVersionedValidationErrors() {
        assertEquals("Error 422: Radio inválido", apiErrorMessage(http(422, """{"message":"Radio inválido"}""")))
        assertEquals("Error 422: Radio inválido", apiErrorMessage(http(422, """{"error":{"code":"validation","message":"Radio inválido"}}""")))
    }

    @Test fun authorizationPointsToConfigurationWithoutEchoingServerDetails() {
        val message = apiErrorMessage(http(401, """{"message":"untrusted server content"}"""))
        assertTrue(message.contains("claves"))
        assertFalse(message.contains("untrusted"))
    }

    @Test fun incompatibleJsonDoesNotExposeTheRawPayload() {
        val message = apiErrorMessage(kotlinx.serialization.SerializationException("raw payload"))
        assertTrue(message.contains("API de SIGRA"))
        assertFalse(message.contains("raw payload"))
    }

    @Test fun htmlFailureDoesNotLeakIntoTheUi() {
        val message = apiErrorMessage(http(503, "<html>database exception trace</html>"))
        assertTrue(message.contains("503"))
        assertFalse(message.contains("trace"))
    }

    @Test fun distinguishesAnUnresolvableHostFromAnEmptyCatalog() {
        assertTrue(apiErrorMessage(UnknownHostException()).contains("dominio"))
    }
}
