package com.futsch1.medtimer.database;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.google.gson.annotations.Expose;

import java.util.List;

public class FullMedicine {
    @Embedded
    @Expose
    public Medicine medicine;
    @Relation(
            parentColumn = "medicineId",
            entityColumn = "tagId",
            associateBy = @Junction(MedicineToTag.class)
    )
    @Expose
    public List<Tag> tags;
    @Relation(
            parentColumn = "medicineId",
            entityColumn = "medicineRelId"
    )
    @Expose
    public List<Reminder> reminders;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FullMedicine that = (FullMedicine) o;
        return medicine.equals(that.medicine) && reminders.equals(that.reminders) && tags.equals(that.tags);
    }

    @Override
    public int hashCode() {
        int result = medicine.hashCode();
        result += reminders.hashCode();
        result += tags.hashCode();
        return result;
    }

    public boolean isOutOfStock() {
        return isStockManagementActive() && reminders.stream().anyMatch(reminder -> reminder.getReminderType() == Reminder.ReminderType.OUT_OF_STOCK && reminder.getOutOfStockThreshold() >= medicine.amount);
    }

    public boolean isStockManagementActive() {
        return (medicine.amount != 0 || hasStockReminder());
    }

    private boolean hasStockReminder() {
        return reminders.stream().anyMatch(reminder -> reminder.getReminderType() == Reminder.ReminderType.OUT_OF_STOCK);
    }
}
