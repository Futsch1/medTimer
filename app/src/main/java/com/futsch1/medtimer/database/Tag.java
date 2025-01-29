package com.futsch1.medtimer.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;

import java.util.Objects;

@Entity
public class Tag {
    @PrimaryKey(autoGenerate = true)
    public int tagId;
    @Expose
    public String name;

    public Tag(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagId, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return tagId == tag.tagId && Objects.equals(name, tag.name);
    }
}
