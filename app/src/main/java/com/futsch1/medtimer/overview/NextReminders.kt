package com.futsch1.medtimer.overview

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.allStatusValues
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class NextReminders @SuppressLint("WrongViewCast") constructor(
    parentFragment: Fragment,
    private val medicineViewModel: MedicineViewModel
) {

    private lateinit var reminderEvents: List<ReminderEvent>
    private lateinit var fullMedicines: List<FullMedicine>

    init {
        setupScheduleObservers(parentFragment)
    }

    private fun setupScheduleObservers(parentFragment: Fragment) {
        medicineViewModel.medicineRepository.getLiveReminderEvents(
            Instant.now().toEpochMilli() / 1000 - 33 * 24 * 60 * 60,
            allStatusValues
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
        }, PreferenceManager.getDefaultSharedPreferences(medicineViewModel.getApplication()))

        val reminders: List<ScheduledReminder> = scheduler.schedule(
            fullMedicines, reminderEvents
        )
        medicineViewModel.setScheduledReminders(reminders)
    }
}
