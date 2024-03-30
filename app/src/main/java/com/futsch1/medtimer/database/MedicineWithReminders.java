package com.futsch1.medtimer.database;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.google.gson.annotations.Expose;

import java.util.List;

public class MedicineWithReminders {
    @Relation(
            parentColumn = "medicineId",
            entityColumn = "medicineRelId"
    )
    @Expose
    public List<Reminder> reminders;
    @Embedded
    @Expose
    public Medicine medicine;
}
