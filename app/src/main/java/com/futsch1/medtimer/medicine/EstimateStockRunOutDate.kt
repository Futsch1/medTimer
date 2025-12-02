package com.futsch1.medtimer.medicine

import androidx.preference.PreferenceManager
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.SchedulingSimulator
import java.time.LocalDate
import java.time.ZoneId

fun estimateStockRunOutDate(medicineViewModel: MedicineViewModel, medicineId: Int, currentAmount: Double? = null): LocalDate? {
    val fullMedicine = medicineViewModel.medicineRepository.getMedicine(medicineId)
    if (currentAmount != null) {
        fullMedicine.medicine.amount = currentAmount
    }
    val recentReminders =
        medicineViewModel.medicineRepository.getReminderEventsForScheduling(listOf(fullMedicine)).filter { it.status != ReminderEvent.ReminderStatus.RAISED }
    val schedulingSimulator = SchedulingSimulator(listOf(fullMedicine), recentReminders, object : TimeAccess {
        override fun systemZone(): ZoneId {
            return ZoneId.systemDefault()
        }

        override fun localDate(): LocalDate {
            return LocalDate.now()
        }
    }, PreferenceManager.getDefaultSharedPreferences(medicineViewModel.getApplication()))
    val endDate = LocalDate.now().plusDays(365)
    var runOutDate: LocalDate? = null

    schedulingSimulator.simulate { _, scheduledDate: LocalDate, amount: Double ->
        if (amount == 0.0) {
            runOutDate = scheduledDate
        }
        scheduledDate <= endDate && amount != 0.0
    }

    return runOutDate
}
