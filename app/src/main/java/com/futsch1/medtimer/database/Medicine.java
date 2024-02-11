package com.futsch1.medtimer.database;

import android.graphics.Color;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Medicine {

    @ColumnInfo(name = "medicineName")
    public final String name;
    @PrimaryKey(autoGenerate = true)
    public int medicineId;

    @ColumnInfo(defaultValue = "0xFFFF0000")
    public int color;
    @ColumnInfo(defaultValue = "false")
    public boolean useColor;

    public Medicine(String name) {
        this.name = name;
        this.useColor = false;
        this.color = Color.DKGRAY;
    }

    public Medicine(String name, int id) {
        this.name = name;
        this.medicineId = id;
        this.useColor = false;
        this.color = Color.DKGRAY;
    }
}
