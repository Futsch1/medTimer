package com.futsch1.medtimer.new_overview

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.R
import com.futsch1.medtimer.overview.ManualDose
import java.time.LocalDate

class NewOverviewFragment : Fragment() {

    private lateinit var medicineViewModel: MedicineViewModel
    private lateinit var optionsMenu: OptionsMenu
    private lateinit var daySelector: DaySelector
    private lateinit var overviewViewModel: OverviewViewModel
    private lateinit var fragmentOverview: View
    private lateinit var thread: HandlerThread

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentOverview = inflater.inflate(R.layout.fragment_new_overview, container, false)

        medicineViewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
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

        thread = HandlerThread("LogManualDose")
        thread.start()
        setupLogManualDose()
        FilterToggleGroup(fragmentOverview.findViewById(R.id.filterButtons), overviewViewModel)

        return fragmentOverview
    }

    private fun setupReminders() {
        val reminders: RecyclerView = fragmentOverview.findViewById(R.id.reminders)
        val adapter = RemindersViewAdapter(RemindersViewAdapter.OverviewEventDiff(), viewLifecycleOwner.lifecycleScope)
        reminders.setAdapter(adapter)
        reminders.setLayoutManager(LinearLayoutManager(fragmentOverview.context))

        overviewViewModel.overviewEvents.observe(getViewLifecycleOwner(), adapter::submitList)
    }

    private fun setupLogManualDose() {
        val logManualDose = fragmentOverview.findViewById<Button>(R.id.logManualDose)
        logManualDose.setOnClickListener { _: View? ->
            val handler = Handler(thread.getLooper())
            // Run the setup of the drop down in a separate thread to access the database
            handler.post {
                ManualDose(requireContext(), medicineViewModel.medicineRepository, this.requireActivity()).logManualDose()
            }
        }
    }

    fun daySelected(date: LocalDate) {
        overviewViewModel.day = date
    }


    override fun onDestroy() {
        super.onDestroy()
        thread.quit()
        optionsMenu.onDestroy()
    }
}