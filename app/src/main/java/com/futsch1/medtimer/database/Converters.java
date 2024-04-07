package com.futsch1.medtimer.database;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.DayOfWeek;
import java.util.Set;

public class Converters {
    @TypeConverter
    public static Set<DayOfWeek> fromString(String value) {
        Type listType = new TypeToken<Set<DayOfWeek>>() {
        }.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromSet(Set<DayOfWeek> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }
}