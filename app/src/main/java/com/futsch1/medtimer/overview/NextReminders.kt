package com.futsch1.medtimer.overview

import android.annotation.SuppressLint
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
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
    private val expandNextReminders: MaterialButton =
        fragmentView.findViewById(R.id.expandNextReminders)
    private lateinit var reminderEvents: List<ReminderEvent>
    private var nextRemindersExpanded =
        fragmentView.context.resources.configuration.orientation == ORIENTATION_LANDSCAPE
    private lateinit var fullMedicines: List<FullMedicine>

    init {
        val recyclerView = fragmentView.findViewById<RecyclerView>(R.id.nextReminders)
        recyclerView.adapter = nextRemindersViewAdapter
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        nextRemindersViewAdapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                recyclerView.scrollToPosition(0)
            }
        })

        medicineViewModel.scheduledReminders.observe(parentFragment.viewLifecycleOwner) { scheduledReminders: List<ScheduledReminder>? ->
            this.updatedNextReminders(
                scheduledReminders
            )
        }

        setupScheduleObservers(parentFragment)
        setupExpandNextReminders(fragmentView)
    }

    private fun updatedNextReminders(scheduledReminders: List<ScheduledReminder>?) {
        if (scheduledReminders.isNullOrEmpty()) {
            expandNextReminders.visibility = View.GONE

            nextRemindersViewAdapter.submitList(ArrayList())
        } else {
            if (expandNextReminders.context.resources.configuration.orientation == ORIENTATION_LANDSCAPE) {
                expandNextReminders.visibility = View.GONE
            } else {
                expandNextReminders.visibility = View.VISIBLE
            }

            nextRemindersViewAdapter.submitList(
                if (nextRemindersExpanded) scheduledReminders else scheduledReminders.subList(0, 1)
            )
        }
    }

    private fun setupScheduleObservers(parentFragment: Fragment) {
        medicineViewModel.medicineRepository.getLiveReminderEvents(
            0,
            Instant.now().toEpochMilli() / 1000 - 48 * 60 * 60,
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

    private fun setupExpandNextReminders(fragmentView: View) {
        val nextRemindersCard = fragmentView.findViewById<MaterialCardView>(R.id.nextRemindersCard)
        expandNextReminders.setOnClickListener {
            nextRemindersExpanded = !nextRemindersExpanded
            adaptUIToNextRemindersExpandedState(expandNextReminders, nextRemindersCard)
            updatedNextReminders(medicineViewModel.scheduledReminders.value)
        }

        adaptUIToNextRemindersExpandedState(expandNextReminders, nextRemindersCard)
    }

    private fun changedReminderEvents(reminderEvents: List<ReminderEvent>) {
        this.reminderEvents = reminderEvents
        calculateSchedule()
    }

    private fun changedMedicines(fullMedicine: List<FullMedicine>) {
        this.fullMedicines = fullMedicine
        calculateSchedule()
    }

    private fun adaptUIToNextRemindersExpandedState(
        expandNextReminders: MaterialButton,
        nextRemindersCard: MaterialCardView
    ) {
        val layoutParams = nextRemindersCard.layoutParams as LinearLayout.LayoutParams
        if (nextRemindersExpanded) {
            expandNextReminders.setIconResource(R.drawable.chevron_up)
            layoutParams.height = 0
            layoutParams.weight = 1f
            nextRemindersCard.layoutParams = layoutParams
        } else {
            expandNextReminders.setIconResource(R.drawable.chevron_down)
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams.weight = 0f
            nextRemindersCard.layoutParams = layoutParams
        }
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

        val reminders = scheduler.schedule(
            fullMedicines, reminderEvents
        )
        medicineViewModel.setScheduledReminders(reminders)
    }
}
