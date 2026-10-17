package com.futsch1.medtimer.feature.ui.overview.manualDose

import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.domain.model.ManualDose
import com.futsch1.medtimer.core.domain.model.PersistentData
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ManualDoseViewModelTest {
    private val reminderEventRepository: ReminderEventRepository = mock()
    private val persistentDataDataSource: PersistentDataDataSource = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(persistentDataDataSource.data).thenReturn(MutableStateFlow(PersistentData.default()))
        stubbing(reminderEventRepository) {
            on { create(any()) } doAnswer { it.getArgument(0) }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should log a manual dose with the current timestamp`() = runTest {
        val viewModel = ManualDoseViewModel(
            medicineRepository = mock<MedicineRepository>().also {
                whenever(it.getAllFlow()).thenReturn(flowOf(emptyList()))
            },
            reminderRepository = mock(),
            reminderEventRepository = reminderEventRepository,
            persistentDataDataSource = persistentDataDataSource,
            commandBus = mock<ReminderCommandBus>(),
            timeFormatter = mock<TimeFormatter>(),
        )
        val beforeLog = Instant.now()

        viewModel.selectMedicineEntry(ManualDoseMedicineEntry("Medicine A", null))
        viewModel.selectAmount("2 tablets")
        viewModel.selectTimeAndLog(LocalTime.now())

        val afterLog = Instant.now()
        val eventCaptor = argumentCaptor<ReminderEvent>()
        verify(reminderEventRepository).create(eventCaptor.capture())
        val event = eventCaptor.firstValue
        assertEquals("Medicine A", event.medicineName)
        assertEquals("2 tablets", event.amount)
        assertEquals(-1, event.reminderId)
        assertTrue(event.remindedTimestamp >= beforeLog)
        assertTrue(event.remindedTimestamp <= afterLog)
        assertEquals(event.remindedTimestamp, event.processedTimestamp)
        verify(persistentDataDataSource).addManualDose(ManualDose("Medicine A", null, "2 tablets"))
    }
}
