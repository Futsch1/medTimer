package com.futsch1.medtimer.database.backup

import com.futsch1.medtimer.core.domain.backup.FullMedicineBackup
import com.futsch1.medtimer.core.domain.backup.MedicineBackup
import com.futsch1.medtimer.core.domain.backup.ReminderBackup
import com.futsch1.medtimer.core.domain.backup.TagBackup
import com.futsch1.medtimer.core.domain.repository.BackupRepository
import com.futsch1.medtimer.database.MedicineToTagEntity
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement

class JSONMedicineBackup(
    private val backupRepository: BackupRepository
) : JSONBackup<FullMedicineBackup>(FullMedicineBackup::class.java) {
    override fun createBackup(databaseVersion: Int, list: List<FullMedicineBackup>): JsonElement {
        list.flatMap { it.reminders }
            .filter { it.instructions == null }
            .forEach { it.instructions = "" }
        return super.createBackup(databaseVersion, list)
    }

    override fun registerTypeAdapters(builder: GsonBuilder): GsonBuilder {
        return builder
            .registerTypeAdapter(MedicineBackup::class.java, FullDeserialize<MedicineBackup?>())
            .registerTypeAdapter(TagBackup::class.java, FullDeserialize<Any?>())
            .registerTypeAdapter(ReminderBackup::class.java, FullDeserialize<ReminderBackup?>())
    }

    override fun isInvalid(item: FullMedicineBackup?): Boolean {
        return item == null
    }

    override suspend fun applyBackup(list: List<FullMedicineBackup>) {
        backupRepository.clearMedicineData()

        var sortOrder = 1.0

        for (fullMedicine in list) {
            if (fullMedicine.medicine.sortOrder == 0.0) {
                fullMedicine.medicine.sortOrder = sortOrder++
            }
            val medicineId = backupRepository.insertMedicine(fullMedicine.medicine)
            backupRepository.insertReminders(fullMedicine.reminders, medicineId)
            processTags(fullMedicine, medicineId)
        }
    }

    private suspend fun processTags(fullMedicine: FullMedicineBackup, medicineId: Int) {
        for (tag in fullMedicine.tags) {
            val tagEntity = backupRepository.getTagByName(tag.name ?: "")
            val tagId = tagEntity?.tagId ?: backupRepository.insertTag(tag)
            backupRepository.linkMedicineTag(MedicineToTagEntity(medicineId, tagId))
        }
    }
}
