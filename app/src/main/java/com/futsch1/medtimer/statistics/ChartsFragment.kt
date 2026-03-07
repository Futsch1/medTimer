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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChartsFragment : Fragment() {
    private lateinit var medicineRepository: MedicineRepository

    private lateinit var takenSkippedChartView: PieChart
    private lateinit var takenSkippedTotalChartView: PieChart
    private lateinit var medicinesPerDayChartView: XYPlot
    private var takenSkippedChart: TakenSkippedChart? = null
    private var takenSkippedTotalChart: TakenSkippedChart? = null
    private var medicinesPerDayChart: MedicinePerDayChart? = null

    private var days = 0

    companion object {
        private const val DAYS_BUNDLE_KEY = "days"

        fun newInstance(days: Int) = ChartsFragment().apply {
            arguments = Bundle().apply { putInt(DAYS_BUNDLE_KEY, days) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        days = arguments?.getInt(DAYS_BUNDLE_KEY) ?: 0
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
        medicineRepository = MedicineRepository(requireActivity().application)

        setupTakenSkippedCharts()
        lifecycleScope.launch(Dispatchers.Default) {
            this@ChartsFragment.setupMedicinesPerDayChart()
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
            this.medicinesPerDayChart = MedicinePerDayChart(
                medicinesPerDayChartView,
                requireContext(),
                medicineRepository.medicines
            )
        } catch (_: IllegalStateException) {
            // Intentionally ignored
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch(Dispatchers.Default) {
            this@ChartsFragment.populateStatistics()
        }
    }

    private suspend fun populateStatistics() {
        val statisticsProvider = StatisticsProvider(medicineRepository)

        // We assume these are already initialized by this point and want the app to fail if not initialized
        val medicinesPerDayChart = medicinesPerDayChart!!
        val takenSkippedChart = takenSkippedChart!!
        val takenSkippedTotalChart = takenSkippedTotalChart!!

        try {
            val series = statisticsProvider.getLastDaysReminders(days)
            withContext(Dispatchers.Main) {
                medicinesPerDayChart.updateData(series)
            }

            val data = statisticsProvider.getTakenSkippedData(days)
            withContext(Dispatchers.Main) {
                takenSkippedChart.updateData(
                    data.taken,
                    data.skipped,
                    days
                )
            }

            val dataTotal = statisticsProvider.getTakenSkippedData(0)
            withContext(Dispatchers.Main) {
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
        this.days = days

        lifecycleScope.launch(Dispatchers.Default) {
            this@ChartsFragment.populateStatistics()
        }
    }
}