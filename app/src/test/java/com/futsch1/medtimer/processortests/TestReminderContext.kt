package com.futsch1.medtimer.processortests

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.text.SpannableStringBuilder
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.model.PersistentData
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.preferences.PreferencesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.time.Instant
import java.time.LocalDate

class RepositoryFakes {
    val medicines = mutableListOf<Medicine>()
    val reminderEvents = mutableListOf<ReminderEvent>()
    val reminders = mutableListOf<Reminder>()

    val medicineRepositoryMock: MedicineRepository = mock()
    val reminderRepositoryMock: ReminderRepository = mock()
    val reminderEventRepositoryMock: ReminderEventRepository = mock()

    init {
        // MedicineRepository mocks
        `when`(runBlocking { medicineRepositoryMock.getFullAll() }).thenAnswer { buildFullMedicines() }
        `when`(runBlocking { medicineRepositoryMock.getFull(anyInt()) }).thenAnswer { buildFullMedicines().first { m -> m.medicine.medicineId == it.arguments[0] } }
        `when`(runBlocking { medicineRepositoryMock.update(any()) }).thenAnswer {
            val medicine = it.arguments[0] as Medicine
            val index = medicines.indexOfFirst { m -> m.medicineId == medicine.medicineId }
            medicines[index] = medicine
        }

        // ReminderRepository mocks
        `when`(runBlocking { reminderRepositoryMock.get(anyInt()) }).thenAnswer { reminders.first { r -> r.reminderId == it.arguments[0] } }

        // ReminderEventRepository mocks
        `when`(runBlocking { reminderEventRepositoryMock.getForScheduling(anyList()) }).thenAnswer { reminderEvents }
        `when`(runBlocking { reminderEventRepositoryMock.get(anyInt()) }).thenAnswer { reminderEvents.first { r -> r.reminderEventId == it.arguments[0] } }
        `when`(runBlocking { reminderEventRepositoryMock.create(any()) }).thenAnswer {
            val reminderEvent = it.arguments[0] as ReminderEvent
            reminderEvent.reminderEventId = reminderEvents.size + 1
            reminderEvents.add(reminderEvent)
            reminderEvent.reminderEventId.toLong()
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
}

class NotificationManagerFake {
    val activeNotifications = mutableMapOf<Int, Notification>()

    val mock: NotificationManager = mock()

    init {
        `when`(mock.activeNotifications).thenAnswer { getNotifications() }
    }

    fun getNotifications(): Array<StatusBarNotification> {
        val statusBarNotifications = mutableListOf<StatusBarNotification>()
        for (activeNotification in activeNotifications) {
            val statusBarNotificationMock = mock<StatusBarNotification>()
            `when`(statusBarNotificationMock.id).thenReturn(activeNotification.key)
            `when`(statusBarNotificationMock.notification).thenReturn(activeNotification.value)
            statusBarNotifications.add(statusBarNotificationMock)
        }
        return statusBarNotifications.toTypedArray()
    }

    fun add(id: Int, reminderIds: IntArray = intArrayOf(), reminderEventIds: IntArray = intArrayOf(), remindTimestamp: Long = 0) {
        val notificationMock = mock<Notification>()
        val bundleMock = mock<Bundle>()
        notificationMock.extras = bundleMock
        `when`(bundleMock.getIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST)).thenReturn(reminderIds)
        `when`(bundleMock.getIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST)).thenReturn(reminderEventIds)
        `when`(bundleMock.getLong(ActivityCodes.EXTRA_REMIND_INSTANT)).thenReturn(remindTimestamp)
        `when`(bundleMock.getInt(ActivityCodes.EXTRA_NOTIFICATION_ID)).thenReturn(id)
        activeNotifications[id] = notificationMock
    }
}

class TestReminderContext {
    val alarmManagerMock: AlarmManager = mock()
    val notificationManagerFake = NotificationManagerFake()
    val notificationChannelMock: NotificationChannel = mock()
    val contextMock: Context = mock()
    val repositoryFakes = RepositoryFakes()
    val localPreferencesMock: SharedPreferences = mock()
    val preferencesDataSourceMock: PreferencesDataSource = mock()
    val persistentDataDataSourceMock: PersistentDataDataSource = mock()

    var notificationId: Int = 1
    val localDate: LocalDate = LocalDate.ofEpochDay(0)
    var instant: Instant = Instant.ofEpochSecond(0)
    var userPreferences = UserPreferences.default()
    var persistentData = PersistentData.default()

    init {
        `when`(contextMock.packageName).thenReturn("com.futsch1.medtimer")

        `when`(alarmManagerMock.canScheduleExactAlarms()).thenReturn(true)

        `when`(localPreferencesMock.getInt(eq("notificationId"), anyInt())).thenAnswer { notificationId }
        val editMock = mock<SharedPreferences.Editor>()
        `when`(localPreferencesMock.edit()).thenReturn(editMock)
        `when`(editMock.putInt(eq("notificationId"), anyInt())).then { notificationId = it.arguments[1] as Int; editMock }

        `when`(notificationChannelMock.id).thenReturn("channel")

        val spannableStringBuilderMock = mock<SpannableStringBuilder>()
        `when`(spannableStringBuilderMock.append(any<CharSequence>())).thenReturn(spannableStringBuilderMock)

        `when`(preferencesDataSourceMock.preferences).thenAnswer { MutableStateFlow(userPreferences) }
        `when`(persistentDataDataSourceMock.data).thenAnswer { MutableStateFlow(persistentData) }
    }
}
