package com.sigra.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
enum class AlertChannel { PUSH, POLLING, DETECTION, FOREGROUND, OPENED, LEGACY }

@Serializable
data class StoredAlert(
    val alert: SeismicAlert,
    val firstReceivedAtMs: Long,
    val read: Boolean = false,
    val channel: AlertChannel,
)

@Serializable
data class AlertInbox(
    val entries: List<StoredAlert> = emptyList(),
    val processedIds: List<String> = emptyList(),
) {
    val unreadCount: Int get() = entries.count { !it.read }

    fun receive(alert: SeismicAlert, channel: AlertChannel, nowMs: Long): InboxUpdate {
        if (!alert.isValidForInbox()) return InboxUpdate(this, false)
        val previous = entries.firstOrNull { it.alert.uuid == alert.uuid }
        val firstReceipt = alert.uuid !in processedIds && previous == null
        // Clearing the visible history must not make the same server response reappear.
        if (previous == null && !firstReceipt) return InboxUpdate(this, false)
        val entry = when {
            previous == null -> StoredAlert(alert, nowMs, channel = channel)
            channel == AlertChannel.PUSH || channel == AlertChannel.OPENED -> previous
            else -> previous.copy(alert = alert, channel = channel)
        }
        return InboxUpdate(
            copy(
                entries = (entries.filterNot { it.alert.uuid == alert.uuid } + entry)
                    .sortedWith(compareByDescending<StoredAlert> { it.firstReceivedAtMs }
                        .thenByDescending { it.alert.origin_time_ms }.thenBy { it.alert.uuid }).take(MAX_ENTRIES),
                processedIds = (processedIds.filterNot { it == alert.uuid } + alert.uuid).takeLast(MAX_PROCESSED),
            ),
            firstReceipt,
        )
    }

    fun markRead(uuid: String): AlertInbox = copy(entries = entries.map {
        if (it.alert.uuid == uuid && !it.read) it.copy(read = true) else it
    })

    fun markAllRead(): AlertInbox = copy(entries = entries.map { if (it.read) it else it.copy(read = true) })

    fun clearHistory(): AlertInbox = copy(entries = emptyList())

    companion object {
        const val MAX_ENTRIES = 50
        const val MAX_PROCESSED = 500
    }
}

data class InboxUpdate(val inbox: AlertInbox, val firstReceipt: Boolean)

fun SeismicAlert.isValidForInbox(): Boolean = uuid.isNotBlank() && uuid.length <= 200 &&
    magnitude.isFinite() && magnitude in -2.0..10.0 && latitude.isFinite() && longitude.isFinite() &&
    latitude in -90.0..90.0 && longitude in -180.0..180.0

object AlertInboxCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(inbox: AlertInbox): String = json.encodeToString(inbox)

    fun encodeAlert(alert: SeismicAlert): String = json.encodeToString(alert)

    fun decode(value: String?, legacyJson: String?, legacyIds: Set<String>): AlertInbox {
        if (value != null) {
            runCatching { json.decodeFromString<AlertInbox>(value) }.getOrNull()?.let { decoded ->
                return decoded.copy(
                    entries = decoded.entries.filter { it.alert.isValidForInbox() }
                        .distinctBy { it.alert.uuid }.take(AlertInbox.MAX_ENTRIES),
                    processedIds = (decoded.processedIds + legacyIds).distinct().takeLast(AlertInbox.MAX_PROCESSED),
                )
            }
        }
        val legacy = legacyJson?.takeIf { it.isNotBlank() }?.let { valueJson ->
            runCatching {
                val alert = json.decodeFromString<SeismicAlert>(valueJson)
                if ("_received_at_ms" in Json.parseToJsonElement(valueJson).jsonObject) alert
                else alert.copy(seconds_to_s_wave = null, receivedAtMs = 0)
            }.getOrNull()?.takeIf { it.isValidForInbox() }
        }
        return AlertInbox(
            entries = legacy?.let { listOf(StoredAlert(it, it.receivedAtMs, read = true, channel = AlertChannel.LEGACY)) }
                ?: emptyList(),
            processedIds = (legacyIds + listOfNotNull(legacy?.uuid)).toList().takeLast(AlertInbox.MAX_PROCESSED),
        )
    }
}
