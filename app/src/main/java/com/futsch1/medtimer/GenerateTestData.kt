package com.futsch1.medtimer

import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.database.TagRepository
import com.futsch1.medtimer.database.toModel.toModel
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Tag
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.LinkedList

class GenerateTestData @AssistedInject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val tagRepository: TagRepository,
    @Assisted val withEvents: Boolean
) {

    @AssistedFactory
    interface Factory {
        fun create(withEvents: Boolean): GenerateTestData
    }

    suspend fun generateTestMedicine() {
        val testReminderOmega3 = TestReminderTimeBased("1", 9 * 60, 1, 0, "")
        val testMedicines = arrayOf(
            TestMedicine(
                "Omega 3 (EPA/DHA 500mg)", null, 1, 10.0, arrayOf(
                    testReminderOmega3,
                    TestReminderLinked("1", 9 * 60 + 30, testReminderOmega3),
                    TestReminderOutOfStock(12.0),
                    TestReminderExpirationDate(10 * 60, 10)
                ), arrayOf("Supplements")
            ),
            TestMedicine(
                "B12 (500µg)", -0x750000, 2, 50.5, arrayOf(
                    TestReminderTimeBased("2", 7 * 60, 1, 0, "")
                ), arrayOf("Vitamins", "Energy")
            ),
            TestMedicine(
                "Ginseng (200mg)", -0x6f1170, 3, 0.0, arrayOf(
                    TestReminderTimeBased("1", 9 * 60, 1, 0, "before breakfast", withEvents)
                ), arrayOf("Energy")
            ),
            TestMedicine(
                "Selen (200 µg)", null, 0, 0.0, arrayOf(
                    TestReminderTimeBased("2", 9 * 60, 1, 0, "", withEvents),
                    TestReminderIntervalBased("1", 36 * 60)
                ), arrayOf("Supplements")
            )
        )

        var sortOrder = 1.0
        for (testMedicine in testMedicines) {
            val medicine = testMedicine.toMedicine(sortOrder++)
            val medicineId = medicineRepository.create(medicine).toInt()
            for (testReminder in testMedicine.reminders) {
                testReminder.id =
                    reminderRepository.create(testReminder.toReminder(medicineId).toModel())
                        .toInt()
            }
            for (tag in testMedicine.tags) {
                val tagId = tagRepository.create(Tag(tag, 0))
                tagRepository.addMedicineTag(medicineId, tagId.toInt())
            }
            if (withEvents) {
                // Insert reminder events for every single day back from today
                val reminderEvents: MutableList<ReminderEventEntity> = LinkedList()
                for (testReminder in testMedicine.reminders) {
                    val today = Instant.now()

                    for (i in 1..1000) {
                        val reminderEvent = ReminderEventEntity()
                        reminderEvent.reminderId = testReminder.id
                        reminderEvent.remindedTimestamp = today.minus(Period.ofDays(i)).epochSecond
                        reminderEvent.processedTimestamp = reminderEvent.remindedTimestamp
                        reminderEvent.status = ReminderEventEntity.ReminderEntityStatus.TAKEN
                        reminderEvent.medicineName = testMedicine.name
                        reminderEvent.amount = testReminder.toReminder(0).amount
                        reminderEvent.notes = ""
                        reminderEvent.tags = testMedicine.tags.toList()
                        reminderEvents.add(reminderEvent)
                    }
                }

                reminderEventRepository.createAll(reminderEvents.map { it.toModel() })
            }
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

        fun toMedicine(sortOrder: Double): Medicine {
            return Medicine(
                name = name,
                id = 0,
                color = color ?: 0,
                useColor = color != null,
                notificationImportance = Medicine.NotificationImportance.DEFAULT,
                iconId = iconId,
                amount = stock,
                refillSize = if (stock > 0) 20.0 else 0.0,
                unit = if (stock > 0) "pills" else "",
                notes = if (stock > 0) "Some note\nabout this medicine" else "",
                showNotificationAsAlarm = false,
                productionDate = LocalDate.now(),
                expirationDate = LocalDate.now().plusDays(7),
                sortOrder = sortOrder,
                tags = emptyList(),
                reminders = reminders.map { it.toReminder(0).toModel() }
            )
        }
    }

    private abstract class TestReminder {
        abstract fun toReminder(medicineId: Int): ReminderEntity
        var id: Int = -1
    }

    private class TestReminderTimeBased(
        val amount: String, val time: Int, val consecutiveDays: Int,
        val pauseDays: Int,
        val instructions: String, val variableAmount: Boolean = false
    ) : TestReminder() {
        override fun toReminder(medicineId: Int): ReminderEntity {
            val reminder = ReminderEntity(medicineId)
            reminder.amount = amount
            reminder.timeInMinutes = time
            reminder.consecutiveDays = consecutiveDays
            reminder.pauseDays = pauseDays
            reminder.instructions = instructions
            reminder.cycleStartDay = LocalDate.now().toEpochDay()
            reminder.variableAmount = variableAmount
            return reminder
        }
    }

    private class TestReminderLinked(
        val amount: String, val time: Int, val sourceReminder: TestReminder
    ) : TestReminder() {
        override fun toReminder(medicineId: Int): ReminderEntity {
            val reminder = ReminderEntity(medicineId)
            reminder.amount = amount
            reminder.timeInMinutes = time
            reminder.linkedReminderId = sourceReminder.id
            return reminder
        }
    }

    private class TestReminderIntervalBased(
        val amount: String, val time: Int
    ) : TestReminder() {
        override fun toReminder(medicineId: Int): ReminderEntity {
            val reminder = ReminderEntity(medicineId)
            reminder.amount = amount
            reminder.timeInMinutes = time
            reminder.intervalStartsFromProcessed = true
            reminder.intervalStart = Instant.now().epochSecond - 60
            return reminder
        }
    }

    private class TestReminderOutOfStock(
        val threshold: Double
    ) : TestReminder() {
        override fun toReminder(medicineId: Int): ReminderEntity {
            val reminder = ReminderEntity(medicineId)
            reminder.outOfStockThreshold = threshold
            reminder.outOfStockReminderType = ReminderEntity.OutOfStockReminderType.DAILY
            return reminder
        }
    }

    private class TestReminderExpirationDate(
        val time: Int, val daysBefore: Long
    ) : TestReminder() {
        override fun toReminder(medicineId: Int): ReminderEntity {
            val reminder = ReminderEntity(medicineId)
            reminder.timeInMinutes = time
            reminder.periodStart = daysBefore
            reminder.expirationReminderType = ReminderEntity.ExpirationReminderType.DAILY
            return reminder
        }
    }
}
