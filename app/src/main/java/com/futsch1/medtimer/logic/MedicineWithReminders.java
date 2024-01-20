package com.futsch1.medtimer.logic;

import androidx.room.PrimaryKey;
import androidx.room.Relation;

import java.util.List;

public class MedicineWithReminders {
    @PrimaryKey
    public int medicineId;

    public String name;

    @Relation(
            parentColumn = "medicineId",
            entityColumn = "medicineRelId"
    )
    public List<Reminder> reminders;
}
