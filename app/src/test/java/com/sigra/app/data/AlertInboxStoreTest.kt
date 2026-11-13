package com.sigra.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.rules.TemporaryFolder
import java.io.File

class AlertInboxStoreTest {
    @get:Rule val directory = TemporaryFolder()
    private lateinit var worker: Job
    private lateinit var file: File
    private lateinit var store: SettingsStore
    private val alert = SeismicAlert(uuid = "event", magnitude = 4.0, distance_km = 10.0, receivedAtMs = 1000)

    private fun openStore() {
        worker = SupervisorJob()
        store = SettingsStore(PreferenceDataStoreFactory.create(
            scope = CoroutineScope(worker + Dispatchers.IO), produceFile = { file },
        ))
    }

    @Before fun setUp() {
        file = File(directory.root, "settings.preferences_pb")
        openStore()
    }

    @After fun closeStore() = runBlocking { worker.cancelAndJoin() }

    @Test fun simultaneousPushAndPollingOnlyClaimOnce() = runBlocking {
        val uuid = store.ensureDeviceUuid()
        store.setMonitoring(true)
        assertTrue(store.saveRegistration("token", uuid))
        val receipts = (0 until 32).map { index ->
            async(Dispatchers.Default) {
                store.receiveAlert(alert, if (index % 2 == 0) AlertChannel.PUSH else AlertChannel.POLLING, uuid)
            }
        }.awaitAll()
        assertEquals(1, receipts.count { it?.firstReceipt == true })
        assertEquals(1, store.snapshot().alertInbox.entries.size)
        assertTrue(alert.uuid in store.snapshot().seenAlerts)
        assertFalse(alert.uuid in receipts.first { it?.firstReceipt == true }!!.settings.seenAlerts)
    }

    @Test fun simultaneousDifferentAlertsDoNotLoseEntries() = runBlocking {
        (0 until 24).map { index ->
            async(Dispatchers.Default) { store.receiveAlert(alert.copy(uuid = "event-$index"), AlertChannel.FOREGROUND) }
        }.awaitAll()
        assertEquals(24, store.snapshot().alertInbox.entries.size)
        assertEquals(24, store.snapshot().alertInbox.unreadCount)
    }

    @Test fun diskReopenKeepsHistoryReadStateAndDuplicateClaim() = runBlocking {
        store.receiveAlert(alert, AlertChannel.POLLING, nowMs = 2000)
        store.markAlertRead(alert.uuid)
        worker.cancelAndJoin()
        openStore()
        val restored = store.snapshot().alertInbox
        assertEquals(1, restored.entries.size)
        assertEquals(0, restored.unreadCount)
        assertEquals(1000L, restored.entries.single().alert.receivedAtMs)
        assertFalse(store.receiveAlert(alert, AlertChannel.POLLING)!!.firstReceipt)
    }

    @Test fun clearingHistoryDoesNotReplayTheSameServerAlert() = runBlocking {
        store.receiveAlert(alert, AlertChannel.POLLING)
        store.clearAlertHistory()
        assertFalse(store.receiveAlert(alert, AlertChannel.POLLING)!!.firstReceipt)
        assertTrue(store.snapshot().alertInbox.entries.isEmpty())
        assertNull(store.snapshot().lastAlertJson)
    }

    @Test fun obsoleteIdentityCannotStoreALateResponse() = runBlocking {
        val oldUuid = store.ensureDeviceUuid()
        store.clearRegistration()
        assertNull(store.receiveAlert(alert, AlertChannel.POLLING, expectedUuid = oldUuid))
        assertTrue(store.snapshot().alertInbox.entries.isEmpty())
    }

    @Test fun pushWithoutRegistrationIsIgnored() = runBlocking {
        assertNull(store.receiveAlert(alert, AlertChannel.PUSH))
        assertTrue(store.snapshot().alertInbox.entries.isEmpty())
    }

    @Test fun leavingNetworkKeepsHistoryAndReadState() = runBlocking {
        store.receiveAlert(alert, AlertChannel.POLLING)
        store.markAllAlertsRead()
        store.clearRegistration()
        val actual = store.snapshot()
        assertEquals(1, actual.alertInbox.entries.size)
        assertEquals(0, actual.alertInbox.unreadCount)
        assertTrue(alert.uuid in actual.seenAlerts)
        assertNull(actual.deviceToken)
        assertFalse(actual.monitoringEnabled)
    }
    @Test fun catalogPreferencesPersistAllNewFiltersOnDisk() = runBlocking {
        val filters = QuakeFilters(500, "INGV", QuakeOrder.MAGNITUDE, 168, 3.0)
        store.setQuakeFilters(filters)
        worker.cancelAndJoin()
        openStore()
        assertEquals(filters, store.snapshot().quakeFilters)
        store.setQuakeFilters(QuakeFilters())
        assertNull(store.snapshot().quakeFilters.minMagnitude)
        assertEquals(120, store.snapshot().quakeFilters.hours)
    }

}
