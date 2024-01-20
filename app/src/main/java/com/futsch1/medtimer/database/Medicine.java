package com.futsch1.medtimer.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Medicine {
    @PrimaryKey
    public int uid;

    @ColumnInfo(name = "name")
    public String name;
}
