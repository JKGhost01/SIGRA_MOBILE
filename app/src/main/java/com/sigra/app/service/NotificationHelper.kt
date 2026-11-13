package com.sigra.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sigra.app.MainActivity
import com.sigra.app.R
import com.sigra.app.data.ApiClient
import com.sigra.app.data.SeismicAlert
import com.sigra.app.data.UserSettings
import kotlinx.serialization.encodeToString
import com.sigra.app.util.matchesNotificationPreferences
import com.sigra.app.util.remainingSeconds
import com.sigra.app.util.alertSourceLabel
import com.sigra.app.util.confidenceDescription

object NotificationHelper {
    const val CHANNEL_MONITORING = "sigra_monitoreo"
    const val CHANNEL_ALERTS = "sigra_alertas"
    const val MONITORING_ID = 1001
    const val EXTRA_ALERT_JSON = "sigra_alert_json"
    private const val ALERT_ID = 2001
    private fun alertTag(uuid: String) = "sigra.alert.$uuid"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val monitoreo = NotificationChannel(
            CHANNEL_MONITORING,
            context.getString(R.string.channel_monitoring),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Servicio en primer plano que escucha el acelerómetro."
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val alertas = NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.channel_alerts),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alertas de sismos detectados por la red SIGRA."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 350, 200, 350, 200, 800)
            enableLights(true)
            lightColor = 0xFFE55C4D.toInt()
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(false)
            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }

        manager.createNotificationChannel(monitoreo)
        manager.createNotificationChannel(alertas)
    }

    fun monitoringNotification(context: Context, texto: String = context.getString(R.string.notification_monitoring_text)): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_MONITORING)
            .setSmallIcon(R.drawable.ic_stat_sigra)
            .setContentTitle(context.getString(R.string.notification_monitoring_title))
            .setContentText(texto)
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    internal fun showAlert(context: Context, alert: SeismicAlert, settings: UserSettings) {
        if (!alert.matchesNotificationPreferences(settings)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val json = try {
            ApiClient.json.encodeToString(alert)
        } catch (_: Exception) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse("sigra://alert/${Uri.encode(alert.uuid)}")
            putExtra(EXTRA_ALERT_JSON, json)
        }
        val pending = PendingIntent.getActivity(
            context,
            alert.uuid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val eta = alert.remainingSeconds()
        val titulo = when {
            eta == null -> "SISMO ESTIMADO ${"%.1f".format(alert.magnitude)}"
            eta > 0 -> "SISMO M${"%.1f".format(alert.magnitude)} · ${eta.toInt()} s"
            else -> "SISMO ESTIMADO M${"%.1f".format(alert.magnitude)}"
        }

        val distancia = alert.distance_km?.let { if (it < 10) "%.1f km".format(it) else "${it.toInt()} km" }
        val cuerpo = buildString {
            if (distancia != null) append("A $distancia. ")
            append("Agáchate, cúbrete y sujétate. Aléjate de ventanas.")
        }

        val textoLargo = buildString {
            append(cuerpo)
            append("\n\n")
            append("Fuente: ")
            append(alertSourceLabel(alert.source))
            append(" · ")
            append(alert.confidenceDescription())
            if (alert.device_count > 0) {
                append(" · ")
                append(alert.device_count)
                append(" teléfonos")
            }
            append("\nEstimación experimental. Sigue las instrucciones oficiales.")
        }

        val notificacion = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_sigra)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(textoLargo))
            .setContentIntent(pending)
            .setFullScreenIntent(pending, true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 350, 200, 350, 200, 800))
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
            .setTimeoutAfter(5 * 60_000L)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(alertTag(alert.uuid), ALERT_ID, notificacion)
        } catch (_: SecurityException) {
            // Sin permiso de notificaciones: la app mostrará la alerta al abrirse.
        }
    }

    fun cancelAlert(context: Context, uuid: String) {
        NotificationManagerCompat.from(context).cancel(alertTag(uuid), ALERT_ID)
    }
}
