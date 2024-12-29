package com.futsch1.medtimer.database;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("java:S1319")
public class Converters {
    private Converters() {
        // Intentionally left empty
    }

    @TypeConverter
    public static List<Boolean> fromString(String value) {
        Type listType = new TypeToken<List<Boolean>>() {
        }.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromSet(List<Boolean> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static ArrayList<Integer> intListFromString(String value) {
        Type listType = new TypeToken<ArrayList<Integer>>() {
        }.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromIntList(ArrayList<Integer> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }
}