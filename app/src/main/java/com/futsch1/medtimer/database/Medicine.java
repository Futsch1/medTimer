package com.futsch1.medtimer.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Medicine {

    @PrimaryKey(autoGenerate = true)
    public int medicineId;

    public String name;

    public Medicine(String name) {
        this.name = name;
    }
}
