package com.sigra.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object LocationHelper {
    fun isRecent(location: Location): Boolean {
        val ageNanos = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        return location.latitude.isFinite() && location.longitude.isFinite() &&
            location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0 &&
            ageNanos in 0L..300_000_000_000L
    }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun getLocation(context: Context, timeoutMs: Long = 8_000): Location? {
        if (!hasPermission(context)) return null

        val cliente = LocationServices.getFusedLocationProviderClient(context)

        val ultima = withTimeoutOrNull(2_000) {
            suspendCancellableCoroutine { cont ->
                cliente.lastLocation
                    .addOnSuccessListener { location -> if (cont.isActive) cont.resume(location) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
        }

        if (ultima != null && isRecent(ultima)) {
            return ultima
        }

        val actual = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val cancellation = CancellationTokenSource()
                cont.invokeOnCancellation { cancellation.cancel() }
                cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                    .addOnSuccessListener { location -> if (cont.isActive) cont.resume(location) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
        }

        return actual?.takeIf(::isRecent) ?: ultima?.takeIf(::isRecent)
    }
}
