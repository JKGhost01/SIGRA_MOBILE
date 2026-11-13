package com.sigra.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Each action owns an equal share of the bar; none covers the map or hides offscreen. */
@Composable
fun MapControls(
    locating: Boolean,
    hasEvents: Boolean,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onLocation: () -> Unit,
    onFitEvents: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            MapAction("Alejar", Icons.Default.Remove, onZoomOut, Modifier.weight(1f))
            MapAction("Acercar", Icons.Default.Add, onZoomIn, Modifier.weight(1f))
            MapAction(if (locating) "Buscando…" else "Ubicación", Icons.Default.MyLocation,
                onLocation, Modifier.weight(1f), enabled = !locating, loading = locating)
            MapAction("Encuadrar", Icons.Default.CenterFocusStrong, onFitEvents,
                Modifier.weight(1f), enabled = hasEvents)
        }
    }
}

@Composable
private fun MapAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}
