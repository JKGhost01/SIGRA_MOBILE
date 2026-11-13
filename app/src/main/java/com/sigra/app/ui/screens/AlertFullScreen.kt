package com.sigra.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sigra.app.data.SeismicAlert
import com.sigra.app.ui.components.fuente
import com.sigra.app.ui.components.rememberAlertTime
import com.sigra.app.util.remainingSeconds
import com.sigra.app.util.arrivalLabel
import com.sigra.app.util.confidenceDescription
import com.sigra.app.util.Geo

@Composable
fun AlertFullScreen(alerta: SeismicAlert, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val now = rememberAlertTime(alerta.uuid)
    val restante = alerta.remainingSeconds(now)

    val fondo = Brush.verticalGradient(
        listOf(
            Color(0xFFB91C1C),
            Color(0xFFEA580C),
            Color(0xFF7C2D12),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fondo)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                if (alerta.source == "simulation") "PRUEBA DE ALERTA" else "SISMO ESTIMADO",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFFFEDD5),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                Geo.formatoMagnitud(alerta.magnitude),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (restante != null && restante > 0) "$restante" else "—",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 64.sp),
                color = Color.White,
                fontWeight = FontWeight.Black,
            )
            Text(
                alerta.arrivalLabel(now),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFEDD5),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "${Geo.formatoDistancia(alerta.distance_km)} · ${alerta.confidenceDescription()}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFEDD5),
            )
            Text(
                "Fuente: ${fuente(alerta.source)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFEDD5),
            )
            Spacer(Modifier.height(26.dp))

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Instruccion("AGÁCHATE", "Al suelo, sobre manos y rodillas.", Modifier.fillMaxWidth())
                Instruccion("CÚBRETE", "Cabeza y cuello bajo una mesa.", Modifier.fillMaxWidth())
                Instruccion("SUJÉTATE", "Espera a que termine el movimiento.", Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Cerrar alerta", color = Color(0xFF7C2D12), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Estimación experimental. Sigue las instrucciones de las autoridades.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFEDD5),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Instruccion(titulo: String, texto: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.12f),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                titulo,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                texto,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFEDD5),
                textAlign = TextAlign.Center,
            )
        }
    }
}
