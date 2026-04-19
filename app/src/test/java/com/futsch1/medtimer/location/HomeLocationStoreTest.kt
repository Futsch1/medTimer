package com.futsch1.medtimer.location

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeLocationStoreTest {
    private lateinit var store: HomeLocationStore

    @Before
    fun setUp() {
        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_location", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        store = HomeLocationStore(prefs, GsonBuilder().create())
    }

    @Test
    fun `getHomeLocation returns null when nothing saved`() {
        assertNull(store.getHomeLocation())
    }

    @Test
    fun `saveHomeLocation and getHomeLocation round-trip`() {
        val location = HomeLocation(48.137, 11.575, 200f)
        store.saveHomeLocation(location)
        val loaded = store.getHomeLocation()
        assertNotNull(loaded)
        assertEquals(48.137, loaded!!.latitude, 0.0001)
        assertEquals(11.575, loaded.longitude, 0.0001)
        assertEquals(200f, loaded.radiusMeters, 0.01f)
    }

    @Test
    fun `clearHomeLocation removes saved location`() {
        store.saveHomeLocation(HomeLocation(1.0, 2.0))
        store.clearHomeLocation()
        assertNull(store.getHomeLocation())
    }

    @Test
    fun `getPendingLocationSnoozes returns empty list when nothing saved`() {
        assertTrue(store.getPendingLocationSnoozes().isEmpty())
    }

    @Test
    fun `addPendingLocationSnooze and getPendingLocationSnoozes round-trip`() {
        val data = ReminderNotificationData(
            remindInstant = Instant.ofEpochSecond(1234567890L),
            reminderIds = intArrayOf(1, 2),
            reminderEventIds = intArrayOf(10, 20),
            notificationId = 42
        )
        store.addPendingLocationSnooze(data)

        val loaded = store.getPendingLocationSnoozes()
        assertEquals(1, loaded.size)
        val result = loaded[0]
        assertEquals(1234567890L, result.remindInstant.epochSecond)
        assertTrue(result.reminderIds.contentEquals(intArrayOf(1, 2)))
        assertTrue(result.reminderEventIds.contentEquals(intArrayOf(10, 20)))
        assertEquals(42, result.notificationId)
    }

    @Test
    fun `multiple addPendingLocationSnooze calls accumulate entries`() {
        store.addPendingLocationSnooze(ReminderNotificationData(Instant.ofEpochSecond(100), intArrayOf(1), intArrayOf(10), 1))
        store.addPendingLocationSnooze(ReminderNotificationData(Instant.ofEpochSecond(200), intArrayOf(2), intArrayOf(20), 2))
        store.addPendingLocationSnooze(ReminderNotificationData(Instant.ofEpochSecond(300), intArrayOf(3), intArrayOf(30), 3))

        assertEquals(3, store.getPendingLocationSnoozes().size)
    }

    @Test
    fun `clearAllPendingLocationSnoozes empties the list`() {
        store.addPendingLocationSnooze(ReminderNotificationData(Instant.ofEpochSecond(100), intArrayOf(1), intArrayOf(10), 1))
        store.addPendingLocationSnooze(ReminderNotificationData(Instant.ofEpochSecond(200), intArrayOf(2), intArrayOf(20), 2))

        store.clearAllPendingLocationSnoozes()

        assertTrue(store.getPendingLocationSnoozes().isEmpty())
    }
}
