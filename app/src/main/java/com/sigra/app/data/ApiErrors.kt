package com.sigra.app.data

import java.io.IOException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

/** Accepts both the legacy message and the structured /api/v1 error contract. */
fun apiErrorMessage(error: Throwable): String = when (error) {
    is HttpException -> {
        val detail = runCatching {
            val body = Json.parseToJsonElement(error.response()?.errorBody()?.string().orEmpty()).jsonObject
            body["message"]?.jsonPrimitive?.content
                ?: body["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull()?.take(240)
        when (error.code()) {
            401, 403 -> "Acceso rechazado (${error.code()}). Revisa las claves de esta compilación y el registro del dispositivo."
            404 -> "Ruta no encontrada (404). Comprueba que el servidor tenga la API SIGRA actualizada."
            429 -> "Demasiadas solicitudes. Espera un momento antes de actualizar."
            500, 502, 503, 504 -> "El servidor SIGRA no está disponible (${error.code()}). Conservamos los últimos datos recibidos."
            else -> "Error ${error.code()}: ${detail ?: "el servidor rechazó la solicitud"}"
        }
    }
    is kotlinx.serialization.SerializationException -> "La respuesta no coincide con la API de SIGRA. Comprueba el servidor configurado."
    is UnknownHostException -> "No se encuentra el servidor SIGRA. Comprueba la conexión y el dominio configurado."
    is SSLException -> "No se pudo establecer una conexión HTTPS segura con SIGRA."
    is IOException -> "Sin conexión con SIGRA. Revisa tu red y vuelve a intentarlo."
    else -> error.message ?: "No se pudo completar la consulta."
}
