package com.futsch1.medtimer.feature.ui.overview.manualDose

import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.domain.model.ManualDose
import com.futsch1.medtimer.core.domain.model.ManualDoses
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.PersistentData
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stubbing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ManualDoseViewModelTest {
    private companion object {
        const val MEDICINE_A = "Medicine A"
        const val TWO_TABLETS = "2 tablets"
    }

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
    fun `recent dose with deleted medicine does not retain stale ID`() = runTest {
        val persistentData = PersistentData.default().copy(
            lastManualDoses = ManualDoses("""[{"medicineLabel":"$MEDICINE_A","medicineId":42,"amount":"$TWO_TABLETS"}]"""),
        )
        whenever(persistentDataDataSource.data).thenReturn(MutableStateFlow(persistentData))
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

        val dose = viewModel.state.first().lastCustomDoses.single()
        val selectedDateTime = LocalDateTime.of(2025, 3, 8, 9, 15)
        viewModel.setSelectedDateTime(selectedDateTime)
        assertNull(dose.medicineId)
        var result: ManualDoseSaveResult? = null
        viewModel.logRecentDose(dose) { result = it }

        assertNotNull(result?.event)
        assertEquals(
            selectedDateTime.toInstant(ZoneId.systemDefault().rules.getOffset(selectedDateTime)),
            result?.event?.remindedTimestamp,
        )
        assertEquals(selectedDateTime.toLocalDate(), result?.day)
        verify(persistentDataDataSource).addManualDose(ManualDose(MEDICINE_A, null, TWO_TABLETS))
    }

    @Test
    fun `recent dose is saved after event creation completes`() = runTest {
        val createGate = CompletableDeferred<ReminderEvent>()
        val delayedRepository = object : ReminderEventRepository by reminderEventRepository {
            override suspend fun create(reminderEvent: ReminderEvent): ReminderEvent = createGate.await()
        }
        val viewModel = ManualDoseViewModel(
            medicineRepository = mock<MedicineRepository>().also {
                whenever(it.getAllFlow()).thenReturn(flowOf(emptyList()))
            },
            reminderRepository = mock(),
            reminderEventRepository = delayedRepository,
            persistentDataDataSource = persistentDataDataSource,
            commandBus = mock<ReminderCommandBus>(),
            timeFormatter = mock<TimeFormatter>(),
        )
        viewModel.selectMedicineEntry(ManualDoseMedicineEntry(MEDICINE_A, null))
        viewModel.selectAmount(TWO_TABLETS)

        var result: ManualDoseSaveResult? = null
        viewModel.selectTimeAndLog(LocalTime.now()) { result = it }

        verify(persistentDataDataSource, never()).addManualDose(any())
        createGate.complete(ReminderEvent.default())

        assertNotNull(result?.event)
        verify(persistentDataDataSource).addManualDose(ManualDose(MEDICINE_A, null, TWO_TABLETS))
    }

    @Test
    fun `successful stock change updates event metadata`() = runTest {
        val commandBus = mock<ReminderCommandBus>().also {
            whenever(it.processStockHandling(any(), any(), any())).thenReturn(8.0)
        }
        val medicine = Medicine.default().copy(name = MEDICINE_A, id = 1, amount = 10.0)
        val viewModel = ManualDoseViewModel(
            medicineRepository = mock<MedicineRepository>().also {
                whenever(it.getAllFlow()).thenReturn(flowOf(emptyList()))
            },
            reminderRepository = mock(),
            reminderEventRepository = reminderEventRepository,
            persistentDataDataSource = persistentDataDataSource,
            commandBus = commandBus,
            timeFormatter = mock<TimeFormatter>(),
        )
        viewModel.selectMedicineEntry(ManualDoseMedicineEntry(MEDICINE_A, medicine))
        viewModel.selectAmount(TWO_TABLETS)
        var result: ManualDoseSaveResult? = null
        viewModel.selectTimeAndLog(LocalTime.now()) { result = it }

        assertNotNull(result?.event)
        verify(commandBus).processStockHandling(any(), any(), any())
        val savedEvent = argumentCaptor<ReminderEvent>()
        verify(reminderEventRepository).update(savedEvent.capture())
        assertTrue(savedEvent.firstValue.stockHandled)
        assertEquals(8.0, savedEvent.firstValue.stockAfter, 0.0)
    }

    @Test
    fun `should log a manual dose at the timestamp captured when the dialog opened`() = runTest {
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
        val selectedDateTime = LocalDateTime.of(2025, 3, 8, 9, 15)
        viewModel.setSelectedDateTime(selectedDateTime)

        assertFalse(viewModel.selectMedicineEntry(ManualDoseMedicineEntry("  ", null)))
        assertTrue(viewModel.selectMedicineEntry(ManualDoseMedicineEntry(MEDICINE_A, null)))
        viewModel.selectAmount("  $TWO_TABLETS  ")
        var result: ManualDoseSaveResult? = null
        viewModel.selectTimeAndLog(null) { result = it }

        val eventCaptor = argumentCaptor<ReminderEvent>()
        verify(reminderEventRepository).create(eventCaptor.capture())
        val event = eventCaptor.firstValue
        assertNotNull(result?.event)
        assertEquals(MEDICINE_A, event.medicineName)
        assertEquals(TWO_TABLETS, event.amount)
        assertEquals(-1, event.reminderId)
        assertEquals(
            selectedDateTime.toInstant(ZoneId.systemDefault().rules.getOffset(selectedDateTime)),
            event.remindedTimestamp,
        )
        assertEquals(selectedDateTime.toLocalDate(), result?.day)
        assertEquals(event.remindedTimestamp, event.processedTimestamp)
        verify(persistentDataDataSource).addManualDose(ManualDose(MEDICINE_A, null, TWO_TABLETS))
    }

    @Test
    fun `alternate time keeps the date captured when the dialog opened`() = runTest {
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
        val selectedDateTime = LocalDateTime.of(2025, 3, 8, 9, 15)
        val selectedTime = LocalTime.of(14, 30)
        viewModel.setSelectedDateTime(selectedDateTime)
        viewModel.selectMedicineEntry(ManualDoseMedicineEntry(MEDICINE_A, null))

        var result: ManualDoseSaveResult? = null
        viewModel.selectTimeAndLog(selectedTime) { result = it }

        val event = argumentCaptor<ReminderEvent>().also { verify(reminderEventRepository).create(it.capture()) }.firstValue
        val expectedDateTime = selectedDateTime.with(selectedTime)
        assertEquals(
            expectedDateTime.toInstant(ZoneId.systemDefault().rules.getOffset(expectedDateTime)),
            event.remindedTimestamp,
        )
        assertEquals(expectedDateTime.toLocalDate(), result?.day)
    }
}
