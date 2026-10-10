package com.futsch1.medtimer.feature.ui.medicine

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SetupGuidanceTest {
    @Test
    fun `should offer adding a medicine when none exist`() {
        assertEquals(SetupGuidance.ADD_MEDICINE, setupGuidance(emptyList()))
    }

    @Test
    fun `should not nag on the medicine list when a medicine exists without reminders`() {
        assertEquals(SetupGuidance.NONE, setupGuidance(listOf(medicine(id = 1))))
    }

    @Test
    fun `should offer adding a reminder for the selected medicine when it has none`() {
        assertEquals(
            SetupGuidance.ADD_REMINDER,
            setupGuidance(listOf(medicine(id = 1)), selectedMedicineId = 1)
        )
    }

    @Test
    fun `should hide setup guidance when a valid schedule has nothing due on the selected day`() {
        val selectedDay = LocalDate.of(2026, 10, 13)
        val scheduledReminder = Reminder.default().copy(
            medicineRelId = 1,
            time = com.futsch1.medtimer.core.domain.model.ReminderTime(LocalTime.of(8, 0)),
            days = listOf(DayOfWeek.MONDAY)
        )
        val scheduledMedicine = medicine(id = 1).copy(reminders = listOf(scheduledReminder))

        assertFalse(selectedDay.dayOfWeek in scheduledReminder.days)
        assertEquals(SetupGuidance.NONE, setupGuidance(listOf(scheduledMedicine), selectedMedicineId = 1))
    }

    @Test
    fun `should hide selected medicine guidance when its reminder is added`() {
        val medicine = medicine(id = 1)
        val withReminder = medicine.copy(reminders = listOf(Reminder.default().copy(medicineRelId = 1)))

        assertEquals(SetupGuidance.ADD_REMINDER, setupGuidance(listOf(medicine), selectedMedicineId = 1))
        assertEquals(SetupGuidance.NONE, setupGuidance(listOf(withReminder), selectedMedicineId = 1))
    }

    private fun medicine(id: Int) = Medicine.default().copy(id = id, name = "Medicine A")
}
