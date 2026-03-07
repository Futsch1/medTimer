package com.futsch1.medtimer.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.google.gson.annotations.Expose
import java.util.Objects

class FullMedicine {
    @Embedded
    @Expose
    var medicine: Medicine = Medicine()

    @Relation(parentColumn = "medicineId", entityColumn = "tagId", associateBy = Junction(MedicineToTag::class))
    @Expose
    var tags: List<Tag> = listOf()

    @Relation(parentColumn = "medicineId", entityColumn = "medicineRelId")
    @Expose
    var reminders: MutableList<Reminder> = mutableListOf()

    override fun equals(other: Any?): Boolean {
        if (other !is FullMedicine) return false
        return medicine == other.medicine && reminders == other.reminders && tags == other.tags
    }

    override fun hashCode(): Int {
        return Objects.hash(medicine, reminders, tags)
    }

    val isOutOfStock: Boolean
        get() = this.isStockManagementActive && reminders.any { reminder -> reminder.reminderType == Reminder.ReminderType.OUT_OF_STOCK && reminder.outOfStockThreshold >= medicine.amount }

    val isStockManagementActive: Boolean
        get() = (medicine.amount != 0.0 || hasStockReminder())

    private fun hasStockReminder(): Boolean {
        return reminders.any { reminder -> reminder.reminderType == Reminder.ReminderType.OUT_OF_STOCK }
    }
}
