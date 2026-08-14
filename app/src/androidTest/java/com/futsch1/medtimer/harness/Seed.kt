package com.futsch1.medtimer.harness

import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderTime
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import kotlin.time.Duration

/**
 * A test's starting data, written straight into the repositories.
 *
 * Arranging a medicine and its reminders through the editor costs roughly fifteen seconds per test
 * on CI's emulator, and the editor is not what most tests are about. The tests that own a creation
 * flow still drive it - see docs/guidelines/testing.md - everything else arranges here.
 *
 * What lands in the database is what the editor writes: the same defaults [Medicine] and [Reminder]
 * start from, and the same fields the new-reminder dialogs fill in. The app reacts to the write the
 * way it reacts to the editor's, so scheduling and the overview follow on their own.
 */
class Seed(
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val timeAccess: TimeAccess
) {

    /** Creates [name] with whatever [block] adds to it, and answers its id. */
    fun medicine(name: String, block: MedicineSeed.() -> Unit = {}): Int = runBlocking {
        val seed = MedicineSeed(timeAccess).apply(block)

        val medicineId = medicineRepository.create(
            seed.medicine.copy(name = name, sortOrder = medicineRepository.getHighestSortOrder())
        )
        create(seed.reminders, medicineId)

        medicineId
    }

    /** Adds reminders to a medicine that is already there - for state a test builds up in steps. */
    fun remindersOf(medicineId: Int, block: MedicineSeed.() -> Unit) = runBlocking {
        create(MedicineSeed(timeAccess).apply(block).reminders, medicineId)
    }

    /** One write: the app reschedules off each, so a medicine added a reminder at a time is catchable half-built. */
    private suspend fun create(reminders: List<Pair<Reminder, Int?>>, medicineId: Int) {
        val reminderIds = reminderRepository.createMany(
            reminders.map { (reminder, _) -> reminder.copy(medicineRelId = medicineId) }
        )

        // A link holds the position of the reminder it follows, whose id only exists once written.
        val linked = reminders.mapIndexedNotNull { position, (reminder, linkedToPosition) ->
            linkedToPosition?.let {
                reminder.copy(
                    id = reminderIds[position],
                    medicineRelId = medicineId,
                    linkedReminderId = reminderIds[it]
                )
            }
        }
        if (linked.isNotEmpty()) {
            reminderRepository.updateMany(linked)
        }
    }

    /** The medicine under construction: its stock, its appearance and the reminders on it. */
    class MedicineSeed internal constructor(private val timeAccess: TimeAccess) {
        internal var medicine: Medicine = Medicine.default()
        internal val reminders = mutableListOf<Pair<Reminder, Int?>>()

        fun stock(amount: Double, unit: String = "", refillSize: Double = 0.0) {
            medicine = medicine.copy(amount = amount, unit = unit, refillSize = refillSize)
        }

        fun cannotBeSkipped() {
            medicine = medicine.copy(cannotBeSkipped = true)
        }

        /** A time based reminder, the kind the time based card creates. */
        fun reminder(
            amount: String,
            time: LocalTime = ReminderTime(ReminderTime.DEFAULT_TIME).getLocalTime(),
            variableAmount: Boolean = false,
            automaticallyTaken: Boolean = false,
            active: Boolean = true
        ) {
            add(
                newReminder().copy(
                    amount = amount,
                    time = ReminderTime(time),
                    variableAmount = variableAmount,
                    automaticallyTaken = automaticallyTaken,
                    active = active
                )
            )
        }

        /** A continuous interval reminder, counting from now the way the dialog starts it. */
        fun intervalReminder(amount: String, interval: Duration) {
            add(
                newReminder().copy(
                    amount = amount,
                    time = ReminderTime(interval.inWholeMinutes.toInt(), isDuration = true),
                    intervalStart = timeAccess.now()
                )
            )
        }

        /** A reminder [after] the one added before it, the way the linked reminder dialog creates it. */
        fun linkedReminder(amount: String, after: Duration) {
            val linkedToPosition = reminders.size - 1
            add(
                newReminder().copy(
                    amount = amount,
                    time = ReminderTime(after.inWholeMinutes.toInt(), isDuration = true)
                ),
                linkedToPosition
            )
        }

        /** Fires once when the stock falls to [threshold]. */
        fun stockReminder(threshold: Double) {
            add(
                newReminder().copy(
                    outOfStockThreshold = threshold,
                    outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE
                )
            )
        }

        /** Fires at [at] on every day the stock sits below [threshold]. */
        fun dailyStockReminder(threshold: Double, at: LocalTime) {
            add(
                newReminder().copy(
                    time = ReminderTime(at),
                    outOfStockThreshold = threshold,
                    outOfStockReminderType = Reminder.OutOfStockReminderType.DAILY
                )
            )
        }

        private fun add(reminder: Reminder, linkedToPosition: Int? = null) {
            reminders += reminder to linkedToPosition
        }

        private fun newReminder() = Reminder.default().copy(
            createdTime = timeAccess.now(),
            cycleStartDay = timeAccess.localDate().plusDays(1),
            instructions = ""
        )
    }
}
