package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderTime
import com.futsch1.medtimer.core.domain.model.Tag
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.feature.reminders.api.buildReminderEvent
import com.futsch1.medtimer.feature.reminders.api.scheduling.ReminderEdit
import com.futsch1.medtimer.feature.reminders.api.scheduling.ReminderEditAction
import com.futsch1.medtimer.feature.reminders.api.scheduling.planReminderEdit
import com.futsch1.medtimer.feature.reminders.api.scheduling.withSnapshotFields
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * One test per row of the rule table in ReminderEditRules.kt. Times are UTC; "today" is Tuesday 1970-01-06.
 */
class ReminderEditRulesTest {
    private val zone: ZoneId = ZoneId.of("Z")
    private var today: LocalDate = LocalDate.EPOCH.plusDays(5)
    private var now: Instant = at(8, 30)
    private val preferencesFlow = MutableStateFlow(UserPreferences.default())

    private val timeAccess = object : TimeAccess {
        override fun systemZone(): ZoneId = zone
        override fun localDate(): LocalDate = today
        override fun now(): Instant = now
    }
    private val dataSource: PreferencesDataSource = Mockito.mock<PreferencesDataSource>().also {
        Mockito.`when`(it.preferences).thenReturn(preferencesFlow)
    }

    private val medicine = Medicine.default().copy(id = 1, name = "Medicine A")
    private val reminder = Reminder.default().copy(id = 1, medicineRelId = 1, amount = "1", time = ReminderTime(480))

    private fun at(hour: Int, minute: Int, date: LocalDate = today): Instant =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant()

    private fun openEvent(
        id: Int,
        time: Instant,
        forReminder: Reminder = reminder,
        status: ReminderEvent.ReminderStatus = ReminderEvent.ReminderStatus.RAISED
    ): ReminderEvent =
        ReminderEvent.default().copy(reminderEventId = id, reminderId = forReminder.id, remindedTimestamp = time, status = status)
            .withSnapshotFields(medicine, forReminder)

    private fun plan(
        newMedicine: Medicine?,
        newReminder: Reminder?,
        openEvents: List<ReminderEvent>,
        schedulingEvents: List<ReminderEvent> = openEvents,
        oldReminder: Reminder = reminder
    ): List<ReminderEditAction> =
        planReminderEdit(ReminderEdit(medicine, oldReminder, newMedicine, newReminder), openEvents, schedulingEvents, timeAccess, dataSource)

    private fun single(actions: List<ReminderEditAction>): ReminderEditAction {
        assertEquals(1, actions.size)
        return actions[0]
    }

    @Test
    fun noChangeKeepsEvent() {
        val event = openEvent(11, at(8, 0))
        assertEquals(ReminderEditAction.Keep(event), single(plan(medicine, reminder, listOf(event))))
    }

    @Test
    fun timeMovedLaterDropsRaisedEvent() {
        val event = openEvent(11, at(8, 0))
        val action = single(plan(medicine, reminder.copy(time = ReminderTime(540)), listOf(event)))
        assertEquals(ReminderEditAction.Drop(event), action)
    }

    @Test
    fun timeMovedEarlierFutureDropsRaisedEvent() {
        now = at(7, 0)
        val oldReminder = reminder.copy(time = ReminderTime(600))
        val event = openEvent(11, at(10, 0), oldReminder)
        val action = single(plan(medicine, reminder.copy(time = ReminderTime(540)), listOf(event), oldReminder = oldReminder))
        assertEquals(ReminderEditAction.Drop(event), action)
    }

    @Test
    fun timeMovedIntoPastRetimesRaisedEvent() {
        val event = openEvent(11, at(8, 0))
        val action = single(plan(medicine, reminder.copy(time = ReminderTime(495), amount = "2"), listOf(event)))
        assertEquals(ReminderEditAction.Retime(event.copy(remindedTimestamp = at(8, 15), amount = "2")), action)
    }

    @Test
    fun snoozedEventFollowsEdit() {
        // Snooze does not change the dose time, so a snoozed dose counts as set by the schedule
        val event = openEvent(11, at(8, 0)).copy(remainingRepeats = 2)
        assertEquals(ReminderEditAction.Drop(event), single(plan(medicine, reminder.copy(time = ReminderTime(540)), listOf(event))))
        val retimed = single(plan(medicine, reminder.copy(time = ReminderTime(500)), listOf(event)))
        assertEquals(ReminderEditAction.Retime(event.copy(remindedTimestamp = at(8, 20))), retimed)
    }

    @Test
    fun amountChangedRefreshesEvent() {
        val event = openEvent(11, at(8, 0))
        val action = single(plan(medicine, reminder.copy(amount = "2", variableAmount = true), listOf(event)))
        assertEquals(ReminderEditAction.Refresh(event.copy(amount = "2", askForAmount = true)), action)
    }

