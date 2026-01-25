package com.futsch1.medtimer.database

import android.app.Application
import androidx.lifecycle.LiveData
import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.Arrays
import java.util.LinkedList
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.stream.Collectors

class MedicineRepository(val application: Application?) {
    private val medicineDao: MedicineDao
    private val database: MedicineRoomDatabase = MedicineRoomDatabase.getDatabase(application)

    // Stream.toList() not available in SDK version selected
    private val allStatusValues: List<ReminderStatus> =
        Arrays.stream(arrayOf(ReminderStatus.DELETED, ReminderStatus.RAISED, ReminderStatus.SKIPPED, ReminderStatus.TAKEN))
            .collect(
                Collectors.toList()
            )

    private val statusValuesWithoutDelete: List<ReminderStatus> =
        Arrays.stream(arrayOf(ReminderStatus.RAISED, ReminderStatus.SKIPPED, ReminderStatus.TAKEN)).collect(
            Collectors.toList()
        )

    init {
        medicineDao = database.medicineDao()
    }

    val version: Int
        get() = database.version

    val liveMedicines: LiveData<List<FullMedicine>>
        get() = medicineDao.getLiveMedicines()

    fun getOnlyMedicine(medicineId: Int): Medicine? {
        return medicineDao.getOnlyMedicine(medicineId)
    }

    fun getLiveMedicine(medicineId: Int): LiveData<FullMedicine> {
        return medicineDao.getLiveMedicine(medicineId)
    }

    fun getMedicine(medicineId: Int): FullMedicine? {
        return medicineDao.getMedicine(medicineId)
    }

    fun getLiveReminders(medicineId: Int): LiveData<List<Reminder>> {
        return medicineDao.getLiveReminders(medicineId)
    }

    fun getReminders(medicineId: Int): List<Reminder> {
        return medicineDao.getReminders(medicineId)
    }

    fun getReminder(reminderId: Int): Reminder? {
        return medicineDao.getReminder(reminderId)
    }

    fun getReminderFlow(reminderId: Int): Flow<Reminder> {
        return medicineDao.getReminderFlow(reminderId)
    }

    fun getMedicineFlow(medicineId: Int): Flow<FullMedicine> {
        return medicineDao.getMedicineFlow(medicineId)
    }


    fun getLiveReminderEvents(timeStamp: Long, withDeleted: Boolean): LiveData<List<ReminderEvent>> {
        return medicineDao.getLiveReminderEventsStartingFrom(timeStamp, if (withDeleted) allStatusValues else statusValuesWithoutDelete)
    }

    val allReminderEventsWithoutDeleted: List<ReminderEvent>
        get() = medicineDao.getLimitedReminderEvents(0L, statusValuesWithoutDelete)

    fun getLastDaysReminderEvents(days: Int): List<ReminderEvent> {
        return medicineDao.getLimitedReminderEvents(Instant.now().toEpochMilli() / 1000 - (days.toLong() * 24 * 60 * 60), allStatusValues)
    }

    fun getReminderEventsForScheduling(medicines: List<FullMedicine>): List<ReminderEvent> {
        val reminderEvents: MutableList<ReminderEvent> = LinkedList<ReminderEvent>()
        for (medicine in medicines) {
            for (reminder in medicine.reminders) {
                if (reminder.active) {
                    reminderEvents.addAll(medicineDao.getLastReminderEvents(reminder.reminderId, 2))
                }
            }
        }
        return reminderEvents
    }

    fun getLastReminderEvent(reminderId: Int): ReminderEvent? {
        return medicineDao.getLastReminderEvent(reminderId)
    }

    fun insertMedicine(medicine: Medicine): Long {
        return internalInsert(medicine) { medicine -> medicineDao.insertMedicine(medicine) }
    }

    private fun <T> internalInsert(insertType: T, f: Insert<T>): Long {
        try {
            return MedicineRoomDatabase.databaseWriteExecutor.submit(Callable { f.insert(insertType) }).get()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (_: ExecutionException) {
            return -1
        }
        return 0
    }

    fun deleteMedicine(medicineId: Int) {
        MedicineRoomDatabase.databaseWriteExecutor.execute {
            medicineDao.deleteMedicineToTagForMedicine(medicineId)
            medicineDao.deleteMedicine(medicineDao.getOnlyMedicine(medicineId))
        }
    }

    fun insertReminder(reminder: Reminder): Long {
        return internalInsert(reminder) { reminder -> medicineDao.insertReminder(reminder) }
    }

    fun updateReminder(reminder: Reminder) {
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.updateReminder(reminder) }
    }

