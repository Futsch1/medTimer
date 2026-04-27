package com.futsch1.medtimer.location

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PersistentDataDataSourcePendingSnoozeTest {
    private lateinit var dataSource: PersistentDataDataSource

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val defaultPrefs = context.getSharedPreferences("test_default", Context.MODE_PRIVATE)
        val medTimerPrefs = context.getSharedPreferences("test_medtimer", Context.MODE_PRIVATE)
        defaultPrefs.edit().clear().commit()
        medTimerPrefs.edit().clear().commit()
        dataSource = PersistentDataDataSource(
            defaultPrefs,
            medTimerPrefs,
            CoroutineScope(Dispatchers.Unconfined),
            GsonBuilder().create()
        )
    }

    @Test
    fun getPendingSnoozeReturnsEmptyListInitially() {
        assertTrue(dataSource.getPendingLocationSnoozes().isEmpty())
    }

    @Test
    fun addAndGetPendingSnoozeRoundTrip() {
        val data = ReminderNotificationData(
            remindInstant = Instant.ofEpochSecond(1234567890L),
            reminderIds = listOf(1, 2),
            reminderEventIds = mutableListOf(10, 20),
            notificationId = 42
        )
        dataSource.addPendingLocationSnooze(data)

        val loaded = dataSource.getPendingLocationSnoozes()
        assertEquals(1, loaded.size)
        val result = loaded[0]
        assertEquals(1234567890L, result.remindInstant.epochSecond)
        assertEquals(listOf(1, 2), result.reminderIds)
        assertEquals(listOf(10, 20), result.reminderEventIds)
        assertEquals(42, result.notificationId)
    }

    @Test
    fun multiplePendingSnoozesAccumulate() {
        dataSource.addPendingLocationSnooze(ReminderNotificationData(Instant.ofEpochSecond(100), listOf(1), mutableListOf(10), 1))
        dataSource.addPendingLocationSnooze(ReminderNotificationData(Instant.ofEpochSecond(200), listOf(2), mutableListOf(20), 2))
        dataSource.addPendingLocationSnooze(ReminderNotificationData(Instant.ofEpochSecond(300), listOf(3), mutableListOf(30), 3))

        assertEquals(3, dataSource.getPendingLocationSnoozes().size)
    }

    @Test
    fun clearAllPendingSnoozesEmptiesList() {
        dataSource.addPendingLocationSnooze(ReminderNotificationData(Instant.ofEpochSecond(100), listOf(1), mutableListOf(10), 1))
        dataSource.addPendingLocationSnooze(ReminderNotificationData(Instant.ofEpochSecond(200), listOf(2), mutableListOf(20), 2))

        dataSource.clearAllPendingLocationSnoozes()

        assertTrue(dataSource.getPendingLocationSnoozes().isEmpty())
    }
}
