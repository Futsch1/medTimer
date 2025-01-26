package com.futsch1.medtimer.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;

@Entity
public class Tag {
    @PrimaryKey(autoGenerate = true)
    public int tagId;
    @Expose
    public String name;
}
