package com.futsch1.medtimer.feature.ui.statistics

import androidx.compose.ui.graphics.toArgb
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.PersistentData
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.domain.model.Tag
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.TagRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.core.ui.filter.TagEventFilter
import com.futsch1.medtimer.feature.ui.statistics.charts.ChartsPresenter
import com.futsch1.medtimer.feature.ui.statistics.table.ReminderTablePresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val statisticsProvider: StatisticsProvider = mock()
    private val calendarEventsProvider: CalendarEventsProvider = mock()
    private val medicineRepository: MedicineRepository = mock()
    private val reminderEventRepository: ReminderEventRepository = mock()
    private val tagRepository: TagRepository = mock()
    private val persistentDataDataSource: PersistentDataDataSource = mock()
    private val tagEventFilter = TagEventFilter()
    private val timeFormatter: TimeFormatter = mock()
    private val chartsPresenter = ChartsPresenter(timeFormatter)
    private val reminderTablePresenter = ReminderTablePresenter(timeFormatter)

    private val dataFlow = MutableStateFlow(PersistentData.default())
    private val eventsFlow = MutableStateFlow<List<ReminderEvent>>(emptyList())
    private val medicinesFlow = MutableStateFlow<List<Medicine>>(emptyList())

    private lateinit var viewModel: StatisticsScreenViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(persistentDataDataSource.data).thenReturn(dataFlow)
        whenever(reminderEventRepository.getAllFlow(any(), any())).thenReturn(eventsFlow)
        whenever(tagRepository.getAllFlow()).thenReturn(flowOf(emptyList()))
        whenever(timeFormatter.toDateTimeString(any<java.time.Instant>())).thenReturn("")

        whenever(statisticsProvider.aggregate(any(), any())).thenReturn(
            ChartsData(
                perDay = MedicinePerDayData(emptyList(), emptyList()),
                period = StatisticsProvider.TakenSkipped(0, 0),
                total = StatisticsProvider.TakenSkipped(0, 0),
            )
        )
        whenever(calendarEventsProvider.structuredEventsFlow(any(), any())).thenReturn(flowOf(emptyMap()))
        whenever(medicineRepository.getAllFlow()).thenReturn(medicinesFlow)

        viewModel = buildViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state reads activeView from persisted data`() {
        assertEquals(StatisticFragment.CHARTS, viewModel.uiState.value.activeView)
    }

    @Test
    fun `initial state reads analysisDays from persisted data`() {
        assertEquals(7, viewModel.uiState.value.analysisDays)
    }

    @Test
    fun `initial state with non-default persisted values`() {
        dataFlow.value = PersistentData.default().copy(
            activeStatisticsFragment = StatisticFragment.TABLE,
            analysisDays = 14,
        )
        val vm = buildViewModel()

        assertEquals(StatisticFragment.TABLE, vm.uiState.value.activeView)
        assertEquals(14, vm.uiState.value.analysisDays)
    }

    // ── onSelectView ──────────────────────────────────────────────────────────

    @Test
    fun `onSelectView updates the active view in state`() {
        viewModel.onSelectView(StatisticFragment.TABLE)

        assertEquals(StatisticFragment.TABLE, viewModel.uiState.value.activeView)
    }

    @Test
    fun `onSelectView persists the selected fragment`() {
        viewModel.onSelectView(StatisticFragment.CALENDAR)

        verify(persistentDataDataSource).setActiveStatisticsFragment(StatisticFragment.CALENDAR)
    }

    // ── onSelectRange ─────────────────────────────────────────────────────────

    @Test
    fun `onSelectRange is a no-op when the value is unchanged`() {
        viewModel.onSelectRange(7)   // same as PersistentData.default().analysisDays

        verify(persistentDataDataSource, never()).setAnalysisDays(any())
    }

    @Test
    fun `onSelectRange updates analysisDays state`() {
        viewModel.onSelectRange(14)

        assertEquals(14, viewModel.uiState.value.analysisDays)
    }

    @Test
    fun `onSelectRange persists the new value`() {
        viewModel.onSelectRange(30)

        verify(persistentDataDataSource).setAnalysisDays(30)
    }

    // ── table rows ────────────────────────────────────────────────────────────

    @Test
    fun `table rows are empty when repository emits no events`() {
        assertTrue(viewModel.uiState.value.tableRows.isEmpty())
    }

    @Test
    fun `table rows reflect reminder events emitted by the repository`() {
        val event = ReminderEvent.default().copy(
            status = ReminderEvent.ReminderStatus.TAKEN,
            medicineName = "Vitamin X",
        )
        eventsFlow.value = listOf(event)

        assertEquals(1, viewModel.uiState.value.tableRows.size)
    }

    @Test
    fun `table rows apply tag filter from persistent data`() {
        val taggedEvent = ReminderEvent.default().copy(
            status = ReminderEvent.ReminderStatus.TAKEN,
            medicineName = "Vitamin X",
            tags = listOf("Vitamins"),
        )
        val untaggedEvent = ReminderEvent.default().copy(
            status = ReminderEvent.ReminderStatus.TAKEN,
            medicineName = "Medicine A",
            tags = emptyList(),
        )
        eventsFlow.value = listOf(taggedEvent, untaggedEvent)

        // Activate tag filter for "Vitamins" (tag id = 1)
        whenever(tagRepository.getAllFlow()).thenReturn(flowOf(listOf(Tag("Vitamins", 1))))
        dataFlow.value = PersistentData.default().copy(filterTags = setOf("1"))
        val vm = buildViewModel()

        assertEquals(1, vm.uiState.value.tableRows.size)
        assertEquals("Vitamin X", vm.uiState.value.tableRows[0].cells[1].text)
    }

    // ── charts ────────────────────────────────────────────────────────────────

    @Test
    fun `charts recompute live when the medicine repository emits a custom color`() {
        // One series named "Vitamin X" so its color is driven by the medicine flow.
        whenever(statisticsProvider.aggregate(any(), any())).thenReturn(
            ChartsData(
                perDay = MedicinePerDayData(epochDays = listOf(0L), series = listOf(MedicineDaySeries("Vitamin X", listOf(1)))),
                period = StatisticsProvider.TakenSkipped(0, 0),
                total = StatisticsProvider.TakenSkipped(0, 0),
            )
        )
        val vm = buildViewModel()
        // No custom colors yet → the series falls back to the first palette slot.
        assertEquals(
            com.futsch1.medtimer.feature.ui.statistics.charts.ChartSeriesColors.PALETTE[0].toArgb(),
            vm.uiState.value.charts?.seriesColors?.first(),
        )

        medicinesFlow.value = listOf(Medicine.default().copy(name = "Vitamin X", color = 0x12345678, useColor = true))

        // The flow input recomputed the chart with no other trigger — the custom color now wins.
        assertEquals(0x12345678, vm.uiState.value.charts?.seriesColors?.first())
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildViewModel() = StatisticsScreenViewModel(
        statisticsProvider = statisticsProvider,
        chartsPresenter = chartsPresenter,
        reminderTablePresenter = reminderTablePresenter,
        calendarEventsProvider = calendarEventsProvider,
        medicineRepository = medicineRepository,
        reminderEventRepository = reminderEventRepository,
        tagRepository = tagRepository,
        persistentDataDataSource = persistentDataDataSource,
        tagEventFilter = tagEventFilter,
        ioDispatcher = testDispatcher,
    )
}
