package com.futsch1.medtimer.new_overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.R
import java.time.LocalDate

class NewOverviewFragment : Fragment() {

    private lateinit var optionsMenu: OptionsMenu
    private lateinit var daySelector: DaySelector
    private lateinit var overviewViewModel: OverviewViewModel
    private lateinit var fragmentOverview: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentOverview = inflater.inflate(R.layout.fragment_new_overview, container, false)

        val medicineViewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
        overviewViewModel = ViewModelProvider(this, OverviewViewModelFactory(requireActivity().application, medicineViewModel))[OverviewViewModel::class.java]
        NextReminders(this, medicineViewModel)

        daySelector = DaySelector(requireContext(), fragmentOverview.findViewById(R.id.overviewWeek)) { day -> daySelected(day) }

        optionsMenu = OptionsMenu(
            this,
            medicineViewModel,
            fragmentOverview, false
        )
        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner())

        setupReminders()

        return fragmentOverview
    }

    private fun setupReminders() {
        val reminders: RecyclerView = fragmentOverview.findViewById(R.id.reminders)
        val adapter = RemindersViewAdapter(RemindersViewAdapter.OverviewEventDiff())
        reminders.setAdapter(adapter)
        reminders.setLayoutManager(LinearLayoutManager(fragmentOverview.context))

        overviewViewModel.overviewEvents.observe(getViewLifecycleOwner(), adapter::submitList)
    }

    fun daySelected(date: LocalDate) {
        overviewViewModel.day = date
    }
}