package com.futsch1.medtimer.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.R
import com.futsch1.medtimer.model.StatisticFragment
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.remindertable.ReminderTableFragment
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    private val medicineViewModel: MedicineViewModel by viewModels()
    private lateinit var timeSpinner: Spinner
    private lateinit var chartsFragment: ChartsFragment
    private lateinit var analysisDays: AnalysisDays
    private lateinit var optionsMenu: OptionsMenu

    @Inject
    lateinit var persistentDataDataSource: PersistentDataDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        analysisDays = AnalysisDays(requireContext())

        chartsFragment = ChartsFragment.newInstance(analysisDays.days)

        optionsMenu = OptionsMenu(
            this,
            medicineViewModel,
            NavHostFragment.findNavController(this), true
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val statisticsView = inflater.inflate(R.layout.fragment_statistics, container, false)

        timeSpinner = statisticsView.findViewById(R.id.timeSpinner)

        setupTimeSpinner()

        setupFragmentButtons(statisticsView)

        loadActiveFragment(persistentDataDataSource.data.value.activeStatisticsFragment)

        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner())

        return statisticsView
    }

    override fun onPause() {
        try {
            requireActivity().supportFragmentManager.executePendingTransactions()
        } catch (_: IllegalStateException) {
            // Intentionally empty
        } catch (_: IllegalArgumentException) {
            // Intentionally empty
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        optionsMenu.onDestroy()

        try {
            requireActivity().supportFragmentManager.executePendingTransactions()
        } catch (_: IllegalStateException) {
            // Intentionally empty
        } catch (_: IllegalArgumentException) {
            // Intentionally empty
        }
    }

    private fun setupFragmentButtons(statisticsView: View) {
        val chipGroup = statisticsView.findViewById<ChipGroup>(R.id.analysisView)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds: List<Int> ->
            if (checkedIds.isNotEmpty()) {
                val checkedId: Int = checkedIds[0]
                if (R.id.chartChip == checkedId) {
                    loadActiveFragment(StatisticFragment.CHARTS)
                } else if (R.id.tableChip == checkedId) {
                    loadActiveFragment(StatisticFragment.TABLE)
                } else {
                    loadActiveFragment(StatisticFragment.CALENDAR)
                }
            }
        }
        chipGroup.check(
            when (persistentDataDataSource.data.value.activeStatisticsFragment) {
                StatisticFragment.TABLE -> R.id.tableChip
                StatisticFragment.CALENDAR -> R.id.calendarChip
                else -> R.id.chartChip
            }
        )
    }

    private fun loadActiveFragment(fragmentType: StatisticFragment) {
        val fragment = when (fragmentType) {
            StatisticFragment.TABLE -> ReminderTableFragment()
            StatisticFragment.CALENDAR -> CalendarFragment()
            else -> chartsFragment
        }
        val transaction = getChildFragmentManager().beginTransaction()
        transaction.replace(R.id.container, fragment)
        try {
            transaction.commit()
            persistentDataDataSource.setActiveStatisticsFragment(fragmentType)
            checkTimeSpinnerVisibility()
        } catch (_: IllegalStateException) {
            // Intentionally empty
        }
    }

    private fun checkTimeSpinnerVisibility() {
        if (persistentDataDataSource.data.value.activeStatisticsFragment == StatisticFragment.CHARTS) {
            timeSpinner.visibility = View.VISIBLE
        } else {
            timeSpinner.visibility = View.INVISIBLE
        }
    }

    private fun setupTimeSpinner() {
        timeSpinner.setSelection(analysisDays.position)
        timeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position != analysisDays.position) {
                    analysisDays.position = position

                    chartsFragment.setDays(analysisDays.days)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Intentionally empty
            }
        }
    }
}