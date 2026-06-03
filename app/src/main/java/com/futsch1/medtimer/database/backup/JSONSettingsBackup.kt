package com.futsch1.medtimer.database.backup

import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.datastore.applyTo
import com.futsch1.medtimer.core.datastore.toSettingsBackup
import com.futsch1.medtimer.core.domain.backup.SettingsBackup
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParseException

class JSONSettingsBackup(private val preferencesDataSource: PreferencesDataSource) {

    fun createBackup(): JsonElement {
        val data = preferencesDataSource.preferences.value.toSettingsBackup()
        return GsonBuilder().setPrettyPrinting().create().toJsonTree(data)
    }

    fun applyBackup(json: String): Boolean {
        return try {
            val data = Gson().fromJson(json, SettingsBackup::class.java) ?: return false
            data.applyTo(preferencesDataSource)
            true
        } catch (e: JsonParseException) {
            Log.e(LogTags.BACKUP, "Settings backup restore failed: ${e.message}")
            false
        } catch (e: IllegalArgumentException) {
            Log.e(LogTags.BACKUP, "Settings backup restore failed: ${e.message}")
            false
        } catch (e: NullPointerException) {
            Log.e(LogTags.BACKUP, "Settings backup restore failed: ${e.message}")
            false
        }
    }
}
