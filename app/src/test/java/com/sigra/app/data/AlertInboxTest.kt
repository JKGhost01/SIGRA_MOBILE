package com.sigra.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertInboxTest {
    private val alert = SeismicAlert(uuid = "event", magnitude = 4.0, seconds_to_s_wave = 20.0, receivedAtMs = 1000)

    @Test fun firstReceiptIsUnreadAndClaimedOnce() {
        val first = AlertInbox().receive(alert, AlertChannel.PUSH, 2000)
        assertTrue(first.firstReceipt)
        assertEquals(1, first.inbox.unreadCount)
        assertFalse(first.inbox.receive(alert, AlertChannel.POLLING, 3000).firstReceipt)
    }

    @Test fun duplicatePushDoesNotRestartCountdownOrUnreadState() {
        val first = AlertInbox().receive(alert, AlertChannel.PUSH, 2000).inbox.markRead(alert.uuid)
        val duplicate = first.receive(alert.copy(receivedAtMs = 9000), AlertChannel.PUSH, 9000)
        assertEquals(first.entries, duplicate.inbox.entries)
        assertEquals(1000L, duplicate.inbox.entries.single().alert.receivedAtMs)
        assertEquals(0, duplicate.inbox.unreadCount)
    }

    @Test fun serverResponseEnrichesPushWithoutClaimingAgain() {
        val first = AlertInbox().receive(alert.copy(source = "unknown"), AlertChannel.PUSH, 2000).inbox
        val full = alert.copy(source = "sgc", origin_time_ms = 500, confidence = 0.9, device_count = 4)
        val update = first.receive(full, AlertChannel.POLLING, 3000)
        assertFalse(update.firstReceipt)
        assertEquals(full, update.inbox.entries.single().alert)
        assertEquals(2000L, update.inbox.entries.single().firstReceivedAtMs)
        assertEquals(full, update.inbox.receive(alert, AlertChannel.PUSH, 4000).inbox.entries.single().alert)
    }

    @Test fun openingACachedNotificationDoesNotOverwriteNewerServerDetails() {
        val current = alert.copy(status = "expired", magnitude = 4.2)
        val inbox = AlertInbox().receive(current, AlertChannel.POLLING, 2000).inbox
        assertEquals(current, inbox.receive(alert, AlertChannel.OPENED, 4000).inbox.entries.single().alert)
    }

    @Test fun readAndClearOperationsKeepDeduplication() {
        val first = AlertInbox().receive(alert, AlertChannel.PUSH, 2000).inbox
        assertEquals(0, first.markAllRead().unreadCount)
        val cleared = first.clearHistory()
        assertTrue(cleared.entries.isEmpty())
        val replay = cleared.receive(alert, AlertChannel.POLLING, 9000)
        assertFalse(replay.firstReceipt)
        assertTrue(replay.inbox.entries.isEmpty())
    }

    @Test fun storageIsBoundedAndKeepsRecentEntries() {
        var inbox = AlertInbox()
        repeat(600) { inbox = inbox.receive(alert.copy(uuid = "event-$it"), AlertChannel.POLLING, it.toLong()).inbox }
        assertEquals(AlertInbox.MAX_ENTRIES, inbox.entries.size)
        assertEquals(AlertInbox.MAX_PROCESSED, inbox.processedIds.size)
        assertEquals("event-599", inbox.entries.first().alert.uuid)
        assertEquals("event-550", inbox.entries.last().alert.uuid)
    }

    @Test fun malformedAlertsAreNotStoredOrClaimed() {
        val inbox = AlertInbox()
        listOf(alert.copy(uuid = ""), alert.copy(latitude = 91.0), alert.copy(magnitude = Double.NaN)).forEach {
            assertEquals(InboxUpdate(inbox, false), inbox.receive(it, AlertChannel.POLLING, 2000))
        }
    }

    @Test fun serializationKeepsReadStateAndTiming() {
        val inbox = AlertInbox().receive(alert, AlertChannel.POLLING, 2000).inbox.markAllRead()
        assertEquals(inbox, AlertInboxCodec.decode(AlertInboxCodec.encode(inbox), null, emptySet()))
    }

    @Test fun legacyCacheDoesNotFabricateAFreshCountdownOrUnreadNotice() {
        val legacy = """{"uuid":"legacy","magnitude":4.0,"seconds_to_s_wave":20.0}"""
        val inbox = AlertInboxCodec.decode(null, legacy, setOf("other"))
        assertEquals(0, inbox.unreadCount)
        assertEquals(null, inbox.entries.single().alert.seconds_to_s_wave)
        assertEquals(0L, inbox.entries.single().alert.receivedAtMs)
        assertTrue("legacy" in inbox.processedIds)
    }

    @Test fun corruptCacheKeepsKnownIdsAndAnExplicitEmptyHistoryDoesNotRemigrate() {
        val legacy = """{"uuid":"legacy","magnitude":4.0}"""
        val recovered = AlertInboxCodec.decode("invalid", legacy, setOf("other"))
        assertTrue("other" in recovered.processedIds)
        val cleared = recovered.clearHistory()
        assertTrue(AlertInboxCodec.decode(AlertInboxCodec.encode(cleared), legacy, emptySet()).entries.isEmpty())
    }
}
