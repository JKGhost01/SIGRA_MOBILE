package com.sigra.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sigra.app.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendiente = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val store = SettingsStore(context)
                val settings = store.snapshot()
                if (settings.onboardingCompleted && settings.monitoringEnabled) {
                    // This app does not request background location. Modern Android requires a visible start.
                    if (Build.VERSION.SDK_INT >= 34) {
                        store.stopMonitoringWithIssue("Después de reiniciar el teléfono, abre SIGRA y activa el monitoreo de nuevo.")
                    } else {
                        SeismicMonitorService.start(context)
                    }
                }
            } catch (_: Exception) {
                // El sistema puede restringir el arranque en segundo plano.
            } finally {
                pendiente.finish()
            }
        }
    }
}
