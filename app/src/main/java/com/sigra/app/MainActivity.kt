package com.sigra.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sigra.app.data.AppAppearance
import com.sigra.app.data.ApiClient
import com.sigra.app.data.PushAlertDecoder
import com.sigra.app.data.SeismicAlert
import com.sigra.app.viewmodel.MainViewModel
import com.sigra.app.service.NotificationHelper
import com.sigra.app.ui.SigraRoot
import com.sigra.app.ui.theme.SigraTheme

class MainActivity : ComponentActivity() {
    private var alertJson by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState != null) {
            alertJson = savedInstanceState.getString(STATE_ALERT_JSON)
        } else {
            handleAlertIntent(intent)
        }

        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.appearance) {
                AppAppearance.SYSTEM -> isSystemInDarkTheme()
                AppAppearance.DARK -> true
                AppAppearance.LIGHT -> false
            }
            SigraTheme(darkTheme = darkTheme) {
                SigraRoot(
                    alertJson = alertJson,
                    onDismissAlert = { alertJson = null },
                    vm = vm,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAlertIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_ALERT_JSON, alertJson)
        super.onSaveInstanceState(outState)
    }

    @Suppress("DEPRECATION")
    private fun handleAlertIntent(intent: Intent?) {
        intent?.getStringExtra(NotificationHelper.EXTRA_ALERT_JSON)?.let {
            alertJson = it
            return
        }
        // Notification+data FCM messages reach the launcher extras when opened from the system tray.
        val extras = intent?.extras ?: return
        val data = extras.keySet().mapNotNull { key -> (extras.get(key) as? String)?.let { key to it } }.toMap()
        val sentAt = extras.get("google.sent_time")?.toString()?.toLongOrNull() ?: return
        val alert = PushAlertDecoder.decode(data, sentAt, System.currentTimeMillis()) ?: return
        alertJson = ApiClient.json.encodeToString(SeismicAlert.serializer(), alert)
    }

    companion object {
        private const val STATE_ALERT_JSON = "sigra.open_alert_json"
    }
}
