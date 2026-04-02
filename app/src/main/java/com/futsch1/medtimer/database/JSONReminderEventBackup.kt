package com.futsch1.medtimer.database

import com.google.gson.GsonBuilder

class JSONReminderEventBackup : JSONBackup<ReminderEventEntity>(ReminderEventEntity::class.java) {
    override fun isInvalid(item: ReminderEventEntity?): Boolean {
        return item == null
    }

    override fun registerTypeAdapters(builder: GsonBuilder): GsonBuilder {
        return builder
            .registerTypeAdapter(ReminderEventEntity::class.java, FullDeserialize<ReminderEventEntity>())
    }

    override suspend fun applyBackup(list: List<ReminderEventEntity>, medicineRepository: MedicineRepository) {
        medicineRepository.deleteReminderEvents()

        medicineRepository.insertReminderEvents(list)
    }
}
