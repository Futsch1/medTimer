package com.futsch1.medtimer.statistics

import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidplot.xy.SimpleXYSeries
import com.androidplot.xy.XYSeries
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.TimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.ceil

data class ChartsUiState(
    val series: List<SimpleXYSeries>,
    val seriesColors: List<Int>,
    val domainMin: Long,
    val domainMax: Long,
    val rangeMax: Number,
    val domainStep: Double,
    val takenPeriod: Long,
    val skippedPeriod: Long,
    val takenTotal: Long,
    val skippedTotal: Long,
    val days: Int
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val statisticsProvider: StatisticsProvider,
    private val medicineRepository: MedicineRepository,
    private val timeFormatter: TimeFormatter,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _days = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ChartsUiState?> = _days
        .flatMapLatest { days -> flow { emit(loadState(days)) } }
        .flowOn(ioDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), null)

    fun setDays(days: Int) {
        _days.value = days
    }

    private suspend fun loadState(days: Int): ChartsUiState {
        val series = statisticsProvider.getLastDaysReminders(days)
        val seriesColors = computeSeriesColors(series)

        val takenSkipped = statisticsProvider.getTakenSkippedData(days)
        val takenSkippedTotal = statisticsProvider.getTakenSkippedData(0)

        val domainMin = calculateMinDomain(series)
        val domainMax = calculateMaxDomain(series)
        val adjustedDomainMin = if (domainMax == domainMin) domainMin - 1 else domainMin
        val numDomains = domainMax - adjustedDomainMin + 1

        return ChartsUiState(
            series = series,
            seriesColors = seriesColors,
            domainMin = adjustedDomainMin,
            domainMax = domainMax,
            rangeMax = calculateMaxRange(series),
            domainStep = getDomainStepVal(numDomains),
            takenPeriod = takenSkipped.taken,
            skippedPeriod = takenSkipped.skipped,
            takenTotal = takenSkippedTotal.taken,
            skippedTotal = takenSkippedTotal.skipped,
            days = days
        )
    }

    private suspend fun computeSeriesColors(series: List<SimpleXYSeries>): List<Int> {
        val allMedicines = medicineRepository.getAll()
        var colorIndex = 0
        return series.map { xySeries ->
            allMedicines
                .firstOrNull { it.name == xySeries.title && it.useColor }
                ?.color
                ?: COLORS[colorIndex++ % COLORS.size]
        }
    }

    private fun calculateMaxRange(series: List<SimpleXYSeries>): Number {
        if (series.isEmpty()) return 0
        return (0 until series[0].size()).maxOfOrNull { x ->
            series.sumOf { it.getY(x).toLong() }
        } ?: 0
    }

    private fun calculateMinDomain(series: List<SimpleXYSeries>): Long {
        return if (series.isEmpty()) LocalDate.now().toEpochDay() - 1
        else series[0].getX(0).toLong()
    }

    private fun calculateMaxDomain(series: List<SimpleXYSeries>): Long {
        return if (series.isEmpty()) LocalDate.now().toEpochDay()
        else series[0].let { s: XYSeries -> s.getX(s.size() - 1).toLong() }
    }

    private fun getDomainStepVal(numDomains: Long): Double {
        return if (timeFormatter.daysSinceEpochToDateString(
                LocalDate.of(2024, 12, 31).toEpochDay()
            ).length > 8
        ) {
            ceil((numDomains / 5.0f).toDouble())
        } else {
            ceil((numDomains / 7.0f).toDouble())
        }
    }

    companion object {
        private val COLORS = intArrayOf(
            "#003f5c".toColorInt(),
            "#2f4b7c".toColorInt(),
            "#665191".toColorInt(),
            "#a05195".toColorInt(),
            "#d45087".toColorInt(),
            "#f95d6a".toColorInt(),
            "#ff7c43".toColorInt(),
            "#ffa600".toColorInt(),
            "#004c6d".toColorInt(),
            "#295d7d".toColorInt(),
            "#436f8e".toColorInt(),
            "#5b829f".toColorInt(),
            "#7295b0".toColorInt(),
            "#89a8c2".toColorInt(),
            "#a1bcd4".toColorInt(),
            "#b8d0e6".toColorInt(),
            "#d0e5f8".toColorInt()
        )
    }
}
