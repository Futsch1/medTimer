package com.futsch1.medtimer.core.domain.repository

import com.futsch1.medtimer.core.domain.backup.FullMedicineBackup
import com.futsch1.medtimer.core.domain.backup.MedicineBackup
import com.futsch1.medtimer.core.domain.backup.ReminderBackup
import com.futsch1.medtimer.core.domain.backup.ReminderEventBackup
import com.futsch1.medtimer.core.domain.backup.TagBackup

interface BackupRepository {
    val databaseVersion: Int
    suspend fun getMedicineBackup(): List<FullMedicineBackup>
    suspend fun getReminderEventBackup(): List<ReminderEventBackup>
    suspend fun clearMedicineData()
    suspend fun insertMedicine(medicine: MedicineBackup): Int
    suspend fun insertReminders(reminders: List<ReminderBackup>, medicineId: Int)
    suspend fun insertTag(tag: TagBackup): Int
    suspend fun linkMedicineTag(medicineId: Int, tagId: Int)
    suspend fun clearReminderEvents()
    suspend fun insertReminderEvents(events: List<ReminderEventBackup>)
}
