package com.sigra.app.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SigraApi {
    @GET("api/health")
    suspend fun health(): retrofit2.Response<HealthResponse>

    @POST("api/red/registro")
    suspend fun registro(
        @Header("X-SIGRA-KEY") appKey: String,
        @Body body: RegistrationRequest,
        @Header("X-Device-Token") currentToken: String? = null,
    ): RegistrationResponse

    @POST("api/red/estado")
    suspend fun estado(
        @Header("X-SIGRA-KEY") appKey: String,
        @Header("X-Device-Token") deviceToken: String,
        @Body body: HeartbeatRequest,
    ): HeartbeatResponse

    @POST("api/red/deteccion")
    suspend fun deteccion(
        @Header("X-SIGRA-KEY") appKey: String,
        @Header("X-Device-Token") deviceToken: String,
        @Body body: DetectionRequest,
    ): DetectionResponse

    @GET("api/red/alertas")
    suspend fun alertas(
        @Header("X-SIGRA-KEY") appKey: String,
        @Header("X-Device-Token") deviceToken: String?,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
    ): AlertsResponse

    @GET("api/red/red")
    suspend fun red(): PublicStatsResponse

    @GET("api/sismos/recent")
    suspend fun sismos(
        @Header("Authorization") authorization: String,
        @Query("hours") hours: Int = 120,
        @Query("min_magnitude") minMagnitude: Double? = null,
        @Query("limit") limit: Int = 1000,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radius_km") radiusKm: Int? = null,
        @Query("fuente") source: String? = null,
        @Query("order") order: String = "fecha",
    ): SismosResponse

    @POST("api/red/desactivar")
    suspend fun desactivar(
        @Header("X-SIGRA-KEY") appKey: String,
        @Header("X-Device-Token") deviceToken: String,
    ): Map<String, String>
}
