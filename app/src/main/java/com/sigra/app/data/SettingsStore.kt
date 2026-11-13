package com.sigra.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sigraDataStore: DataStore<Preferences> by preferencesDataStore(name = "sigra_settings")

data class UserSettings(
    val deviceUuid: String = "",
    val deviceToken: String? = null,
    val onboardingCompleted: Boolean = false,
    val monitoringEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val alertRadiusKm: Int = 250,
    val minMagnitude: Float = 3.0f,
    val lastLat: Double? = null,
    val lastLng: Double? = null,
    val fcmToken: String? = null,
    val lastAlertUuid: String? = null,
    val lastAlertJson: String? = null,
    val seenAlerts: Set<String> = emptySet(),
    val quakeFilters: QuakeFilters = QuakeFilters(),
    val appearance: AppAppearance = AppAppearance.DARK,
    val monitoringIssue: String? = null,
    val alertInbox: AlertInbox = AlertInbox(),
) {
    val hasLocation: Boolean
        get() = lastLat != null && lastLng != null && lastLat.isFinite() && lastLng.isFinite() &&
            lastLat in -90.0..90.0 && lastLng in -180.0..180.0
}

class SettingsStore(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.sigraDataStore)

    private object Keys {
        val deviceUuid = stringPreferencesKey("device_uuid")
        val deviceToken = stringPreferencesKey("device_token")
        val onboarding = booleanPreferencesKey("onboarding_completed")
        val monitoring = booleanPreferencesKey("monitoring_enabled")
        val notifications = booleanPreferencesKey("notifications_enabled")
        val alertRadius = intPreferencesKey("alert_radius_km")
        val minMagnitude = floatPreferencesKey("min_magnitude")
        val lastLat = doublePreferencesKey("last_lat")
        val lastLng = doublePreferencesKey("last_lng")
        val fcmToken = stringPreferencesKey("fcm_token")
        val lastAlertUuid = stringPreferencesKey("last_alert_uuid")
        val lastAlertJson = stringPreferencesKey("last_alert_json")
        val seenAlerts = stringSetPreferencesKey("seen_alerts")
        val mapRadius = intPreferencesKey("map_radius_km")
        val mapSource = stringPreferencesKey("map_source")
        val mapHours = intPreferencesKey("map_hours")
        val mapMinMagnitude = doublePreferencesKey("map_min_magnitude")
        val mapOrder = stringPreferencesKey("map_order")
        val appearance = stringPreferencesKey("appearance")
        val monitoringIssue = stringPreferencesKey("monitoring_issue")
        val alertInbox = stringPreferencesKey("alert_inbox_v1")
    }

    val settings: Flow<UserSettings> = dataStore.data.map(::readSettings)

    private fun readInbox(prefs: Preferences): AlertInbox = AlertInboxCodec.decode(
        prefs[Keys.alertInbox], prefs[Keys.lastAlertJson], prefs[Keys.seenAlerts] ?: emptySet(),
    )

    private fun readSettings(prefs: Preferences): UserSettings {
        val inbox = readInbox(prefs)
        return UserSettings(
            deviceUuid = prefs[Keys.deviceUuid].orEmpty(),
            deviceToken = prefs[Keys.deviceToken]?.takeIf { it.isNotBlank() },
            onboardingCompleted = prefs[Keys.onboarding] ?: false,
            monitoringEnabled = prefs[Keys.monitoring] ?: false,
            notificationsEnabled = prefs[Keys.notifications] ?: true,
            alertRadiusKm = prefs[Keys.alertRadius] ?: 250,
            minMagnitude = prefs[Keys.minMagnitude] ?: 3.0f,
            lastLat = prefs[Keys.lastLat],
            lastLng = prefs[Keys.lastLng],
            fcmToken = prefs[Keys.fcmToken]?.takeIf { it.isNotBlank() },
            lastAlertUuid = prefs[Keys.lastAlertUuid],
            lastAlertJson = prefs[Keys.lastAlertJson],
            seenAlerts = inbox.processedIds.toSet(),
            quakeFilters = QuakeFilters.fromStoredValues(
                prefs[Keys.mapRadius], prefs[Keys.mapSource], prefs[Keys.mapOrder], prefs[Keys.mapHours], prefs[Keys.mapMinMagnitude],
            ),
            appearance = AppAppearance.entries.firstOrNull { it.name == prefs[Keys.appearance] }
                ?: AppAppearance.DARK,
            monitoringIssue = prefs[Keys.monitoringIssue],
            alertInbox = inbox,
        )
    }

    suspend fun snapshot(): UserSettings = settings.first()

    suspend fun setAppearance(appearance: AppAppearance) {
        dataStore.edit { it[Keys.appearance] = appearance.name }
    }

    suspend fun setQuakeFilters(filters: QuakeFilters) {
        dataStore.edit { prefs ->
            prefs[Keys.mapRadius] = filters.radiusKm
            val source = filters.source
            if (source == null) prefs.remove(Keys.mapSource) else prefs[Keys.mapSource] = source
            prefs[Keys.mapOrder] = filters.order.apiValue
            prefs[Keys.mapHours] = filters.hours
            filters.minMagnitude?.let { prefs[Keys.mapMinMagnitude] = it } ?: prefs.remove(Keys.mapMinMagnitude)
        }
    }

    suspend fun ensureDeviceUuid(): String {
        var uuid = ""
        dataStore.edit { prefs ->
            uuid = prefs[Keys.deviceUuid]?.takeIf { it.isNotBlank() } ?: DeviceInfo.nuevoUuid()
            prefs[Keys.deviceUuid] = uuid
        }
        return uuid
    }

    suspend fun saveRegistration(token: String, expectedUuid: String): Boolean {
        var saved = false
        dataStore.edit { prefs ->
            if (prefs[Keys.deviceUuid] != expectedUuid || prefs[Keys.monitoring] != true) return@edit
            prefs[Keys.deviceToken] = token
            saved = true
        }
        return saved
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        dataStore.edit { it[Keys.onboarding] = completed }
    }

    suspend fun setMonitoring(enabled: Boolean) {
        dataStore.edit {
            it[Keys.monitoring] = enabled
            it.remove(Keys.monitoringIssue)
        }
    }

    suspend fun stopMonitoringWithIssue(message: String) {
        dataStore.edit {
            it[Keys.monitoring] = false
            it[Keys.monitoringIssue] = message
        }
    }

    suspend fun setNotifications(enabled: Boolean) {
        dataStore.edit { it[Keys.notifications] = enabled }
    }

    suspend fun setAlertRadius(km: Int) {
        dataStore.edit { it[Keys.alertRadius] = km.coerceIn(25, 1000) }
    }

    suspend fun setMinMagnitude(magnitude: Float) {
        dataStore.edit { it[Keys.minMagnitude] = magnitude.coerceIn(1.5f, 8f) }
    }

    suspend fun saveLocation(lat: Double, lng: Double) {
        dataStore.edit { prefs ->
            prefs[Keys.lastLat] = lat
            prefs[Keys.lastLng] = lng
        }
    }

    suspend fun saveFcmToken(token: String) {
        dataStore.edit { it[Keys.fcmToken] = token }
    }

    /** Claim and persist before notifying. Concurrent receivers can only claim a UUID once. */
    suspend fun receiveAlert(
        alert: SeismicAlert,
        channel: AlertChannel,
        expectedUuid: String? = null,
        nowMs: Long = System.currentTimeMillis(),
    ): AlertReceipt? {
        if (!alert.isValidForInbox()) return null
        var receipt: AlertReceipt? = null
        dataStore.edit { prefs ->
            val actual = readSettings(prefs)
            if (expectedUuid != null && actual.deviceUuid != expectedUuid) return@edit
            if (channel == AlertChannel.PUSH && actual.deviceToken == null) return@edit
            val update = actual.alertInbox.receive(alert, channel, nowMs)
            if (update.inbox != actual.alertInbox) writeInbox(prefs, update.inbox)
            receipt = AlertReceipt(alert, actual, update.firstReceipt)
        }
        return receipt
    }

    suspend fun markAlertRead(uuid: String) {
        dataStore.edit { prefs ->
            val inbox = readInbox(prefs)
            val updated = inbox.markRead(uuid)
            if (inbox != updated) writeInbox(prefs, updated)
        }
    }

    suspend fun markAllAlertsRead() {
        dataStore.edit { prefs ->
            val inbox = readInbox(prefs)
            if (inbox.unreadCount > 0) writeInbox(prefs, inbox.markAllRead())
        }
    }

    suspend fun clearAlertHistory() {
        dataStore.edit { prefs -> writeInbox(prefs, readInbox(prefs).clearHistory()) }
    }

    private fun writeInbox(prefs: MutablePreferences, inbox: AlertInbox) {
        prefs[Keys.alertInbox] = AlertInboxCodec.encode(inbox)
        prefs[Keys.seenAlerts] = inbox.processedIds.toSet()
        val latest = inbox.entries.firstOrNull()?.alert
        if (latest == null) {
            prefs.remove(Keys.lastAlertUuid)
            prefs.remove(Keys.lastAlertJson)
        } else {
            prefs[Keys.lastAlertUuid] = latest.uuid
            prefs[Keys.lastAlertJson] = AlertInboxCodec.encodeAlert(latest)
        }
    }

    suspend fun clearRegistration() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.deviceToken)
            prefs[Keys.deviceUuid] = DeviceInfo.nuevoUuid()
            prefs[Keys.monitoring] = false
            prefs.remove(Keys.monitoringIssue)
            // Keep the inbox and deduplication history; leaving only removes the network identity.
        }
    }
}

data class AlertReceipt(val alert: SeismicAlert, val settings: UserSettings, val firstReceipt: Boolean)
