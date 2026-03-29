package com.futsch1.medtimer.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.androidplot.pie.PieChart
import com.androidplot.xy.XYPlot
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ChartsFragment : Fragment() {
    @Inject
    @Dispatcher(MedTimerDispatchers.Main)
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    @Dispatcher(MedTimerDispatchers.Default)
    lateinit var backgroundDispatcher: CoroutineDispatcher

    @Inject
    lateinit var medicineRepository: MedicineRepository

    @Inject
    lateinit var medicinePerDayChartFactory: MedicinePerDayChart.Factory

    @Inject
    lateinit var statisticsProvider: StatisticsProvider

    private lateinit var takenSkippedChartView: PieChart
    private lateinit var takenSkippedTotalChartView: PieChart
    private lateinit var medicinesPerDayChartView: XYPlot
    private var takenSkippedChart: TakenSkippedChart? = null
    private var takenSkippedTotalChart: TakenSkippedChart? = null
    private var medicinesPerDayChart: MedicinePerDayChart? = null

    private var currentDays = 0
    private val daysFlow = MutableSharedFlow<Int>(replay = 1)

    companion object {
        private const val DAYS_BUNDLE_KEY = "days"

        fun newInstance(days: Int) = ChartsFragment().apply {
            arguments = Bundle().apply { putInt(DAYS_BUNDLE_KEY, days) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentDays = arguments?.getInt(DAYS_BUNDLE_KEY) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val statisticsView = inflater.inflate(R.layout.fragment_charts, container, false)

        medicinesPerDayChartView = statisticsView.findViewById(R.id.medicinesPerDayChart)
        takenSkippedChartView = statisticsView.findViewById(R.id.takenSkippedChart)
        takenSkippedTotalChartView =
            statisticsView.findViewById(R.id.takenSkippedChartTotal)

        setupTakenSkippedCharts()
        lifecycleScope.launch(backgroundDispatcher) {
            setupMedicinesPerDayChart()
            daysFlow.collect { days ->
                populateStatistics(days)
            }
        }

        return statisticsView
    }

    private fun setupTakenSkippedCharts() {
        this.takenSkippedChart = TakenSkippedChart(takenSkippedChartView, requireContext())
        this.takenSkippedTotalChart =
            TakenSkippedChart(takenSkippedTotalChartView, requireContext())
    }

    private fun setupMedicinesPerDayChart() {
        try {
            this.medicinesPerDayChart = medicinePerDayChartFactory.create(
                medicinesPerDayChartView,
                medicineRepository.medicines
            )
        } catch (_: IllegalStateException) {
            // Intentionally ignored
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { daysFlow.emit(currentDays) }
    }

    private suspend fun populateStatistics(days: Int) {
        val statisticsProvider = this.statisticsProvider
        val medicinesPerDayChart = medicinesPerDayChart ?: return
        val takenSkippedChart = takenSkippedChart ?: return
        val takenSkippedTotalChart = takenSkippedTotalChart ?: return

        try {
            val series = statisticsProvider.getLastDaysReminders(days)
            withContext(mainDispatcher) {
                medicinesPerDayChart.updateData(series)
            }

            val data = statisticsProvider.getTakenSkippedData(days)
            withContext(mainDispatcher) {
                takenSkippedChart.updateData(
                    data.taken,
                    data.skipped,
                    days
                )
            }

            val dataTotal = statisticsProvider.getTakenSkippedData(0)
            withContext(mainDispatcher) {
                takenSkippedTotalChart.updateData(
                    dataTotal.taken,
                    dataTotal.skipped,
                    0
                )
            }
        } catch (_: IllegalStateException) {
            // Intentionally ignored
        }
    }

    fun setDays(days: Int) {
        this.currentDays = days
        lifecycleScope.launch { daysFlow.emit(days) }
    }
}