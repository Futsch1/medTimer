package com.futsch1.medtimer.database;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class MedicineWithTags {
    @Embedded
    public Medicine medicine;
    @Relation(
            parentColumn = "medicineId",
            entityColumn = "tagId",
            associateBy = @Junction(MedicineToTag.class)
    )
    public List<Tag> tags;
}
