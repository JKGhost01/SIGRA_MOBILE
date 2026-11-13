package com.sigra.app.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sigra.app.data.AlertChannel
import com.sigra.app.data.PushAlertDecoder
import com.sigra.app.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/** Firebase callbacks run off the UI thread. Finish bounded local work before returning. */
class SigraMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        persistLocally { it.saveFcmToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val alert = PushAlertDecoder.decode(message.data, message.sentTime, System.currentTimeMillis()) ?: return
        persistLocally { store -> AlertReceiver.receive(applicationContext, store, alert, AlertChannel.PUSH) }
    }

    private fun persistLocally(block: suspend (SettingsStore) -> Unit) {
        try {
            runBlocking(Dispatchers.IO) {
                withTimeout(4_000) { block(SettingsStore(applicationContext)) }
            }
        } catch (_: Exception) {
            // No network work here. Polling can recover received alerts if local delivery failed.
        }
    }
}
