package com.sigra.app.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Captures real Retrofit requests without opening sockets or contacting production. */
class SigraApiContractTest {
    private val requests = mutableListOf<Request>()
    private fun api(payload: String, code: Int = 200): SigraApi {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests.add(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("contract fixture")
                .body(payload.toResponseBody("application/json".toMediaType())).build()
        }.build()
        return Retrofit.Builder().baseUrl("https://sigra.example/").client(client)
            .addConverterFactory(Json { ignoreUnknownKeys = true; explicitNulls = false }
                .asConverterFactory("application/json".toMediaType()))
            .build().create(SigraApi::class.java)
    }

    @Test fun catalogUsesTheDocumentedPathAndServerSideFilters() = runBlocking {
        val response = api("""{"status":"ok","count":1,"events":[{"id":"gf-test","fuente":"GEOFON","distancia_km":null,"closer_towns":[{"name":"Test"}]}]}""")
            .sismos("Bearer catalog-key", hours = 336, minMagnitude = 4.0, source = "GEOFON", order = "magnitud")
        val request = requests.single()
        assertEquals("/api/sismos/recent", request.url.encodedPath)
        assertEquals("Bearer catalog-key", request.header("Authorization"))
        assertNull(request.header("X-SIGRA-KEY"))
        assertEquals("336", request.url.queryParameter("hours"))
        assertEquals("4.0", request.url.queryParameter("min_magnitude"))
        assertEquals("GEOFON", request.url.queryParameter("fuente"))
        assertEquals("magnitud", request.url.queryParameter("order"))
        assertNull(request.url.queryParameter("lat"))
        assertNull(request.url.queryParameter("radius_km"))
        assertEquals("GEOFON", response.events.single().fuente)
        assertNotNull(response.events.single().closer_towns)
    }

    @Test fun rotatingAnExistingRegistrationSendsItsCurrentToken() = runBlocking {
        api("""{"status":"ok","device_token":"new-token"}""").registro(
            "network-key", RegistrationRequest(device_uuid = "existing-device"), currentToken = "current-token")
        val request = requests.single()
        assertEquals("/api/red/registro", request.url.encodedPath)
        assertEquals("network-key", request.header("X-SIGRA-KEY"))
        assertEquals("current-token", request.header("X-Device-Token"))
        assertNull(request.header("Authorization"))
    }

    @Test fun firstRegistrationDoesNotInventADeviceToken() = runBlocking {
        api("""{"status":"ok","device_token":"new-token"}""").registro(
            "network-key", RegistrationRequest(device_uuid = "new-device"))
        assertNull(requests.single().header("X-Device-Token"))
    }

    @Test fun healthIsPublicAndDoesNotRegisterADevice() = runBlocking {
        val result = api("""{"status":"ok","service":"sigra","version":"1.3.1","api_version":"v1","database":"ok"}""").health()
        assertEquals("/api/health", requests.single().url.encodedPath)
        assertEquals("GET", requests.single().method)
        assertNull(requests.single().header("Authorization"))
        assertNull(requests.single().header("X-SIGRA-KEY"))
        assertEquals("sigra", result.body()?.service)
        assertEquals("v1", result.body()?.api_version)
    }

    @Test fun databaseFailureDoesNotBecomeAnEmptySuccessfulFeed() = runBlocking {
        val result = api("""{"status":"degraded","service":"sigra","database":"error"}""", 503).health()
        assertFalse(result.isSuccessful)
        assertEquals(503, result.code())
    }
}
