package com.futsch1.medtimer.database.backup

import com.futsch1.medtimer.database.dao.ReminderEventDao
import com.futsch1.medtimer.database.ReminderEventEntity
import com.google.gson.GsonBuilder

class JSONReminderEventBackup(
    private val reminderEventDao: ReminderEventDao
) : JSONBackup<ReminderEventEntity>(ReminderEventEntity::class.java) {
    override fun isInvalid(item: ReminderEventEntity?): Boolean {
        return item == null
    }

    override fun registerTypeAdapters(builder: GsonBuilder): GsonBuilder {
        return builder
            .registerTypeAdapter(ReminderEventEntity::class.java, FullDeserialize<ReminderEventEntity>())
    }

    override suspend fun applyBackup(list: List<ReminderEventEntity>) {
        reminderEventDao.deleteAll()
        reminderEventDao.createAll(list)
    }
}
