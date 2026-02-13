package com.futsch1.medtimer.processorTests

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.SharedPreferences
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.TimeAccess
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MedicineRepositoryFake {
    val medicines = mutableListOf<Medicine>()
    val reminderEvents = mutableListOf<ReminderEvent>()
    val reminders = mutableListOf<Reminder>()

    val mock: MedicineRepository = mock(MedicineRepository::class.java)

    init {
        `when`(mock.medicines).thenAnswer { buildFullMedicines() }
        `when`(mock.getReminderEventsForScheduling(anyList())).thenAnswer { reminderEvents }
        `when`(mock.getReminderEvent(anyInt())).thenAnswer { reminderEvents.first { r -> r.reminderEventId == it.arguments[0] } }
        `when`(mock.insertReminderEvent(anyNotNull())).thenAnswer {
            val reminderEvent = it.arguments[0] as ReminderEvent
            reminderEvent.reminderEventId = reminderEvents.size + 1
            reminderEvents.add(reminderEvent)
            reminderEvent.reminderEventId.toLong()
        }
        `when`(mock.getReminder(anyInt())).thenAnswer { reminders.first { r -> r.reminderId == it.arguments[0] } }
        `when`(mock.getMedicine(anyInt())).thenAnswer { buildFullMedicines().first { m -> m.medicine.medicineId == it.arguments[0] } }
        `when`(mock.updateMedicine(anyNotNull())).thenAnswer {
            val medicine = it.arguments[0] as Medicine
            val index = medicines.indexOfFirst { m -> m.medicineId == medicine.medicineId }
            medicines[index] = medicine
        }
    }

    fun buildFullMedicines(): List<FullMedicine> {
        val fullMedicines = mutableListOf<FullMedicine>()
        for (medicine in medicines) {
            val reminders = this.reminders.stream().filter { r -> r.medicineRelId == medicine.medicineId }
            val fullMedicine = FullMedicine()
            fullMedicine.medicine = medicine
            fullMedicine.reminders = reminders.toList()
            fullMedicine.tags = listOf()
            fullMedicines.add(fullMedicine)
        }
        return fullMedicines
    }

    fun <T> anyNotNull(): T = any()
}

class NotificationManagerFake {
    val activeNotifications = mutableListOf<Notification>()

    val mock: NotificationManager = mock(NotificationManager::class.java)

    init {
        `when`(mock.activeNotifications).thenReturn(getNotifications())
    }

    fun getNotifications(): Array<StatusBarNotification> {
        val statusBarNotifications = mutableListOf<StatusBarNotification>()
        for (activeNotification in activeNotifications) {
            val statusBarNotificationMock = mock(StatusBarNotification::class.java)
            `when`(statusBarNotificationMock.notification).thenReturn(activeNotification)
            statusBarNotifications.add(statusBarNotificationMock)
        }
        return statusBarNotifications.toTypedArray()
    }
}

class TestReminderContext {
    val alarmManagerMock: AlarmManager = mock(AlarmManager::class.java)
    val notificationManagerFake = NotificationManagerFake()
    val notificationChannelMock: NotificationChannel = mock(NotificationChannel::class.java)
    val preferencesMock: SharedPreferences = mock(SharedPreferences::class.java)
    val mock: ReminderContext = mock(ReminderContext::class.java)
    val medicineRepositoryFake = MedicineRepositoryFake()
    val notificationBuilderMock: NotificationCompat.Builder = mock(NotificationCompat.Builder::class.java)
    val localPreferencesMock: SharedPreferences = mock(SharedPreferences::class.java)

    val preferencesMap = mutableMapOf(
        PreferencesNames.NUMBER_OF_REPETITIONS to "3",
        PreferencesNames.SNOOZE_DURATION to "15"
    )
    val stringList = mapOf(
        R.string.high to "High",
        R.string.default_ to "Default"
    )
    var notificationId: Int = 0
    val localDate: LocalDate = LocalDate.ofEpochDay(0)
    val instant: Instant = Instant.ofEpochSecond(0)

    init {
        `when`(mock.alarmManager).thenReturn(alarmManagerMock)
        `when`(mock.notificationManager).thenReturn(notificationManagerFake.mock)
        `when`(mock.preferences).thenReturn(preferencesMock)
        `when`(mock.medicineRepository).thenReturn(medicineRepositoryFake.mock)
        `when`(mock.localPreferences).thenReturn(localPreferencesMock)
        `when`(mock.buildNotificationChannel(anyString(), anyString(), anyInt())).thenReturn(notificationChannelMock)
        `when`(mock.getString(anyInt())).thenAnswer { stringList[it.arguments[0]] }
        `when`(mock.getNotificationBuilder(anyString())).thenReturn(notificationBuilderMock)
        `when`(mock.timeAccess).thenReturn(object : TimeAccess {
            override fun systemZone(): ZoneId = ZoneId.of("UTC")
            override fun localDate(): LocalDate = localDate
            override fun now(): Instant = instant
        })

        `when`(preferencesMock.getString(anyString(), anyString())).thenAnswer { preferencesMap[it.arguments[0]] }

        `when`(localPreferencesMock.getInt(eq("notificationId"), anyInt())).thenAnswer { notificationId }
        val editMock = mock(SharedPreferences.Editor::class.java)
        `when`(localPreferencesMock.edit()).thenReturn(editMock)
        `when`(editMock.putInt(eq("notificationId"), anyInt())).then { notificationId = it.arguments[1] as Int; editMock }

        `when`(notificationChannelMock.id).thenReturn("channel")

        val intentMock = mock(Intent::class.java)
        `when`(mock.getIntent(anyString())).thenReturn(intentMock)
    }
}
