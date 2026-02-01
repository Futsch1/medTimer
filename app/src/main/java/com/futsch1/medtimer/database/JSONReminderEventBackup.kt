package com.futsch1.medtimer.database

import com.google.gson.GsonBuilder

class JSONReminderEventBackup : JSONBackup<ReminderEvent>(ReminderEvent::class.java) {
    override fun isInvalid(item: ReminderEvent?): Boolean {
        return item == null
    }

    override fun registerTypeAdapters(builder: GsonBuilder): GsonBuilder {
        return builder
            .registerTypeAdapter(ReminderEvent::class.java, FullDeserialize<ReminderEvent>())
    }

    override fun applyBackup(list: List<ReminderEvent>, medicineRepository: MedicineRepository) {
        medicineRepository.deleteReminderEvents()

        medicineRepository.insertReminderEvents(list)
    }
}
