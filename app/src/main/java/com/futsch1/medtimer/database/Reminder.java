package com.futsch1.medtimer.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Reminder {
    @PrimaryKey
    public int reminderId;

    public int medicineRelId;

    public long timeInMinutes;

    public String amount;
}
