package com.futsch1.medtimer.database;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class MedicineWithReminders {
    @Relation(
            parentColumn = "medicineId",
            entityColumn = "medicineRelId"
    )
    public List<ReminderEntity> reminders;
    @Embedded
    public Medicine medicine;
}
