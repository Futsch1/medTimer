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
    private lateinit var medicineViewModel: MedicineViewModel
    private lateinit var daySelector: DaySelector
    private lateinit var fragmentOverview: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentOverview = inflater.inflate(R.layout.fragment_new_overview, container, false)
        medicineViewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
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

        val reminderList: MutableList<OverviewEvent> = ArrayList()

        reminderList.add(OverviewEvent("Test", 0, 0, false))
        reminderList.add(OverviewEvent("Test 2", 0, 0, false))
        reminderList.add(OverviewEvent("Test 3", 0, 0, false))

        adapter.setData(reminderList)
        adapter.filter.filter("")
    }

    fun daySelected(date: LocalDate) {

    }
}