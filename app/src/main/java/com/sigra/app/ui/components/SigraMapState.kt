package com.sigra.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.sigra.app.data.SismoEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/** Camera values are saved; the native view is only held while this screen is mounted. */
class SigraMapState internal constructor(
    internal var latitude: Double = 15.0,
    internal var longitude: Double = 0.0,
    internal var zoom: Double = 2.5,
    internal var filterRadius: Int = 0,
) {
    private var view: MapView? = null

    internal fun attach(map: MapView) { view = map }

    internal fun capture(map: MapView) {
        latitude = map.mapCenter.latitude
        longitude = map.mapCenter.longitude
        zoom = map.zoomLevelDouble
    }

    internal fun detach(map: MapView) {
        capture(map)
        if (view === map) view = null
    }

    fun zoomIn() { view?.controller?.zoomIn() }
    fun zoomOut() { view?.controller?.zoomOut() }

    fun centerAt(lat: Double, lng: Double, level: Double = 8.0) {
        if (!lat.isFinite() || !lng.isFinite() || lat !in -90.0..90.0 || lng !in -180.0..180.0) return
        latitude = lat
        longitude = lng
        val requestedZoom = level.coerceIn(2.0, 18.0)
        zoom = requestedZoom
        view?.let { map ->
            // Zoom listeners update the saved camera synchronously; keep the requested point immutable.
            map.controller.setZoom(requestedZoom)
            map.controller.setCenter(GeoPoint(lat, lng))
            capture(map)
        }
    }

    fun showEvents(events: List<SismoEvent>) {
        val points = events.filter { it.latitud.isFinite() && it.longitud.isFinite() &&
            it.latitud in -90.0..90.0 && it.longitud in -180.0..180.0 }.map { GeoPoint(it.latitud, it.longitud) }
        when (points.size) {
            0 -> centerAt(15.0, 0.0, 2.5)
            1 -> centerAt(points.first().latitude, points.first().longitude)
            else -> view?.let { map ->
                val padding = (40 * map.resources.displayMetrics.density).toInt()
                map.zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true, padding, 10.0, 350L)
            }
        }
    }

    companion object {
        val Saver = listSaver<SigraMapState, Any>(
            save = { listOf(it.latitude, it.longitude, it.zoom, it.filterRadius) },
            restore = { SigraMapState(it[0] as Double, it[1] as Double, it[2] as Double, it[3] as Int) },
        )
    }
}

@Composable
fun rememberSigraMapState(): SigraMapState = rememberSaveable(saver = SigraMapState.Saver) { SigraMapState() }
