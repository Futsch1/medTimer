package com.futsch1.medtimer.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Converters {
    @JvmStatic
    @TypeConverter
    fun fromString(value: String): MutableList<Boolean> {
        val listType = object : TypeToken<MutableList<Boolean>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: MutableList<Boolean>): String {
        val gson = Gson()
        return gson.toJson(list)
    }

    @JvmStatic
    @TypeConverter
    fun doubleListFromString(value: String): MutableList<Double> {
        val listType = object : TypeToken<MutableList<Double>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromDoubleList(list: MutableList<Double>): String {
        val gson = Gson()
        return gson.toJson(list)
    }

    @JvmStatic
    @TypeConverter
    fun fromStringString(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        val gson = Gson()
        return gson.toJson(list)
    }
}