package com.futsch1.medtimer.overview

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class NextReminders @SuppressLint("WrongViewCast") constructor(
    parentFragment: Fragment,
    private val medicineViewModel: MedicineViewModel,
    private val dataSource: PreferencesDataSource,
    private val medicineRepository: MedicineRepository,
    private val reminderEventRepository: ReminderEventRepository
) {
    private var reminderEvents: List<ReminderEvent>? = null
    private var medicines: List<Medicine>? = null

    init {
        setupScheduleObservers(parentFragment)
    }

    private fun setupScheduleObservers(parentFragment: Fragment) {
        parentFragment.viewLifecycleOwner.lifecycleScope.launch {
            reminderEventRepository.getAllFlow(
                Instant.now().toEpochMilli() / 1000 - 33 * 24 * 60 * 60,
                ReminderEvent.allStatusValues
            ).collect { reminderEvents ->
                changedReminderEvents(reminderEvents)
            }
        }
        parentFragment.viewLifecycleOwner.lifecycleScope.launch {
            medicineRepository.getAllFlow().collect { medicines ->
                changedMedicines(medicines)
            }
        }
    }

    private fun changedReminderEvents(reminderEvents: List<ReminderEvent>) {
        this.reminderEvents = reminderEvents
        calculateSchedule()
    }

    private fun changedMedicines(medicines: List<Medicine>) {
        this.medicines = medicines
        calculateSchedule()
    }

    private fun calculateSchedule() {
        val medicines = medicines ?: return
        val reminderEvents = reminderEvents ?: return

        val scheduler = ReminderScheduler(object : TimeAccess {
            override fun systemZone(): ZoneId = ZoneId.systemDefault()
            override fun localDate(): LocalDate = LocalDate.now()
            override fun now(): Instant = Instant.now()
        }, dataSource)

        val reminders: List<ScheduledReminder> = scheduler.schedule(
            medicines, reminderEvents
        )
        medicineViewModel.setScheduledReminders(reminders)
    }
}
