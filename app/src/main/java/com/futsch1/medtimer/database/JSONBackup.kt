package com.futsch1.medtimer.database

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

abstract class JSONBackup<T> protected constructor(private val contentClass: Class<T>) {
    fun createBackupAsString(databaseVersion: Int, list: List<T>): String? {
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(createBackup(databaseVersion, list))
    }

    open fun createBackup(databaseVersion: Int, list: List<T>): JsonElement {
        val content = DatabaseContentWithVersion(databaseVersion, list)
        val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create()
        return gson.toJsonTree(content)
    }

    fun parseBackup(jsonFile: String?): List<T>? {
        // In a first step, parse with the version set to 0
        var gson = registerTypeAdapters(GsonBuilder()).setVersion(0.0).create()
        try {
            val type = TypeToken.getParameterized(DatabaseContentWithVersion::class.java, contentClass).type
            var content: DatabaseContentWithVersion<T>? = gson.fromJson(jsonFile, type)
            if (content != null && content.version >= 0) {
                gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().setVersion(content.version.toDouble()).create()
                content = gson.fromJson(jsonFile, type)
                return checkBackup(content?.list)
            }
            return null
        } catch (e: JsonParseException) {
            Log.e(LogTags.BACKUP, (if (e.message != null) e.message else "")!!)
            return null
        }
    }

    protected abstract fun registerTypeAdapters(builder: GsonBuilder): GsonBuilder

    private fun checkBackup(list: List<T>?): List<T>? {
        if (list != null) {
            for (item in list) {
                if (isInvalid(item)) return null
            }
        }
        return list
    }

    protected abstract fun isInvalid(item: T?): Boolean

    abstract fun applyBackup(list: List<T>, medicineRepository: MedicineRepository)

    @JvmRecord
    protected data class DatabaseContentWithVersion<T>(
        @field:Expose val version: Int,
        @field:Expose val list: List<T>?
    )

    protected class FullDeserialize<T> : JsonDeserializer<T?> {
        @Throws(JsonParseException::class)
        override fun deserialize(je: JsonElement?, type: Type, jdc: JsonDeserializationContext?): T {
            val pojo = Gson().fromJson<T>(je, type)

            val fields = pojo!!.javaClass.declaredFields
            for (f in fields) {
                try {
                    if (f[pojo] == null) {
                        throw JsonParseException("Missing field in JSON: " + f.getName())
                    }
                } catch (e: IllegalArgumentException) {
                    Log.e(LogTags.BACKUP, "Internal error: ${e.message}")
                } catch (e: IllegalAccessException) {
                    Log.e(LogTags.BACKUP, "Internal error: ${e.message}")
                }
            }
            return pojo
        }
    }
}
