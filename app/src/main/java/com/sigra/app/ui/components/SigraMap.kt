package com.sigra.app.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.util.Locale
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.sigra.app.R
import com.sigra.app.data.SeismicAlert
import com.sigra.app.data.SismoEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.log2
import com.sigra.app.util.arrivalLabel

@Composable
fun SigraMap(
    modifier: Modifier = Modifier,
    userLat: Double?,
    userLng: Double?,
    alertas: List<SeismicAlert>,
    sismos: List<SismoEvent>,
    radiusKm: Int = 0,
    onEventClick: (SismoEvent) -> Unit = {},
    state: SigraMapState = rememberSigraMapState(),
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEventClick by rememberUpdatedState(onEventClick)
    val rendered = remember { RenderedMapData() }
    val darkMap = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tileFilter = remember(darkMap) {
        if (darkMap) ColorMatrixColorFilter(floatArrayOf(
            -0.10f, -0.30f, -0.05f, 0f, 130f,
            -0.10f, -0.30f, -0.05f, 0f, 140f,
            -0.10f, -0.30f, -0.05f, 0f, 155f,
            0f, 0f, 0f, 1f, 0f,
        )) else null
    }

    val mapView = remember {
        MapView(context).apply {
            // The native map draws while panning; constrain it independently of Compose siblings.
            outlineProvider = ViewOutlineProvider.BOUNDS
            clipToOutline = true
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setDestroyMode(false) // Compose owns the single onDetach call below.
            minZoomLevel = 2.0
            maxZoomLevel = 18.0
            setVerticalMapRepetitionEnabled(false)
            controller.setZoom(state.zoom)
            controller.setCenter(GeoPoint(state.latitude, state.longitude))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        state.attach(mapView)
        val movement = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean { state.capture(mapView); return false }
            override fun onZoom(event: ZoomEvent?): Boolean { state.capture(mapView); return false }
        }
        mapView.addMapListener(movement)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            state.detach(mapView)
            mapView.removeMapListener(movement)
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { mapa ->
                mapa.overlayManager.tilesOverlay.setColorFilter(tileFilter)
                mapa.overlayManager.tilesOverlay.loadingBackgroundColor = if (darkMap) AndroidColor.rgb(15, 22, 34) else AndroidColor.rgb(243, 246, 250)
                if (radiusKm != state.filterRadius && radiusKm > 0 && userLat != null && userLng != null) {
                    state.centerAt(userLat, userLng, log2(20000.0 / radiusKm).coerceIn(2.0, 12.0))
                }
                state.filterRadius = radiusKm

                // A refresh or camera change should not recreate a thousand unchanged markers.
                if (rendered.matches(userLat, userLng, radiusKm, sismos, alertas)) {
                    mapa.invalidate()
                    return@AndroidView
                }
                rendered.update(userLat, userLng, radiusKm, sismos, alertas)

                mapa.overlays.clear()

                val iconoUsuario = ContextCompat.getDrawable(context, R.drawable.ic_user)
                val iconoAlerta = ContextCompat.getDrawable(context, R.drawable.ic_marker)
                val eventIcons = mutableMapOf<String, Drawable>()

                if (userLat != null && userLng != null) {
                    if (radiusKm > 0) {
                        Polygon(mapa).apply {
                            points = circulo(userLat, userLng, radiusKm.toDouble())
                            fillColor = AndroidColor.argb(25, 8, 127, 120)
                            outlinePaint.color = AndroidColor.rgb(8, 127, 120)
                            outlinePaint.strokeWidth = 3f
                        }.also { mapa.overlays.add(it) }
                    }
                    Marker(mapa).apply {
                        position = GeoPoint(userLat, userLng)
                        icon = iconoUsuario
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Tu teléfono"
                        snippet = "Ubicación guardada de este teléfono"
                    }.also { mapa.overlays.add(it) }
                }

                sismos.forEach { sismo ->
                    Marker(mapa).apply {
                        position = GeoPoint(sismo.latitud, sismo.longitud)
                        val label = String.format(Locale.ROOT, "%.1f", sismo.magnitud)
                        icon = eventIcons.getOrPut("${sismo.nivel}:$label") {
                            epicenterIcon(context.resources, label, colorPorNivel(sismo.nivel).toArgb())
                        }
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "M ${"%.1f".format(sismo.magnitud)} · ${sismo.ubicacion}"
                        snippet = "${sismo.fuente} · ${sismo.profundidad.toInt()} km de profundidad" +
                            if (sismo.citizen_reports_count > 0) " · Reportado por ${sismo.citizen_reports_count} usuarios" else ""
                        setOnMarkerClickListener { _, _ -> currentOnEventClick(sismo); true }
                    }.also { mapa.overlays.add(it) }
                }

                alertas.take(10).forEach { alerta ->
                    val color = colorSeveridad(alerta.severity)
                    if (alerta.radius_km > 0) {
                        val puntos = circulo(alerta.latitude, alerta.longitude, alerta.radius_km)
                        Polygon(mapa).apply {
                            points = puntos
                            fillColor = color.copy(alpha = 0.10f).toArgb()
                            outlinePaint.color = color.copy(alpha = 0.65f).toArgb()
                            outlinePaint.strokeWidth = 3f
                        }.also { mapa.overlays.add(it) }
                    }

                    Marker(mapa).apply {
                        position = GeoPoint(alerta.latitude, alerta.longitude)
                        icon = iconoAlerta
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Alerta M ${"%.1f".format(alerta.magnitude)}"
                        snippet = "${alerta.arrivalLabel()} · ${alerta.device_count} teléfonos"
                    }.also { mapa.overlays.add(it) }
                }

                mapa.invalidate()
            },
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shape = MaterialTheme.shapes.small,
        ) {
            TextButton(onClick = { runCatching { uriHandler.openUri("https://www.openstreetmap.org/copyright") } },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text("© OpenStreetMap contributors", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private class RenderedMapData {
    private var lat: Double? = null
    private var lng: Double? = null
    private var radius: Int = -1
    private var events: List<SismoEvent> = emptyList()
    private var alerts: List<SeismicAlert> = emptyList()
    fun matches(lat: Double?, lng: Double?, radius: Int, events: List<SismoEvent>, alerts: List<SeismicAlert>) =
        this.lat == lat && this.lng == lng && this.radius == radius && this.events == events && this.alerts == alerts
    fun update(lat: Double?, lng: Double?, radius: Int, events: List<SismoEvent>, alerts: List<SeismicAlert>) {
        this.lat = lat; this.lng = lng; this.radius = radius; this.events = events; this.alerts = alerts
    }
}

private fun circulo(lat: Double, lng: Double, radioKm: Double, pasos: Int = 64): List<GeoPoint> {
    val radioTierra = 6371.0
    val distancia = radioKm / radioTierra
    val latRad = Math.toRadians(lat)
    val lngRad = Math.toRadians(lng)
    val puntos = ArrayList<GeoPoint>(pasos)

    for (i in 0 until pasos) {
        val angulo = 2.0 * Math.PI * i / pasos
        val latPunto = Math.asin(
            sin(latRad) * cos(distancia) + cos(latRad) * sin(distancia) * cos(angulo),
        )
        val lngPunto = lngRad + kotlin.math.atan2(
            sin(angulo) * sin(distancia) * cos(latRad),
            cos(distancia) - sin(latRad) * sin(latPunto),
        )
        puntos.add(GeoPoint(Math.toDegrees(latPunto), Math.toDegrees(lngPunto)))
    }

    return puntos
}

/** Drawn locally: official magnitude labels do not depend on third-party icon assets. */
private fun epicenterIcon(resources: android.content.res.Resources, label: String, color: Int): Drawable {
    val density = resources.displayMetrics.density
    val size = (38 * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = AndroidColor.rgb(15, 22, 34)
    canvas.drawCircle(center, center, center - 2 * density, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2 * density
    paint.color = color
    canvas.drawCircle(center, center, center - 2 * density, paint)
    paint.style = Paint.Style.FILL
    paint.color = AndroidColor.WHITE
    paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = 11 * density
    canvas.drawText(label, center, center - (paint.ascent() + paint.descent()) / 2f, paint)
    return BitmapDrawable(resources, bitmap)
}
