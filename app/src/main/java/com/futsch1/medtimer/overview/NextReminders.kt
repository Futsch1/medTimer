package com.futsch1.medtimer.overview

import android.annotation.SuppressLint
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.overview.NextRemindersViewAdapter.ScheduledReminderDiff
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class NextReminders @SuppressLint("WrongViewCast") constructor(
    fragmentView: View,
    parentFragment: Fragment,
    private val medicineViewModel: MedicineViewModel
) {
    private val nextRemindersViewAdapter =
        NextRemindersViewAdapter(ScheduledReminderDiff(), medicineViewModel)
    private lateinit var reminderEvents: List<ReminderEvent>
    private lateinit var fullMedicines: List<FullMedicine>

    init {
        val recyclerView = fragmentView.findViewById<RecyclerView>(R.id.nextReminders)
        recyclerView.adapter = nextRemindersViewAdapter
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        nextRemindersViewAdapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                recyclerView.scrollToPosition(0)
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                recyclerView.scrollToPosition(0)
            }
        })

        medicineViewModel.scheduledReminders.observe(parentFragment.viewLifecycleOwner, nextRemindersViewAdapter::submitList)

        setupScheduleObservers(parentFragment)
    }

    private fun setupScheduleObservers(parentFragment: Fragment) {
        medicineViewModel.medicineRepository.getLiveReminderEvents(
            Instant.now().toEpochMilli() / 1000 - 33 * 24 * 60 * 60,
            true
        )
            .observe(parentFragment.viewLifecycleOwner) { reminderEvents: List<ReminderEvent> ->
                this.changedReminderEvents(
                    reminderEvents
                )
            }
        medicineViewModel.medicineRepository.liveMedicines.observe(parentFragment.viewLifecycleOwner) { fullMedicines: List<FullMedicine> ->
            this.changedMedicines(
                fullMedicines
            )
        }
    }

    private fun changedReminderEvents(reminderEvents: List<ReminderEvent>) {
        this.reminderEvents = reminderEvents
        calculateSchedule()
    }

    private fun changedMedicines(fullMedicine: List<FullMedicine>) {
        this.fullMedicines = fullMedicine
        calculateSchedule()
    }

    private fun calculateSchedule() {
        if (!::fullMedicines.isInitialized || !::reminderEvents.isInitialized) {
            return
        }
        val scheduler = ReminderScheduler(object : TimeAccess {
            override fun systemZone(): ZoneId {
                return ZoneId.systemDefault()
            }

            override fun localDate(): LocalDate {
                return LocalDate.now()
            }
        })

        val reminders: List<ScheduledReminder> = scheduler.schedule(
            fullMedicines, reminderEvents
        )
        medicineViewModel.setScheduledReminders(reminders)
    }
}
