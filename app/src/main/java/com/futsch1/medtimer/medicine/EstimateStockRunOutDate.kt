package com.futsch1.medtimer.medicine

import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.SchedulingSimulator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

suspend fun estimateStockRunOutDate(
    medicineRepository: MedicineRepository,
    medicineId: Int,
    currentAmount: Double? = null,
    preferencesDataSource: PreferencesDataSource
): LocalDate? {
    val fullMedicine = medicineRepository.getMedicine(medicineId) ?: return null

    if (currentAmount != null) {
        fullMedicine.medicine.amount = currentAmount
    }
    val recentReminders =
        medicineRepository.getReminderEventsForScheduling(listOf(fullMedicine)).filter { it.status != ReminderEventEntity.ReminderStatus.RAISED }
    // TODO: extract the time access into a re-usable class/object
    val schedulingSimulator = SchedulingSimulator(listOf(fullMedicine), recentReminders, object : TimeAccess {
        override fun systemZone(): ZoneId = ZoneId.systemDefault()
        override fun localDate(): LocalDate = LocalDate.now()
        override fun now(): Instant = Instant.now()
    }, preferencesDataSource)
    val endDate = LocalDate.now().plusDays(365 * 2)
    var runOutDate: LocalDate? = null

    schedulingSimulator.simulate { _, scheduledDate: LocalDate, amount: Double ->
        if (amount == 0.0) {
            runOutDate = scheduledDate
        }
        scheduledDate <= endDate && amount != 0.0
    }

    return runOutDate
}
