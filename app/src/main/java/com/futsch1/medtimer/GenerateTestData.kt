package com.futsch1.medtimer

import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import java.time.Instant
import java.time.LocalDate

class GenerateTestData(private val viewModel: MedicineViewModel) {
    fun generateTestMedicine() {
        val testReminderOmega3 = TestReminderTimeBased("1", 9 * 60, 1, 0, "")
        val testMedicines = arrayOf(
            TestMedicine(
                "Omega 3 (EPA/DHA 500mg)", null, 1, 10, arrayOf(
                    testReminderOmega3,
                    TestReminderLinked("1", 9 * 60 + 30, testReminderOmega3)
                )
            ),
            TestMedicine(
                "B12 (500µg)", -0x750000, 2, 50, arrayOf(
                    TestReminderTimeBased("2", 7 * 60, 1, 0, "")
                )
            ),
            TestMedicine(
                "Ginseng (200mg)", -0x6f1170, 3, 0, arrayOf(
                    TestReminderTimeBased("1", 9 * 60, 1, 0, "before breakfast")
                )
            ),
            TestMedicine(
                "Selen (200 µg)", null, 0, 0, arrayOf(
                    TestReminderTimeBased("2", 9 * 60, 1, 0, ""),
                    TestReminderIntervalBased("1", 36 * 60)
                )
            )
        )

        for (testMedicine in testMedicines) {
            val medicine = testMedicine.toMedicine()
            val medicineId = viewModel.insertMedicine(medicine)
            for (testReminder in testMedicine.reminders) {
                testReminder.id = viewModel.insertReminder(testReminder.toReminder(medicineId))
            }
        }
    }

    // Record, intentionally empty
    @JvmRecord
    private data class TestMedicine(
        val name: String,
        val color: Int?,
        val iconId: Int,
        val stock: Int,
        val reminders: Array<TestReminder>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TestMedicine

            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        fun toMedicine(): Medicine {
            val medicine = Medicine(name)
            if (color != null) {
                medicine.useColor = true
                medicine.color = color
            }
            if (stock > 0) {
                medicine.medicationAmount = stock
                medicine.medicationAmountReminderThreshold = stock / 2
                medicine.medicationStockReminder = Medicine.MedicationStockReminder.ONCE
            }
            return medicine
        }
    }

    private abstract class TestReminder {
        abstract fun toReminder(medicineId: Int): Reminder
        var id: Int = -1
    }

    private class TestReminderTimeBased(
        val amount: String, val time: Int, val consecutiveDays: Int,
        val pauseDays: Int,
        val instructions: String
    ) : TestReminder() {
        override fun toReminder(medicineId: Int): Reminder {
            val reminder = Reminder(medicineId)
            reminder.amount = amount
            reminder.timeInMinutes = time
            reminder.consecutiveDays = consecutiveDays
            reminder.pauseDays = pauseDays
            reminder.instructions = instructions
            reminder.cycleStartDay = LocalDate.now().toEpochDay()
            return reminder
        }
    }

    private class TestReminderLinked(
        val amount: String, val time: Int, val sourceReminder: TestReminder
    ) : TestReminder() {
        override fun toReminder(medicineId: Int): Reminder {
            val reminder = Reminder(medicineId)
            reminder.amount = amount
            reminder.timeInMinutes = time
            reminder.linkedReminderId = sourceReminder.id
            return reminder
        }
    }

    private class TestReminderIntervalBased(
        val amount: String, val time: Int
    ) : TestReminder() {
        override fun toReminder(medicineId: Int): Reminder {
            val reminder = Reminder(medicineId)
            reminder.amount = amount
            reminder.timeInMinutes = time
            reminder.intervalStartsFromProcessed = true
            reminder.intervalStart = Instant.now().epochSecond - 60
            return reminder
        }

    }
}
