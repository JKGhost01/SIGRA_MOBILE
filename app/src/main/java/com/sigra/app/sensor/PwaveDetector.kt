package com.sigra.app.sensor

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class PwaveDetection(
    val timestampMs: Long,
    val peakAccelMps2: Double,
    val staLtaRatio: Double,
    val dominantFreqHz: Double,
    val durationMs: Int,
    val confidence: Double,
)

/**
 * Detector de onda P para el acelerómetro de un teléfono.
 *
 * No pretende ser un sismógrafo: aplica un filtro pasa-banda sencillo,
 * calcula la relación STA/LTA y exige varios muestreos consecutivos por
 * encima del umbral. El servidor confirma el evento con otros teléfonos.
 */
class PwaveDetector(
    private val onDetection: (PwaveDetection) -> Unit,
) {
    private val gravedad = FloatArray(3)
    private var gravedadInicializada = false

    private var anteriorEntrada = 0.0
    private var anteriorSalida = 0.0
    private var pasaBajos = 0.0

    private var ultimoTimestampNs = 0L
    private var sta = 0.0
    private var lta = 0.0
    private var muestras = 0
    private var muestrasSobreUmbral = 0
    private var ultimoDisparoMs = 0L

    private val picos = ArrayDeque<ParPico>()
    private val cruces = ArrayDeque<ParCruce>()

    fun reset() {
        gravedad.fill(0f)
        gravedadInicializada = false
        anteriorEntrada = 0.0
        anteriorSalida = 0.0
        pasaBajos = 0.0
        ultimoTimestampNs = 0L
        sta = 0.0
        lta = 0.0
        muestras = 0
        muestrasSobreUmbral = 0
        ultimoDisparoMs = 0L
        picos.clear()
        cruces.clear()
    }

    fun process(x: Float, y: Float, z: Float, timestampNs: Long, usaAceleracionLineal: Boolean) {
        if (ultimoTimestampNs == 0L) {
            ultimoTimestampNs = timestampNs
            return
        }

        val dt = ((timestampNs - ultimoTimestampNs) / 1_000_000_000.0).coerceIn(0.002, 0.1)
        ultimoTimestampNs = timestampNs
        val ahoraMs = timestampNs / 1_000_000

        val ax: Double
        val ay: Double
        val az: Double
        if (usaAceleracionLineal) {
            ax = x.toDouble()
            ay = y.toDouble()
            az = z.toDouble()
        } else {
            if (!gravedadInicializada) {
                gravedad[0] = x
                gravedad[1] = y
                gravedad[2] = z
                gravedadInicializada = true
                return
            }
            val alpha = 0.95f
            gravedad[0] = alpha * gravedad[0] + (1 - alpha) * x
            gravedad[1] = alpha * gravedad[1] + (1 - alpha) * y
            gravedad[2] = alpha * gravedad[2] + (1 - alpha) * z
            ax = (x - gravedad[0]).toDouble()
            ay = (y - gravedad[1]).toDouble()
            az = (z - gravedad[2]).toDouble()
        }

        val magnitud = ax * ax + ay * ay + az * az
        if (magnitud <= 0.0) return

        // Pasa-altos ~0.5 Hz para quitar deriva y gravedad residual.
        val rcAlto = 1.0 / (2.0 * Math.PI * 0.5)
        val alphaAlto = rcAlto / (rcAlto + dt)
        val salidaAlto = alphaAlto * (anteriorSalida + ax - anteriorEntrada)
        anteriorEntrada = ax
        anteriorSalida = salidaAlto

        // Pasa-bajos ~10 Hz para quedarnos con la banda sísmica útil.
        val rcBajo = 1.0 / (2.0 * Math.PI * 10.0)
        val alphaBajo = dt / (rcBajo + dt)
        pasaBajos += alphaBajo * (salidaAlto - pasaBajos)

        val senal = abs(pasaBajos)

        sta += (senal - sta) * min(1.0, dt / 0.5)
        lta += (senal - lta) * min(1.0, dt / 8.0)

        muestras++
        picos.addLast(ParPico(ahoraMs, senal))
        while (picos.isNotEmpty() && picos.first().ms < ahoraMs - 1_200) {
            picos.removeFirst()
        }
        cruces.addLast(ParCruce(ahoraMs, pasaBajos))
        while (cruces.isNotEmpty() && cruces.first().ms < ahoraMs - 700) {
            cruces.removeFirst()
        }

        if (muestras < 100) return

        val pico = picos.maxOfOrNull { it.valor } ?: senal
        val relacion = sta / max(lta, 1e-5)

        // Movimiento continuo del teléfono: no dispares.
        if (lta > 0.45) {
            muestrasSobreUmbral = 0
            return
        }

        val supera = relacion >= 3.2 && sta >= 0.030 && pico >= 0.05
        if (supera) {
            muestrasSobreUmbral++
        } else {
            muestrasSobreUmbral = 0
        }

        if (muestrasSobreUmbral < 3 || (ahoraMs - ultimoDisparoMs) < 20_000) {
            return
        }

        val frecuencia = estimarFrecuencia(ahoraMs)
        val confianza = (0.35 + (relacion - 3.0) * 0.05 + pico * 0.35).coerceIn(0.35, 0.95)
        val duracion = (300 + relacion * 20).toInt().coerceIn(300, 900)

        ultimoDisparoMs = ahoraMs
        muestrasSobreUmbral = 0

        onDetection(
            PwaveDetection(
                timestampMs = System.currentTimeMillis(),
                peakAccelMps2 = pico,
                staLtaRatio = relacion,
                dominantFreqHz = frecuencia,
                durationMs = duracion,
                confidence = confianza,
            ),
        )
    }

    private fun estimarFrecuencia(ahoraMs: Long): Double {
        val ventana = cruces.filter { it.ms >= ahoraMs - 700 }
        if (ventana.size < 4) return 2.0

        var crucesCero = 0
        var anterior = ventana.first().valor
        for (i in 1 until ventana.size) {
            val actual = ventana[i].valor
            if ((anterior >= 0 && actual < 0) || (anterior < 0 && actual >= 0)) {
                crucesCero++
            }
            anterior = actual
        }

        val duracion = (ventana.last().ms - ventana.first().ms) / 1000.0
        if (duracion <= 0.0) return 2.0

        return (crucesCero / (2.0 * duracion)).coerceIn(0.5, 15.0)
    }

    private data class ParPico(val ms: Long, val valor: Double)
    private data class ParCruce(val ms: Long, val valor: Double)
}