    fun deleteReminder(reminderId: Int) {
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.deleteReminder(medicineDao.getReminder(reminderId)) }
    }

    fun insertReminderEvent(reminderEvent: ReminderEvent): Long {
        return internalInsert(reminderEvent) { reminderEvent -> medicineDao.insertReminderEvent(reminderEvent) }
    }

    fun getReminderEvent(reminderEventId: Int): ReminderEvent? {
        return medicineDao.getReminderEvent(reminderEventId)
    }

    fun getReminderEvent(reminderId: Int, remindedTimestamp: Long): ReminderEvent? {
        return medicineDao.getReminderEvent(reminderId, remindedTimestamp)
    }

    fun updateReminderEvent(reminderEvent: ReminderEvent) {
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.updateReminderEvent(reminderEvent) }
    }

    fun updateReminderEvents(reminderEvents: List<ReminderEvent>) {
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.updateReminderEvents(reminderEvents) }
    }

    fun deleteAll() {
        deleteReminders()
        deleteMedicines()
        deleteReminderEvents()
        deleteTags()
    }

    fun deleteReminders() {
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.deleteReminders() }
    }

    fun deleteMedicines() {
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.deleteMedicines() }
    }

    fun deleteReminderEvents() {
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.deleteReminderEvents() }
    }

    fun deleteTags() {
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.deleteTags() }
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.deleteMedicineToTags() }
    }

    fun deleteReminderEvent(reminderEvent: ReminderEvent) {
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.deleteReminderEvent(reminderEvent) }
    }

    fun getLinkedReminders(reminderId: Int): List<Reminder> {
        return medicineDao.getLinkedReminders(reminderId)
    }

    val liveTags: LiveData<List<Tag>>
        get() = medicineDao.getLiveTags()

    fun insertTag(tag: Tag): Long {
        val existingTag = getTagByName(tag.name)
        return existingTag?.tagId?.toLong() ?: internalInsert(tag) { tag -> medicineDao.insertTag(tag) }
    }

    fun getTagByName(name: String?): Tag? {
        return medicineDao.getTagByName(name)
    }

    fun deleteTag(tag: Tag) {
        MedicineRoomDatabase.databaseWriteExecutor.execute {
            medicineDao.deleteMedicineToTagForTag(tag.tagId)
            medicineDao.deleteTag(tag)
        }
    }

    fun insertMedicineToTag(medicineId: Int, tagId: Int) {
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.insertMedicineToTag(MedicineToTag(medicineId, tagId)) }
    }

    fun deleteMedicineToTag(medicineId: Int, tagId: Int) {
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.deleteMedicineToTag(MedicineToTag(medicineId, tagId)) }
    }

    val liveMedicineToTags: LiveData<List<MedicineToTag>>
        get() = medicineDao.getLiveMedicineToTags()

    fun hasTags(): Boolean {
        return medicineDao.countTags() > 0
    }

    val highestMedicineSortOrder: Double
        get() = medicineDao.getHighestMedicineSortOrder()

    fun moveMedicine(fromPosition: Int, toPosition: Int) {
        val medicines = this.medicines.toMutableList()
        try {
            val moveMedicine = medicines.removeAt(fromPosition)
            medicines.add(toPosition, moveMedicine)
            moveMedicine.medicine.sortOrder = (medicines[toPosition + 1].medicine.sortOrder + medicines[toPosition - 1].medicine.sortOrder) / 2
            updateMedicine(moveMedicine.medicine)
        } catch (_: IndexOutOfBoundsException) {
            // Intentionally left blank
        }
    }

    val medicines: List<FullMedicine>
        get() = medicineDao.getMedicines()

    fun updateMedicine(medicine: Medicine?) {
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.updateMedicine(medicine) }
    }

    fun flushDatabase() {
        if (MedicineRoomDatabase.databaseWriteExecutor.isShutdown) {
            return
        }
        try {
            // Submit an empty task and wait for its completion.
            // This guarantees all prior submitted tasks have completed.
            val future = MedicineRoomDatabase.databaseWriteExecutor.submit {}
            future[10, TimeUnit.SECONDS] // Wait with a timeout
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt() // Restore interrupt status
        } catch (_: ExecutionException) {
            // Intentionally left blank
        } catch (_: TimeoutException) {
            // Intentionally left blank
        }
    }

    fun insertReminderEvents(reminderEvents: List<ReminderEvent>) {
        MedicineRoomDatabase.databaseWriteExecutor.execute { medicineDao.insertReminderEvents(reminderEvents) }
    }

    internal fun interface Insert<T> {
        fun insert(item: T): Long
    }
}
