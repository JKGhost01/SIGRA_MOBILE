package com.sigra.app.service

import android.content.Context
import com.sigra.app.data.AlertChannel
import com.sigra.app.data.SeismicAlert
import com.sigra.app.data.SettingsStore

object AlertReceiver {
    suspend fun receive(
        context: Context,
        store: SettingsStore,
        alert: SeismicAlert,
        channel: AlertChannel,
        expectedUuid: String? = null,
        notify: Boolean = true,
    ) {
        val receipt = store.receiveAlert(alert, channel, expectedUuid) ?: return
        if (receipt.firstReceipt && notify) {
            val current = store.snapshot()
            if (current.deviceUuid != receipt.settings.deviceUuid) return
            val saved = current.alertInbox.entries.firstOrNull { it.alert.uuid == alert.uuid } ?: return
            if (saved.read) return
            // Only the winner of the persisted claim can exclude its own claim from the preference check.
            NotificationHelper.showAlert(context, saved.alert, current.copy(seenAlerts = current.seenAlerts - alert.uuid))
        }
    }

    suspend fun receiveAll(
        context: Context,
        store: SettingsStore,
        alerts: List<SeismicAlert>,
        channel: AlertChannel,
        expectedUuid: String,
    ) {
        // Older entries first, so the newest event becomes the most recently received item.
        alerts.sortedBy { if (it.origin_time_ms > 0) it.origin_time_ms else it.receivedAtMs }
            .forEach { receive(context, store, it, channel, expectedUuid) }
    }
}