    @Test
    fun medicineNameColorIconTagsChangedRefreshEvent() {
        val event = openEvent(11, at(8, 0))
        val newMedicine = medicine.copy(
            name = "Medicine B", color = 0x123456, useColor = true, iconId = 7, tags = listOf(Tag("Morning", 1)), cannotBeSkipped = true
        )
        val action = single(plan(newMedicine, reminder, listOf(event)))
        assertEquals(
            ReminderEditAction.Refresh(
                event.copy(medicineName = "Medicine B", color = 0x123456, useColor = true, iconId = 7, tags = listOf("Morning"), cannotBeSkipped = true)
            ),
            action
        )
    }

    @Test
    fun refreshDoesNotTouchStockValues() {
        val event = openEvent(11, at(8, 0)).copy(stockBefore = 10.0, stockAfter = 10.0, stockUnit = "pills", stockHandled = false)
        val action = single(plan(medicine.copy(amount = 4.0, unit = "tablets"), reminder.copy(amount = "2"), listOf(event)))
        val refreshed = action.reminderEvent
        assertTrue(action is ReminderEditAction.Refresh)
        assertEquals(10.0, refreshed.stockBefore)
        assertEquals(10.0, refreshed.stockAfter)
        assertEquals("pills", refreshed.stockUnit)
        assertEquals(false, refreshed.stockHandled)
    }

    @Test
    fun takenSkippedAcknowledgedEventsAreNotTouched() {
        val events = listOf(
            openEvent(11, at(8, 0), status = ReminderEvent.ReminderStatus.TAKEN),
            openEvent(12, at(8, 0), status = ReminderEvent.ReminderStatus.SKIPPED),
            openEvent(13, at(8, 0), status = ReminderEvent.ReminderStatus.ACKNOWLEDGED),
            openEvent(14, at(8, 0), status = ReminderEvent.ReminderStatus.DELETED)
        )
        assertEquals(emptyList(), plan(medicine, reminder.copy(time = ReminderTime(540), amount = "2"), events))
        assertEquals(emptyList(), plan(null, null, events))
    }

    @Test
    fun takenAtEventEarlierTodayIsNotTouched() {
        val taken = openEvent(11, at(8, 0), status = ReminderEvent.ReminderStatus.TAKEN).copy(processedTimestamp = at(7, 30))
        assertEquals(emptyList(), plan(medicine, reminder.copy(time = ReminderTime(540)), listOf(taken)))
    }

    @Test
    fun manuallyRescheduledEventKeepsTimeAndRefreshesDetails() {
        val event = openEvent(11, at(10, 0))
        val action = single(plan(medicine, reminder.copy(time = ReminderTime(540), amount = "2"), listOf(event)))
        assertEquals(ReminderEditAction.Refresh(event.copy(amount = "2")), action)
    }

    @Test
    fun manualEventOnFutureDayKeepsTimeAndRefreshesDetails() {
        val event = openEvent(11, at(8, 0, today.plusDays(2)))
        val action = single(plan(medicine, reminder.copy(time = ReminderTime(540), amount = "2"), listOf(event)))
        assertEquals(ReminderEditAction.Refresh(event.copy(amount = "2")), action)
    }

    @Test
    fun weekdayRemovedDropsEventToday() {
        val event = openEvent(11, at(8, 0))
        val action = single(plan(medicine, reminder.copy(days = listOf(DayOfWeek.MONDAY)), listOf(event)))
        assertEquals(ReminderEditAction.Drop(event), action)
    }

    @Test
    fun manuallyRescheduledEventKeptWhenWeekdayRemoved() {
        val event = openEvent(11, at(10, 0))
        val action = single(plan(medicine, reminder.copy(days = listOf(DayOfWeek.MONDAY)), listOf(event)))
        assertEquals(ReminderEditAction.Keep(event), action)
    }

    @Test
    fun cyclePauseDayDropsEventToday() {
        val event = openEvent(11, at(8, 0))
        // Cycle of one active and one pause day that started yesterday: today is a pause day
        val newReminder = reminder.copy(consecutiveDays = 1, pauseDays = 1, cycleStartDay = today.minusDays(1))
        assertEquals(ReminderEditAction.Drop(event), single(plan(medicine, newReminder, listOf(event))))
    }

    @Test
    fun inactiveReminderDropsEventToday() {
        val event = openEvent(11, at(8, 0))
        assertEquals(ReminderEditAction.Drop(event), single(plan(medicine, reminder.copy(active = false), listOf(event))))
    }

    @Test
    fun manuallyRescheduledEventDroppedOnDeactivate() {
        val event = openEvent(11, at(10, 0))
        assertEquals(ReminderEditAction.Drop(event), single(plan(medicine, reminder.copy(active = false), listOf(event))))
    }

    @Test
    fun deletedReminderDropsOpenEvents() {
        val events = listOf(openEvent(11, at(8, 0)), openEvent(12, at(10, 0)))
        assertEquals(events.map { ReminderEditAction.Drop(it) }, plan(medicine, null, events))
    }

    @Test
    fun deletedMedicineDropsOpenEvents() {
        val events = listOf(openEvent(11, at(8, 0)), openEvent(12, at(10, 0)))
        assertEquals(events.map { ReminderEditAction.Drop(it) }, plan(null, null, events))
    }

