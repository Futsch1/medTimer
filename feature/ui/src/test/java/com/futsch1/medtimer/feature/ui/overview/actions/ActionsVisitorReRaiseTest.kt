package com.futsch1.medtimer.feature.ui.overview.actions

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.core.common.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
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
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.time.Instant

/**
 * Regression coverage for re-raising an event: it must update the existing [ReminderEvent] row back
 * to RAISED in place — at its original timestamp — rather than deleting the row. Deleting relied on
 * scheduling to recreate it, but scheduling (interval reminders in particular) only ever looks forward
 * from whatever event remains with the highest `remindedTimestamp`; if the re-raised occurrence wasn't
 * the last one of the day, the deleted slot was never recomputed and the event vanished for good.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class ActionsVisitorReRaiseTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val reminderEventRepository: ReminderEventRepository = mock()
    private val medicineRepository: MedicineRepository = mock()
    private val reminderRepository: ReminderRepository = mock()
    private val timePickerDialogFactory: TimePickerDialogFactory = mock()
    private val reminderEventCreator: ReminderEventCreator = mock()
    private val preferencesDataSource: PreferencesDataSource = mock()
    private val persistentDataDataSource: PersistentDataDataSource = mock()
    private val commandBus: ReminderCommandBus = mock()

    private lateinit var activity: FragmentActivity
    private lateinit var actionsVisitor: ActionsVisitor

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
        whenever(preferencesDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default()))
        whenever(persistentDataDataSource.getPendingLocationSnoozes()).thenReturn(emptyList())

        actionsVisitor = ActionsVisitor(
            reminderEventRepository,
            medicineRepository,
            reminderRepository,
            timePickerDialogFactory,
            activity,
            reminderEventCreator,
            preferencesDataSource,
            commandBus,
            CoroutineScope(testDispatcher)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reRaiseUpdatesTheEventInPlaceInsteadOfDeletingIt() {
        val reminderEvent = ReminderEvent.default().copy(
            reminderEventId = 42,
            reminderId = 1,
            status = ReminderEvent.ReminderStatus.TAKEN,
            remindedTimestamp = Instant.ofEpochSecond(720 * 60),
            processedTimestamp = Instant.ofEpochSecond(730 * 60),
            stockHandled = true,
        )
        val pastReminderEvent = PastReminderEvent(preferencesDataSource, persistentDataDataSource, reminderEvent)

        actionsVisitor.startVisit(Button.RERAISE).use {
            actionsVisitor.visit(pastReminderEvent)
        }
        shadowOf(activity.mainLooper).idle()

        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(activity.mainLooper).idle()

        verifyBlocking(reminderEventRepository, never()) { delete(any()) }
        verifyBlocking(reminderEventRepository) {
            update(
                reminderEvent.copy(
                    status = ReminderEvent.ReminderStatus.RAISED,
                    processedTimestamp = Instant.EPOCH,
                    stockHandled = false
                )
            )
        }
        verifyBlocking(commandBus) {
            showReminderNotification(argThat {
                reminderEventIds == listOf(reminderEvent.reminderEventId) &&
                        reminderIds == listOf(reminderEvent.reminderId)
            })
        }
    }
}
