package com.futsch1.medtimer.database.backup

import com.futsch1.medtimer.core.domain.backup.ReminderEventBackup
import com.futsch1.medtimer.core.domain.repository.BackupRepository
import com.google.gson.GsonBuilder

class JSONReminderEventBackup(
    private val backupRepository: BackupRepository
) : JSONBackup<ReminderEventBackup>(ReminderEventBackup::class.java) {
    override fun isInvalid(item: ReminderEventBackup?): Boolean {
        return item == null
    }

    override fun registerTypeAdapters(builder: GsonBuilder): GsonBuilder {
        return builder
            .registerTypeAdapter(ReminderEventBackup::class.java, FullDeserialize<ReminderEventBackup>())
    }

    override suspend fun applyBackup(list: List<ReminderEventBackup>) {
        backupRepository.clearReminderEvents()
        backupRepository.insertReminderEvents(list)
    }
}