    @Test
    fun manualEventOnFutureDayDroppedOnDelete() {
        val event = openEvent(11, at(8, 0, today.plusDays(1)))
        assertEquals(ReminderEditAction.Drop(event), single(plan(medicine, null, listOf(event))))
    }

    @Test
    fun yesterdaysRaisedEventIsNotTouched() {
        val event = openEvent(11, at(8, 0, today.minusDays(1)))
        assertEquals(emptyList(), plan(medicine, reminder.copy(time = ReminderTime(540), amount = "2"), listOf(event)))
        assertEquals(emptyList(), plan(null, null, listOf(event)))
    }

    @Test
    fun weekendModeShiftedEventCountsAsScheduled() {
        // Saturday, weekend mode moves the 08:00 dose to 10:00
        today = LocalDate.EPOCH.plusDays(2)
        now = at(10, 30)
        preferencesFlow.value = UserPreferences.default().copy(
            weekendMode = true,
            weekendStartTime = LocalTime.of(0, 0),
            weekendEndTime = LocalTime.of(10, 0),
            weekendDays = setOf(DayOfWeek.SATURDAY.value.toString(), DayOfWeek.SUNDAY.value.toString())
        )
        val event = openEvent(11, at(10, 0))
        val action = single(plan(medicine, reminder.copy(amount = "2"), listOf(event)))
        assertEquals(ReminderEditAction.Refresh(event.copy(amount = "2")), action)
    }

    @Test
    fun linkedDelayChangedMovesLinkedEvent() {
        now = at(9, 10)
        val linked = Reminder.default().copy(id = 2, medicineRelId = 1, amount = "1", linkedReminderId = 1, time = ReminderTime(60, isDuration = true))
        val source = openEvent(10, at(8, 0), status = ReminderEvent.ReminderStatus.TAKEN).copy(processedTimestamp = at(8, 0))
        val event = openEvent(12, at(9, 0), linked)
        val actions = plan(medicine, linked.copy(time = ReminderTime(30, isDuration = true)), listOf(event), listOf(source, event), linked)
        assertEquals(ReminderEditAction.Retime(event.copy(remindedTimestamp = at(8, 30))), single(actions))
    }

    @Test
    fun intervalChangedMovesRaisedEventToLastResolvedPlusNewInterval() {
        val interval = reminder.copy(intervalStart = at(6, 0, today.minusDays(1)), time = ReminderTime(120, isDuration = true))
        val resolved = openEvent(10, at(6, 0), interval, ReminderEvent.ReminderStatus.TAKEN)
        val event = openEvent(11, at(8, 0), interval)
        val schedulingEvents = listOf(resolved, event)

        val later = plan(medicine, interval.copy(time = ReminderTime(180, isDuration = true)), listOf(event), schedulingEvents, interval)
        assertEquals(ReminderEditAction.Drop(event), single(later))

        val earlier = plan(medicine, interval.copy(time = ReminderTime(60, isDuration = true)), listOf(event), schedulingEvents, interval)
        assertEquals(ReminderEditAction.Retime(event.copy(remindedTimestamp = at(7, 0))), single(earlier))
    }

    @Test
    fun windowedIntervalChangedMovesRaisedEvent() {
        val windowed = reminder.copy(
            windowedInterval = true,
            intervalStartTimeOfDay = LocalTime.of(6, 0),
            intervalEndTimeOfDay = LocalTime.of(22, 0),
            time = ReminderTime(120, isDuration = true)
        )
        val resolved = openEvent(10, at(6, 0), windowed, ReminderEvent.ReminderStatus.TAKEN)
        val event = openEvent(11, at(8, 0), windowed)
        val schedulingEvents = listOf(resolved, event)

        val later = plan(medicine, windowed.copy(time = ReminderTime(180, isDuration = true)), listOf(event), schedulingEvents, windowed)
        assertEquals(ReminderEditAction.Drop(event), single(later))

        val earlier = plan(medicine, windowed.copy(time = ReminderTime(60, isDuration = true)), listOf(event), schedulingEvents, windowed)
        assertEquals(ReminderEditAction.Retime(event.copy(remindedTimestamp = at(7, 0))), single(earlier))
    }

    @Test
    fun newEventAndRefreshedEventUseSameSnapshotFields() {
        val tagged = medicine.copy(color = 0x654321, useColor = true, iconId = 3, tags = listOf(Tag("Evening", 2)), cannotBeSkipped = true)
        val variable = reminder.copy(amount = "3", variableAmount = true)
        val built = runBlocking { buildReminderEvent(at(8, 0).epochSecond, tagged, variable, Mockito.mock(), 0) { "" } }
        val stripped = built.copy(medicineName = "", amount = "", color = 0, useColor = false, iconId = 0, tags = emptyList(), askForAmount = false, cannotBeSkipped = false)
        assertEquals(built, stripped.withSnapshotFields(tagged, variable))
    }
}
