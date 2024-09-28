package com.futsch1.medtimer.database;

import android.graphics.Color;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.futsch1.medtimer.ReminderNotificationChannelManager;
import com.google.gson.annotations.Expose;

@Entity
public class Medicine {

    @ColumnInfo(name = "medicineName")
    @Expose
    public final String name;
    @PrimaryKey(autoGenerate = true)
    public int medicineId;

    @ColumnInfo(defaultValue = "0xFFFF0000")
    @Expose
    public int color;
    @ColumnInfo(defaultValue = "false")
    @Expose
    public boolean useColor;
    @ColumnInfo(defaultValue = "3")
    @Expose
    public int notificationImportance;
    @ColumnInfo(defaultValue = "0")
    @Expose
    public int iconId;

    public Medicine(String name) {
        this.name = name;
        this.useColor = false;
        this.color = Color.DKGRAY;
        this.notificationImportance = ReminderNotificationChannelManager.Importance.DEFAULT.getValue();
        this.iconId = 0;
    }

    public Medicine(String name, int id) {
        this.name = name;
        this.medicineId = id;
        this.useColor = false;
        this.color = Color.DKGRAY;
        this.notificationImportance = ReminderNotificationChannelManager.Importance.DEFAULT.getValue();
        this.iconId = 0;
    }
}
