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
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.database.toModel.toEntity
import com.futsch1.medtimer.database.toModel.toModel
import com.futsch1.medtimer.model.PersistentData
import com.futsch1.medtimer.model.ReminderEvent
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
    val medicines = mutableListOf<MedicineEntity>()
    val reminderEvents = mutableListOf<ReminderEventEntity>()
    val reminders = mutableListOf<ReminderEntity>()

    val medicineRepositoryMock: MedicineRepository = mock()
    val reminderRepositoryMock: ReminderRepository = mock()
    val reminderEventRepositoryMock: ReminderEventRepository = mock()

    init {
        // MedicineRepository mocks
        `when`(runBlocking { medicineRepositoryMock.getAll() }).thenAnswer { buildMedicines() }
        `when`(runBlocking { medicineRepositoryMock.get(anyInt()) }).thenAnswer { buildMedicines().firstOrNull { m -> m.id == it.arguments[0] } }
        `when`(runBlocking { medicineRepositoryMock.update(any()) }).thenAnswer {
            val medicine = it.arguments[0] as com.futsch1.medtimer.model.Medicine
            val index = medicines.indexOfFirst { m -> m.medicineId == medicine.id }
            if (index >= 0) medicines[index] = medicine.toEntity()
        }

        // ReminderRepository mocks
        `when`(runBlocking { reminderRepositoryMock.get(anyInt()) }).thenAnswer { reminders.firstOrNull { r -> r.reminderId == it.arguments[0] }?.toModel() }

        // ReminderEventRepository mocks
        `when`(runBlocking { reminderEventRepositoryMock.getForScheduling(anyList()) }).thenAnswer { reminderEvents.map { it.toModel() } }
        `when`(runBlocking { reminderEventRepositoryMock.get(anyInt()) }).thenAnswer {
            reminderEvents.firstOrNull { r -> r.reminderEventId == it.arguments[0] }?.toModel()
        }
        `when`(runBlocking { reminderEventRepositoryMock.create(any()) }).thenAnswer {
            val reminderEvent = (it.arguments[0] as ReminderEvent).toEntity()
            reminderEvent.reminderEventId = reminderEvents.size + 1
            reminderEvents.add(reminderEvent)
            reminderEvents.size.toLong()
        }
        `when`(runBlocking { reminderEventRepositoryMock.update(any()) }).thenAnswer {
            val domainEvent = it.arguments[0] as ReminderEvent
            val index = reminderEvents.indexOfFirst { e -> e.reminderEventId == domainEvent.reminderEventId }
            if (index >= 0) reminderEvents[index] = domainEvent.toEntity()
        }
        `when`(runBlocking { reminderEventRepositoryMock.updateAll(anyList()) }).thenAnswer {
            @Suppress("UNCHECKED_CAST")
            val domainEvents = it.arguments[0] as List<ReminderEvent>
            domainEvents.forEach { domainEvent ->
                val index = reminderEvents.indexOfFirst { e -> e.reminderEventId == domainEvent.reminderEventId }
                if (index >= 0) reminderEvents[index] = domainEvent.toEntity()
            }
        }
    }

    fun buildMedicines(): List<com.futsch1.medtimer.model.Medicine> {
        return medicines.map { medicineEntity ->
            val fullMedicine = FullMedicineEntity()
            fullMedicine.medicine = medicineEntity
            fullMedicine.reminders = this.reminders.filter { r -> r.medicineRelId == medicineEntity.medicineId }.toMutableList()
            fullMedicine.tags = listOf()
            fullMedicine.toModel()
        }
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
