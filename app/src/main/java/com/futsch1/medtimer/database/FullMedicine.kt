package com.futsch1.medtimer.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.google.gson.annotations.Expose

class FullMedicine {
    @JvmField
    @Embedded
    @Expose
    var medicine: Medicine = Medicine()

    @JvmField
    @Relation(parentColumn = "medicineId", entityColumn = "tagId", associateBy = Junction(MedicineToTag::class))
    @Expose
    var tags: List<Tag> = listOf()

    @JvmField
    @Relation(parentColumn = "medicineId", entityColumn = "medicineRelId")
    @Expose
    var reminders: List<Reminder> = listOf()

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) return false
        val that = other as FullMedicine
        return medicine == that.medicine && reminders == that.reminders && tags == that.tags
    }

    override fun hashCode(): Int {
        var result = medicine.hashCode()
        result += reminders.hashCode()
        result += tags.hashCode()
        return result
    }

    val isOutOfStock: Boolean
        get() = this.isStockManagementActive && reminders.stream()
            .anyMatch { reminder: Reminder -> reminder.reminderType == Reminder.ReminderType.OUT_OF_STOCK && reminder.outOfStockThreshold >= medicine.amount }

    val isStockManagementActive: Boolean
        get() = (medicine.amount != 0.0 || hasStockReminder())

    private fun hasStockReminder(): Boolean {
        return reminders.stream().anyMatch { reminder: Reminder? -> reminder!!.reminderType == Reminder.ReminderType.OUT_OF_STOCK }
    }
}
