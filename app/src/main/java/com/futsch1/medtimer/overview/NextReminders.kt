package com.futsch1.medtimer.overview

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.allStatusValues
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class NextReminders @SuppressLint("WrongViewCast") constructor(
    parentFragment: Fragment,
    private val medicineViewModel: MedicineViewModel
) {
    private val applicationContext = parentFragment.requireContext().applicationContext
    private  var reminderEvents: List<ReminderEvent>? = null
    private  var fullMedicines: List<FullMedicine>? = null

    init {
        setupScheduleObservers(parentFragment)
    }

    private fun setupScheduleObservers(parentFragment: Fragment) {
        parentFragment.viewLifecycleOwner.lifecycleScope.launch {
            medicineViewModel.medicineRepository.getReminderEventsFlow(
                Instant.now().toEpochMilli() / 1000 - 33 * 24 * 60 * 60,
                allStatusValues
            ).collect { reminderEvents ->
                changedReminderEvents(reminderEvents)
            }
        }
        parentFragment.viewLifecycleOwner.lifecycleScope.launch {
            medicineViewModel.medicineRepository.medicinesFlow.collect { fullMedicines ->
                changedMedicines(fullMedicines)
            }
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
        val fullMedicines = fullMedicines ?: return
        val reminderEvents = reminderEvents ?: return

        val scheduler = ReminderScheduler(object : TimeAccess {
            override fun systemZone(): ZoneId = ZoneId.systemDefault()
            override fun localDate(): LocalDate = LocalDate.now()
            override fun now(): Instant = Instant.now()
        }, PreferenceManager.getDefaultSharedPreferences(applicationContext))

        val reminders: List<ScheduledReminder> = scheduler.schedule(
            fullMedicines, reminderEvents
        )
        medicineViewModel.setScheduledReminders(reminders)
    }
}
