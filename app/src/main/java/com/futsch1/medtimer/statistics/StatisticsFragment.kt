package com.futsch1.medtimer.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.R
import com.futsch1.medtimer.remindertable.ReminderTableFragment
import com.futsch1.medtimer.statistics.ActiveStatisticsFragment.StatisticFragmentType
import com.google.android.material.chip.ChipGroup

class StatisticsFragment : Fragment() {
    private var timeSpinner: Spinner? = null

    private var chartsFragment: ChartsFragment? = null
    private var analysisDays: AnalysisDays? = null
    private var activeStatisticsFragment: ActiveStatisticsFragment? = null
    private var optionsMenu: OptionsMenu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        analysisDays = AnalysisDays(requireContext())
        activeStatisticsFragment = ActiveStatisticsFragment(requireContext())

        chartsFragment = ChartsFragment()
        chartsFragment!!.setDays(analysisDays!!.days)

        optionsMenu = OptionsMenu(
            this,
            ViewModelProvider(this)[MedicineViewModel::class.java],
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

        loadActiveFragment(activeStatisticsFragment!!.activeFragment)

        requireActivity().addMenuProvider(optionsMenu!!, getViewLifecycleOwner())

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
        if (optionsMenu != null) {
            optionsMenu!!.onDestroy()
        }
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
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds: MutableList<Int?>? ->
            if (!checkedIds!!.isEmpty()) {
                val checkedId: Int = checkedIds[0]!!
                if (R.id.chartChip == checkedId) {
                    loadActiveFragment(StatisticFragmentType.CHARTS)
                } else if (R.id.tableChip == checkedId) {
                    loadActiveFragment(StatisticFragmentType.TABLE)
                } else {
                    loadActiveFragment(StatisticFragmentType.CALENDAR)
                }
            }
        }
        chipGroup.check(
            when (activeStatisticsFragment!!.activeFragment) {
                StatisticFragmentType.TABLE -> R.id.tableChip
                StatisticFragmentType.CALENDAR -> R.id.calendarChip
                else -> R.id.chartChip
            }
        )
    }

    private fun loadActiveFragment(fragmentType: StatisticFragmentType) {
        val fragment = when (fragmentType) {
            StatisticFragmentType.TABLE -> ReminderTableFragment()
            StatisticFragmentType.CALENDAR -> CalendarFragment()
            else -> chartsFragment
        }!!
        val transaction = getChildFragmentManager().beginTransaction()
        transaction.replace(R.id.container, fragment)
        try {
            transaction.commit()
            activeStatisticsFragment!!.activeFragment = fragmentType
            checkTimeSpinnerVisibility()
        } catch (_: IllegalStateException) {
            // Intentionally empty
        }
    }

    private fun checkTimeSpinnerVisibility() {
        if (activeStatisticsFragment!!.activeFragment == StatisticFragmentType.CHARTS) {
            timeSpinner!!.visibility = View.VISIBLE
        } else {
            timeSpinner!!.visibility = View.INVISIBLE
        }
    }

    private fun setupTimeSpinner() {
        timeSpinner!!.setSelection(analysisDays!!.position)
        timeSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != analysisDays!!.position) {
                    analysisDays!!.position = position

                    chartsFragment!!.setDays(analysisDays!!.days)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Intentionally empty
            }
        }
    }
}