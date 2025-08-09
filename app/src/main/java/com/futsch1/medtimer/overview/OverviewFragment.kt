package com.futsch1.medtimer.overview

import FilterToggleGroup
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.stream.IntStream.range

class OverviewFragment : Fragment() {

    private lateinit var adapter: RemindersViewAdapter
    private lateinit var reminders: RecyclerView
    private lateinit var medicineViewModel: MedicineViewModel
    private lateinit var optionsMenu: OptionsMenu
    private lateinit var daySelector: DaySelector
    private lateinit var overviewViewModel: OverviewViewModel
    private lateinit var fragmentOverview: FragmentSwipeLayout
    private lateinit var thread: HandlerThread
    private var onceStable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        medicineViewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
        overviewViewModel = ViewModelProvider(this, OverviewViewModelFactory(requireActivity().application, medicineViewModel))[OverviewViewModel::class.java]

        optionsMenu = OptionsMenu(
            this,
            medicineViewModel,
            this.findNavController(), false
        )

        thread = HandlerThread("LogManualDose")
        thread.start()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        NextReminders(this, medicineViewModel)

        fragmentOverview = inflater.inflate(R.layout.fragment_overview, container, false) as FragmentSwipeLayout

        daySelector = DaySelector(requireContext(), fragmentOverview.findViewById(R.id.overviewWeek)) { day -> daySelected(day) }

        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner())

        setupReminders()

        setupLogManualDose()
        FilterToggleGroup(fragmentOverview.findViewById(R.id.filterButtons), overviewViewModel, requireContext().getSharedPreferences("medtimer.data", 0))

        fragmentOverview.onSwipeListener = OverviewOnSwipeListener()

        return fragmentOverview
    }

    inner class OverviewOnSwipeListener : OnSwipeListener {
        override fun onSwipeLeft() {
            daySelector.selectNextDay()
        }

        override fun onSwipeRight() {
            daySelector.selectPreviousDay()
        }

        override fun onSwipeUp() {
            // Not required
        }

        override fun onSwipeDown() {
            // Not required
        }
    }

    private fun setupReminders() {
        reminders = fragmentOverview.findViewById(R.id.reminders)
        adapter = RemindersViewAdapter(RemindersViewAdapter.OverviewEventDiff(), requireActivity())
        reminders.setAdapter(adapter)
        reminders.setLayoutManager(LinearLayoutManager(fragmentOverview.context))

        overviewViewModel.overviewEvents.observe(getViewLifecycleOwner()) { list ->
            adapter.submitList(list) {
                reminders.post {
                    if (!onceStable && overviewViewModel.initialized) {
                        onceStable = true
                        scrollToCurrentTimeItem()
                    }
                    updatePositions()
                }
            }
        }
    }

    private fun scrollToCurrentTimeItem() {
        adapter.currentList.forEachIndexed { index, listItem ->
            if (Instant.ofEpochSecond(listItem.timestamp).atZone(ZoneId.systemDefault()).toLocalTime() >= LocalTime.now()) {
                reminders.scrollToPosition(index)
                return
            }
        }
    }

    private fun updatePositions() {
        for (position in range(0, adapter.itemCount)) {
            val positionEnum = when (position) {
                0 -> if (adapter.itemCount > 1) EventPosition.FIRST else EventPosition.ONLY
                adapter.itemCount - 1 -> EventPosition.LAST
                else -> EventPosition.MIDDLE
            }
            val viewAdapter = reminders.findViewHolderForAdapterPosition(position)
            viewAdapter?.let { (it as ReminderViewHolder).setBarsVisibility(positionEnum) }
        }
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
        if (this::thread.isInitialized) {
            thread.quit()
        }
        if (this::optionsMenu.isInitialized) {
            optionsMenu.onDestroy()
        }
    }
}