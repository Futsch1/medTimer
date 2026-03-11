package com.futsch1.medtimer.database

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import java.time.Instant

class JSONMedicineBackup : JSONBackup<FullMedicine>(FullMedicine::class.java) {
    override fun createBackup(databaseVersion: Int, list: List<FullMedicine>): JsonElement {
        // Correct the medicines where the reminder's instructions are null in this loop
        for (fullMedicine in list) {
            fullMedicine.reminders.filter { reminder -> reminder.instructions == null }
                .forEach { reminder -> reminder.instructions = "" }
        }
        return super.createBackup(databaseVersion, list)
    }

    override fun registerTypeAdapters(builder: GsonBuilder): GsonBuilder {
        return builder
            .registerTypeAdapter(Medicine::class.java, FullDeserialize<Medicine?>())
            .registerTypeAdapter(Tag::class.java, FullDeserialize<Any?>())
            .registerTypeAdapter(Reminder::class.java, FullDeserialize<Reminder?>())
    }

    override fun isInvalid(item: FullMedicine?): Boolean {
        return item == null
    }

    override suspend fun applyBackup(list: List<FullMedicine>, medicineRepository: MedicineRepository) {
        medicineRepository.deleteReminders()
        medicineRepository.deleteMedicines()
        medicineRepository.deleteTags()

        var sortOrder = 1.0

        for (fullMedicine in list) {
            if (fullMedicine.medicine.sortOrder == 0.0) {
                fullMedicine.medicine.sortOrder = sortOrder++
            }
            val medicineId = medicineRepository.insertMedicine(fullMedicine.medicine)
            processReminders(medicineRepository, fullMedicine, medicineId.toInt())
            processTags(medicineRepository, fullMedicine, medicineId.toInt())
        }
    }

    private suspend fun processTags(medicineRepository: MedicineRepository, fullMedicine: FullMedicine, medicineId: Int) {
        for (tag in fullMedicine.tags) {
            val tagId = medicineRepository.insertTag(tag).toInt()
            medicineRepository.insertMedicineToTag(medicineId, tagId)
        }
    }

    companion object {
        private suspend fun processReminders(medicineRepository: MedicineRepository, fullMedicine: FullMedicine, medicineId: Int) {
            val reminders: MutableList<Reminder> = mutableListOf()
            for (reminder in fullMedicine.reminders) {
                reminder.medicineRelId = medicineId
                reminder.createdTimestamp = Instant.now().toEpochMilli() / 1000
                reminders.add(reminder)
            }
            medicineRepository.insertReminders(reminders)
        }
    }
}
