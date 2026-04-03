package com.futsch1.medtimer.database

import com.google.gson.GsonBuilder

class JSONReminderEventBackup(
    private val reminderEventRepository: ReminderEventRepository
) : JSONBackup<ReminderEvent>(ReminderEvent::class.java) {
    override fun isInvalid(item: ReminderEvent?): Boolean {
        return item == null
    }

    override fun registerTypeAdapters(builder: GsonBuilder): GsonBuilder {
        return builder
            .registerTypeAdapter(ReminderEvent::class.java, FullDeserialize<ReminderEvent>())
    }

    override suspend fun applyBackup(list: List<ReminderEvent>) {
        reminderEventRepository.deleteAll()

        reminderEventRepository.createAll(list)
    }
}
