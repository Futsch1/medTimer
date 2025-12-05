package com.futsch1.medtimer

import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.Tag
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.LinkedList

class GenerateTestData(private val viewModel: MedicineViewModel) {
    fun generateTestMedicine() {
        val testReminderOmega3 = TestReminderTimeBased("1", 9 * 60, 1, 0, "")
        val testMedicines = arrayOf(
            TestMedicine(
                "Omega 3 (EPA/DHA 500mg)", null, 1, 10.0, arrayOf(
                    testReminderOmega3,
                    TestReminderLinked("1", 9 * 60 + 30, testReminderOmega3)
                ), arrayOf("Supplements")
            ),
            TestMedicine(
                "B12 (500µg)", -0x750000, 2, 50.5, arrayOf(
                    TestReminderTimeBased("2", 7 * 60, 1, 0, "")
                ), arrayOf("Vitamins", "Energy")
            ),
            TestMedicine(
                "Ginseng (200mg)", -0x6f1170, 3, 0.0, arrayOf(
                    TestReminderTimeBased("1", 9 * 60, 1, 0, "before breakfast")
                ), arrayOf("Energy")
            ),
            TestMedicine(
                "Selen (200 µg)", null, 0, 0.0, arrayOf(
                    TestReminderTimeBased("2", 9 * 60, 1, 0, ""),
                    TestReminderIntervalBased("1", 36 * 60)
                ), arrayOf("Supplements")
            )
        )

        var sortOrder = 1.0
        for (testMedicine in testMedicines) {
            val medicine = testMedicine.toMedicine()
            medicine.sortOrder = sortOrder++
            val medicineId = viewModel.medicineRepository.insertMedicine(medicine).toInt()
            for (testReminder in testMedicine.reminders) {
                testReminder.id =
                    viewModel.medicineRepository.insertReminder(testReminder.toReminder(medicineId))
                        .toInt()
            }
            for (tag in testMedicine.tags) {
                val tagId = viewModel.medicineRepository.insertTag(Tag(tag))
                viewModel.medicineRepository.insertMedicineToTag(medicineId, tagId.toInt())
            }
            // Insert reminder events for every day back from today
            val reminderEvents: MutableList<ReminderEvent> = LinkedList()
            for (testReminder in testMedicine.reminders) {
                val today = Instant.now()

                for (i in 1..1000) {
                    val reminderEvent = ReminderEvent()
                    reminderEvent.reminderId = testReminder.id
                    reminderEvent.remindedTimestamp = today.minus(Period.ofDays(i)).epochSecond
                    reminderEvent.processedTimestamp = reminderEvent.remindedTimestamp
                    reminderEvent.status = ReminderEvent.ReminderStatus.TAKEN
                    reminderEvent.medicineName = testMedicine.name
                    reminderEvent.amount = testReminder.toReminder(0).amount
                    reminderEvent.notes = ""
                    reminderEvent.tags = testMedicine.tags.toList()
                    reminderEvents.add(reminderEvent)
                }
            }

            viewModel.medicineRepository.insertReminderEvents(reminderEvents)
        }
    }

    // Record, intentionally empty
    @JvmRecord
    private data class TestMedicine(
        val name: String,
        val color: Int?,
        val iconId: Int,
        val stock: Double,
        val reminders: Array<TestReminder>,
        val tags: Array<String>
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
            medicine.iconId = iconId
            if (stock > 0) {
                medicine.amount = stock
                medicine.outOfStockReminderThreshold = if (stock > 10) stock / 2 else stock * 2
                medicine.outOfStockReminder = Medicine.OutOfStockReminderType.ONCE
                medicine.unit = "pills"
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
