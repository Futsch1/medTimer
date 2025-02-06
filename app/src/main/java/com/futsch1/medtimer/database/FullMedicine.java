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
}
