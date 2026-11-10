package com.futsch1.medtimer.feature.ui.overview.actions

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.core.common.helpers.DatePickerDialogFactory
import com.futsch1.medtimer.core.common.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Coverage for Taken at: a time picker opens on the current date and time, its
 * Change date button picks another day and returns to the time picker, and one picked time applies
 * to every visited event.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class ActionsVisitorTakenAtTest {

    private class TimeDialog(
        val initialTime: LocalTime,
        val title: CharSequence?,
        val negativeButtonText: Int?,
        val onNegative: ((Int) -> Unit)?,
        val onPositive: (Int) -> Unit,
    )

    private class DateDialog(val initialDate: LocalDate?, val maxDate: LocalDate?, val onCancel: (() -> Unit)?, val onPositive: (Long) -> Unit)

    private val testDispatcher = UnconfinedTestDispatcher()

    private val reminderEventCreator: ReminderEventCreator = mock()
    private val preferencesDataSource: PreferencesDataSource = mock()
    private val persistentDataDataSource: PersistentDataDataSource = mock()
    private val timeAccess: TimeAccess = mock()
    private val commandBus: ReminderCommandBus = mock()
    private val timePickerDialogFactory: TimePickerDialogFactory = mock()
    private val datePickerDialogFactory: DatePickerDialogFactory = mock()
    private val timeFormatter: TimeFormatter = mock()

    private val zone = ZoneId.of("Europe/Berlin")
    private val today = LocalDate.of(2026, 10, 19)
    private val yesterday = today.minusDays(1)
    private val now = at(today, 9, 0)

    private val timeDialogs = mutableListOf<TimeDialog>()
    private val dateDialogs = mutableListOf<DateDialog>()

    private lateinit var activity: FragmentActivity
    private lateinit var actionsVisitor: ActionsVisitor

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        whenever(persistentDataDataSource.getPendingLocationSnoozes()).thenReturn(emptyList())
        whenever(timeAccess.now()).thenReturn(now)
        whenever(timeAccess.systemZone()).thenReturn(zone)
        whenever(timeFormatter.localDateToString(any())).thenAnswer { it.getArgument<LocalDate>(0).toString() }
        whenever(preferencesDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default()))
        stubPickers()

        actionsVisitor = ActionsVisitor(
            mock(),
            mock(),
            mock(),
            mock(),
            activity,
            reminderEventCreator,
            preferencesDataSource,
            commandBus,
            TakenTimePicker(datePickerDialogFactory, timePickerDialogFactory, activity, timeAccess, timeFormatter),
            timeAccess,
            CoroutineScope(testDispatcher)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onePickedTimeMarksEveryVisitedEventTaken() {
        val scheduledReminder = ScheduledReminder(Medicine.default(), Reminder.default(), at(today, 8, 30))
        stubCreatedEvent(scheduledReminder, reminderEventId = 3)

        actionsVisitor.startVisit(Button.TAKEN_AT).use {
            actionsVisitor.visit(pastEvent(1, at(today, 8, 0)))
            actionsVisitor.visit(pastEvent(2, at(today, 8, 0)))
            actionsVisitor.visit(scheduledReminder)
        }
        idle()
        confirmTime(8 * 60 + 15)

        verifyBlocking(commandBus) { markReminderEvents(listOf(3, 1, 2), ReminderEvent.ReminderStatus.TAKEN, at(today, 8, 15)) }
    }

    @Test
    fun onlyTheTimePickerOpensWithChangeDateButton() {
        takenAt(pastEvent(1, at(today, 8, 0)))

        assertEquals(1, timeDialogs.size)
        assertTrue(dateDialogs.isEmpty())
        assertEquals(R.string.change_date, timeDialogs.single().negativeButtonText)
        assertNotNull(timeDialogs.single().onNegative)
    }

    @Test
    fun opensOnTodayForAPastDayReminder() {
        takenAt(pastEvent(1, at(yesterday, 21, 0)))

        val dialog = timeDialogs.single()
        assertEquals(LocalTime.of(9, 0), dialog.initialTime)
        assertEquals(activity.getString(R.string.taken_on_date, today.toString()), dialog.title)
        confirmTime(7 * 60)

        verifyBlocking(commandBus) { markReminderEvents(listOf(1), ReminderEvent.ReminderStatus.TAKEN, at(today, 7, 0)) }
    }

    @Test
    fun opensOnTodayForAReminderToday() {
        takenAt(pastEvent(1, at(today, 8, 0)))

        assertEquals(LocalTime.of(9, 0), timeDialogs.single().initialTime)
        assertEquals(activity.getString(R.string.taken_on_date, today.toString()), timeDialogs.single().title)
        confirmTime(8 * 60 + 45)

        verifyBlocking(commandBus) { markReminderEvents(listOf(1), ReminderEvent.ReminderStatus.TAKEN, at(today, 8, 45)) }
    }

    @Test
    fun changeDateOkReturnsToTheTimePickerOnThePickedDate() {
        takenAt(pastEvent(1, at(yesterday, 21, 0)))

        timeDialogs.single().onNegative!!(22 * 60)
        idle()
        val dateDialog = dateDialogs.single()
        assertEquals(today, dateDialog.initialDate)
        assertEquals(today, dateDialog.maxDate)
        assertNotNull(dateDialog.onCancel)
        verifyBlocking(commandBus, never()) { markReminderEvents(any(), any(), anyOrNull()) }

        val pickedDate = today.minusDays(3)
        dateDialog.onPositive(pickedDate.toEpochDay())
        idle()

        verifyBlocking(commandBus, never()) { markReminderEvents(any(), any(), anyOrNull()) }
        assertEquals(2, timeDialogs.size)
        val reopened = timeDialogs.last()
        assertEquals(LocalTime.of(22, 0), reopened.initialTime)
        assertEquals(activity.getString(R.string.taken_on_date, pickedDate.toString()), reopened.title)
        assertEquals(R.string.change_date, reopened.negativeButtonText)
        confirmTime(22 * 60, reopened)

        verifyBlocking(commandBus) { markReminderEvents(listOf(1), ReminderEvent.ReminderStatus.TAKEN, at(pickedDate, 22, 0)) }
    }

    @Test
    fun cancellingTheDatePickerReturnsToTheTimePickerOnTheOldDate() {
        takenAt(pastEvent(1, at(today, 8, 0)))

        timeDialogs.single().onNegative!!(8 * 60)
        idle()
        dateDialogs.single().onCancel!!()
        idle()

        verifyBlocking(commandBus, never()) { markReminderEvents(any(), any(), anyOrNull()) }
        assertEquals(2, timeDialogs.size)
        val reopened = timeDialogs.last()
        assertEquals(LocalTime.of(8, 0), reopened.initialTime)
        assertEquals(activity.getString(R.string.taken_on_date, today.toString()), reopened.title)
        confirmTime(8 * 60, reopened)

        verifyBlocking(commandBus) { markReminderEvents(listOf(1), ReminderEvent.ReminderStatus.TAKEN, at(today, 8, 0)) }
    }

    @Test
    fun aFutureTimeRetriesOnNowAndMarksNothing() {
        takenAt(pastEvent(1, at(today, 8, 0)))

        confirmTime(10 * 60)

        verifyBlocking(commandBus, never()) { markReminderEvents(any(), any(), anyOrNull()) }
        val retry = timeDialogs.last()
        assertEquals(2, timeDialogs.size)
        assertEquals(LocalTime.of(9, 0), retry.initialTime)
        assertEquals(activity.getString(R.string.taken_time_in_future), retry.title)
        assertEquals(R.string.change_date, retry.negativeButtonText)

        // The retry keeps the date in effect, also for its own Change date button.
        retry.onNegative!!(7 * 60)
        idle()
        assertEquals(today, dateDialogs.single().initialDate)
        dateDialogs.single().onPositive(yesterday.toEpochDay())
        idle()

        verifyBlocking(commandBus, never()) { markReminderEvents(any(), any(), anyOrNull()) }
        assertEquals(3, timeDialogs.size)
        val reopened = timeDialogs.last()
        assertEquals(LocalTime.of(7, 0), reopened.initialTime)
        assertEquals(activity.getString(R.string.taken_on_date, yesterday.toString()), reopened.title)
        confirmTime(7 * 60, reopened)

        verifyBlocking(commandBus) { markReminderEvents(listOf(1), ReminderEvent.ReminderStatus.TAKEN, at(yesterday, 7, 0)) }
    }

    @Test
    fun aFutureTimeOnAChangedDateRetriesOnThatDate() {
        takenAt(pastEvent(1, at(yesterday, 21, 0)))

        timeDialogs.single().onNegative!!(10 * 60)
        idle()
        dateDialogs.single().onPositive(today.toEpochDay())
        idle()

        val reopened = timeDialogs.last()
        assertEquals(LocalTime.of(10, 0), reopened.initialTime)
        assertEquals(activity.getString(R.string.taken_on_date, today.toString()), reopened.title)
        confirmTime(10 * 60, reopened)

        verifyBlocking(commandBus, never()) { markReminderEvents(any(), any(), anyOrNull()) }
        val retry = timeDialogs.last()
        assertEquals(3, timeDialogs.size)
        assertEquals(activity.getString(R.string.taken_time_in_future), retry.title)
        assertEquals(R.string.change_date, retry.negativeButtonText)

        // The retry keeps the changed date for its own Change date button.
        retry.onNegative!!(8 * 60)
        idle()
        assertEquals(today, dateDialogs.last().initialDate)
        dateDialogs.last().onCancel!!()
        idle()
        confirmTime(8 * 60, timeDialogs.last())

        verifyBlocking(commandBus) { markReminderEvents(listOf(1), ReminderEvent.ReminderStatus.TAKEN, at(today, 8, 0)) }
    }

    @Test
    fun backOnTheReopenedTimePickerMarksNothing() {
        takenAt(pastEvent(1, at(today, 8, 0)))

        timeDialogs.single().onNegative!!(8 * 60)
        idle()
        dateDialogs.single().onPositive(yesterday.toEpochDay())
        idle()

        assertEquals(2, timeDialogs.size)
        verifyBlocking(commandBus, never()) { markReminderEvents(any(), any(), anyOrNull()) }
    }

    @Test
    fun cancelAfterAnEarlierDateOkKeepsThatDate() {
        takenAt(pastEvent(1, at(today, 8, 0)))
        val threeDaysAgo = today.minusDays(3)

        timeDialogs.single().onNegative!!(8 * 60)
        idle()
        dateDialogs.single().onPositive(threeDaysAgo.toEpochDay())
        idle()
        timeDialogs.last().onNegative!!(11 * 60)
        idle()

        assertEquals(2, dateDialogs.size)
        assertEquals(threeDaysAgo, dateDialogs.last().initialDate)
        dateDialogs.last().onCancel!!()
        idle()

        val reopened = timeDialogs.last()
        assertEquals(3, timeDialogs.size)
        assertEquals(LocalTime.of(11, 0), reopened.initialTime)
        assertEquals(activity.getString(R.string.taken_on_date, threeDaysAgo.toString()), reopened.title)
        confirmTime(11 * 60, reopened)

        verifyBlocking(commandBus) { markReminderEvents(listOf(1), ReminderEvent.ReminderStatus.TAKEN, at(threeDaysAgo, 11, 0)) }
    }

    @Test
    fun changeDateKeepsTheClockEditedBeforeIt() {
        takenAt(pastEvent(1, at(today, 8, 0)))
        assertEquals(LocalTime.of(9, 0), timeDialogs.single().initialTime)

        timeDialogs.single().onNegative!!(6 * 60 + 30)
        idle()
        dateDialogs.single().onCancel!!()
        idle()

        assertEquals(LocalTime.of(6, 30), timeDialogs.last().initialTime)
    }

    @Test
    fun cancellingThePickerMarksNothing() {
        takenAt(pastEvent(1, at(today, 8, 0)))

        verifyBlocking(commandBus, never()) { markReminderEvents(any(), any(), anyOrNull()) }
    }

    private fun takenAt(pastReminderEvent: PastReminderEvent) {
        actionsVisitor.startVisit(Button.TAKEN_AT).use {
            actionsVisitor.visit(pastReminderEvent)
        }
        idle()
    }

    private fun confirmTime(minutes: Int, dialog: TimeDialog = timeDialogs.single()) {
        dialog.onPositive(minutes)
        idle()
    }

    private fun idle() = shadowOf(activity.mainLooper).idle()

    private fun at(date: LocalDate, hour: Int, minute: Int): Instant = date.atTime(hour, minute).atZone(zone).toInstant()

    private fun stubPickers() {
        whenever(timePickerDialogFactory.create(any<LocalTime>(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()))
            .thenAnswer {
                timeDialogs.add(TimeDialog(it.getArgument(0), it.getArgument(3), it.getArgument(4), it.getArgument(5), it.getArgument(6)))
                mock<MaterialTimePicker>()
            }
        whenever(datePickerDialogFactory.create(anyOrNull(), anyOrNull(), anyOrNull(), any()))
            .thenAnswer {
                dateDialogs.add(DateDialog(it.getArgument(0), it.getArgument(1), it.getArgument(2), it.getArgument(3)))
                mock<MaterialDatePicker<Long>>()
            }
    }

    private fun stubCreatedEvent(scheduledReminder: ScheduledReminder, reminderEventId: Int) {
        reminderEventCreator.stub {
            onBlocking { getOrCreateReminderEvent(scheduledReminder, scheduledReminder.timestamp.epochSecond) }
                .thenReturn(ReminderEvent.default().copy(reminderEventId = reminderEventId))
        }
    }

    private fun pastEvent(id: Int, remindedTimestamp: Instant) = PastReminderEvent(
        preferencesDataSource,
        persistentDataDataSource,
        timeAccess,
        ReminderEvent.default().copy(reminderEventId = id, reminderId = id, remindedTimestamp = remindedTimestamp),
    )
}
