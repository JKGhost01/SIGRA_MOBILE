package com.sigra.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sigra.app.ui.components.Panel
import com.sigra.app.ui.theme.Coral
import com.sigra.app.ui.theme.Teal

@Composable
fun OnboardingScreen(onActivate: () -> Unit, onExplore: () -> Unit) {
    var acepto by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Teal.copy(alpha = 0.12f),
                    modifier = Modifier.size(84.dp),
                ) {
                    Icon(
                        Icons.Default.Waves,
                        contentDescription = null,
                        tint = Teal,
                        modifier = Modifier.padding(20.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "SIGRA",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Tu teléfono, un sensor sísmico",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Explora sismos oficiales y participa en una red experimental que busca detectar movimiento y estimar la llegada de ondas sísmicas.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Panel {
                Text("Cómo participar", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Beneficio("1", "Activa el monitoreo", "El teléfono escucha el acelerómetro en segundo plano.")
                Beneficio("2", "Aporta detecciones", "Si reconoce una onda P, envía hora y ubicación al servidor.")
                Beneficio("3", "Recibe alertas", "La red triangula y te avisa con los segundos hasta la onda S.")
            }
        }

        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Teal, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Permisos que pediremos", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ubicación para calcular distancias y notificaciones para avisarte. El monitoreo muestra una notificación permanente. Puedes explorar los sismos sin estos permisos y sin crear una cuenta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Coral.copy(alpha = 0.10f),
            ) {
                Text(
                    "SIGRA es experimental. No reemplaza al SGC ni a los sistemas oficiales. Si un sismo es fuerte, agáchate, cúbrete y sujétate; no esperes la notificación.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = acepto, onCheckedChange = { acepto = it })
                Text(
                    "Entiendo que SIGRA es una estimación experimental y acepto participar en la red.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        item {
            Button(
                onClick = onActivate,
                enabled = acepto,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Activar monitoreo y unirme", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onExplore, modifier = Modifier.fillMaxWidth()) {
                Text("Explorar sin activar monitoreo")
            }
            Text(
                "Al continuar se solicitarán los permisos. Puedes detener el monitoreo cuando quieras.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Beneficio(numero: String, titulo: String, texto: String) {
    Row(modifier = Modifier.padding(vertical = 5.dp)) {
        Surface(
            shape = CircleShape,
            color = Teal,
            modifier = Modifier.size(28.dp),
        ) {
            Text(
                numero,
                modifier = Modifier.padding(top = 5.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.size(10.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.labelLarge)
            Text(
                texto,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
