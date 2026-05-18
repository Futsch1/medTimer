package com.futsch1.medtimer.database

import com.futsch1.medtimer.core.domain.backup.FullMedicineBackup
import com.futsch1.medtimer.core.domain.backup.MedicineBackup
import com.futsch1.medtimer.core.domain.backup.ReminderBackup
import com.futsch1.medtimer.core.domain.backup.ReminderEventBackup
import com.futsch1.medtimer.core.domain.backup.TagBackup
import com.futsch1.medtimer.core.domain.repository.BackupRepository
import com.futsch1.medtimer.database.dao.MedicineDao
import com.futsch1.medtimer.database.dao.ReminderDao
import com.futsch1.medtimer.database.dao.ReminderEventDao
import com.futsch1.medtimer.database.dao.TagDao
import com.futsch1.medtimer.database.toBackup.toBackup
import com.futsch1.medtimer.database.toBackup.toEntity
import java.time.Instant

class BackupRepositoryImpl(
    private val medicineDao: MedicineDao,
    private val reminderDao: ReminderDao,
    private val reminderEventDao: ReminderEventDao,
    private val tagDao: TagDao,
    private val database: MedicineRoomDatabase
) : BackupRepository {

    override val databaseVersion: Int
        get() = database.version

    override suspend fun getMedicineBackup(): List<FullMedicineBackup> {
        return medicineDao.getAll().map { it.toBackup() }
    }

    override suspend fun getReminderEventBackup(): List<ReminderEventBackup> {
        return reminderEventDao.getAllLimited(0L, statusValuesWithoutDelete).map { it.toBackup() }
    }

    override suspend fun clearMedicineData() {
        reminderDao.deleteAll()
        medicineDao.deleteAll()
        tagDao.deleteAll()
    }

    override suspend fun insertMedicine(medicine: MedicineBackup): Int {
        return medicineDao.create(medicine.toEntity()).toInt()
    }

    override suspend fun insertReminders(reminders: List<ReminderBackup>, medicineId: Int) {
        val createdTimestamp = Instant.now().toEpochMilli() / 1000
        val entities = reminders.map { reminder ->
            reminder.toEntity().apply {
                medicineRelId = medicineId
                this.createdTimestamp = createdTimestamp
            }
        }
        reminderDao.createAll(entities)
    }

    override suspend fun insertTag(tag: TagBackup): Int {
        return tagDao.create(tag.toEntity()).toInt()
    }

    override suspend fun linkMedicineTag(medicineId: Int, tagId: Int) {
        tagDao.createMedicineToTag(MedicineToTagEntity(medicineId, tagId))
    }

    override suspend fun clearReminderEvents() {
        reminderEventDao.deleteAll()
    }

    override suspend fun insertReminderEvents(events: List<ReminderEventBackup>) {
        reminderEventDao.createAll(events.map { it.toEntity() })
    }
}
