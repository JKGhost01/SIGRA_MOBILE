package com.sigra.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.sigra.app.data.SettingsStore
import com.sigra.app.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class SigraApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)

        Configuration.getInstance().apply {
            load(this@SigraApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = "$packageName/${BuildConfig.VERSION_NAME}"
        }

        inicializarFirebase()
    }

    private fun inicializarFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                scope.launch {
                    SettingsStore(this@SigraApp).saveFcmToken(token)
                }
            }
        } catch (_: Throwable) {
            // Firebase es opcional: sin google-services.json se usa polling.
        }
    }
}
