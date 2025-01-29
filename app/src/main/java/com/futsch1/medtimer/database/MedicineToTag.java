package com.futsch1.medtimer.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(primaryKeys = {"medicineId", "tagId"})
public class MedicineToTag {
    public int medicineId;
    @ColumnInfo(index = true)
    public int tagId;

    public MedicineToTag(int medicineId, int tagId) {
        this.medicineId = medicineId;
        this.tagId = tagId;
    }
}

