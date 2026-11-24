package com.futsch1.medtimer.feature.ui.overview

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.ui.overview.actions.ActionsVisitor
import com.futsch1.medtimer.feature.ui.overview.actions.Button
import com.futsch1.medtimer.feature.ui.overview.actions.ReminderEventCreator
import com.futsch1.medtimer.feature.ui.overview.actions.RescheduleRequest
import com.futsch1.medtimer.feature.ui.overview.actions.Rescheduler
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class RescheduleTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val reminderEventRepository: ReminderEventRepository = mock()
    private val reminderEventCreator: ReminderEventCreator = mock()
    private val preferencesDataSource: PreferencesDataSource = mock()
    private val persistentDataDataSource: PersistentDataDataSource = mock()
    private val commandBus: ReminderCommandBus = mock()

    private val remindedAt = LocalDateTime.of(2026, 3, 10, 8, 15).atZone(ZoneId.systemDefault()).toInstant()
    private val reminderEvent = ReminderEvent.default().copy(
        reminderEventId = 42,
        reminderId = 1,
        status = ReminderEvent.ReminderStatus.RAISED,
        remindedTimestamp = remindedAt,
    )

    private lateinit var viewModel: RescheduleViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(preferencesDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default()))
        whenever(persistentDataDataSource.getPendingLocationSnoozes()).thenReturn(emptyList())
        val rescheduler = Rescheduler(reminderEventRepository, reminderEventCreator, preferencesDataSource, commandBus, CoroutineScope(testDispatcher))
        viewModel = RescheduleViewModel(rescheduler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pastReminderEvent() = PastReminderEvent(preferencesDataSource, persistentDataDataSource, mock<TimeAccess>(), reminderEvent)

    @Test
    fun theRescheduleActionRequestsATimeStartingAtTheRemindedTime() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val actionsVisitor = ActionsVisitor(
            reminderEventRepository,
            mock<MedicineRepository>(),
            mock<ReminderRepository>(),
            activity,
            reminderEventCreator,
            commandBus,
            CoroutineScope(testDispatcher),
        )
        actionsVisitor.onRescheduleRequested = viewModel::request

        actionsVisitor.startVisit(Button.RESCHEDULE).use {
            actionsVisitor.visit(pastReminderEvent())
        }
        shadowOf(activity.mainLooper).idle()

        val request = assertNotNull(viewModel.request.value)
        assertEquals(LocalTime.of(8, 15), request.initialTime)
        verifyBlocking(reminderEventRepository, never()) { upsertMany(any()) }
    }

    @Test
    fun confirmingMovesTheEventToThePickedTimeOnTheSameDay() {
        val expected = LocalDateTime.of(2026, 3, 10, 21, 40).atZone(ZoneId.systemDefault()).toInstant()
        val rescheduled = reminderEvent.copy(remindedTimestamp = expected)
        runBlocking { whenever(reminderEventRepository.upsertMany(any())).thenReturn(listOf(rescheduled)) }
        viewModel.request(RescheduleRequest(emptyList(), listOf(pastReminderEvent())))

        viewModel.confirm(LocalTime.of(21, 40))

        val captor = argumentCaptor<List<ReminderEvent>>()
        verifyBlocking(reminderEventRepository) { upsertMany(captor.capture()) }
        assertEquals(listOf(rescheduled), captor.firstValue)
        verifyBlocking(commandBus) { scheduleNextNotification() }
        assertNull(viewModel.request.value)
    }

    @Test
    fun cancellingLeavesTheEventAlone() {
        viewModel.request(RescheduleRequest(emptyList(), listOf(pastReminderEvent())))

        viewModel.cancel()

        verifyBlocking(reminderEventRepository, never()) { upsertMany(any()) }
        assertNull(viewModel.request.value)
    }
}
