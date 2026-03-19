package com.futsch1.medtimer.database

import android.content.Context
import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.LinkedList

open class MedicineRepository(
    private val medicineDao: MedicineDao
) {
    // TODO: a temporary constructor for backwards compatibility with existing code; remove it once all usages are replaced with DI
    constructor(context: Context) : this(MedicineRoomDatabase.getDatabase(context).medicineDao())

    val medicinesFlow: Flow<List<FullMedicine>>
        get() = medicineDao.getMedicinesFlow()

    suspend fun getOnlyMedicine(medicineId: Int): Medicine? {
        return medicineDao.getOnlyMedicine(medicineId)
    }

    fun getMedicineFlow(medicineId: Int): Flow<FullMedicine?> {
        return medicineDao.getMedicineFlow(medicineId)
    }

    fun getMedicine(medicineId: Int): FullMedicine? {
        return medicineDao.getMedicine(medicineId)
    }

    fun getRemindersFlow(medicineId: Int): Flow<List<Reminder>> {
        return medicineDao.getRemindersFlow(medicineId)
    }

    fun getReminders(medicineId: Int): List<Reminder> {
        return medicineDao.getReminders(medicineId)
    }

    suspend fun getReminder(reminderId: Int): Reminder? {
        return medicineDao.getReminder(reminderId)
    }

    fun getReminderFlow(reminderId: Int): Flow<Reminder?> {
        return medicineDao.getReminderFlow(reminderId)
    }

    fun getReminderEventsFlow(timeStamp: Long, statusValues: List<ReminderStatus>): Flow<List<ReminderEvent>> {
        return medicineDao.getReminderEventsFlowStartingFrom(timeStamp, statusValues)
    }

    val allReminderEventsWithoutDeleted: List<ReminderEvent>
        get() = medicineDao.getLimitedReminderEvents(0L, statusValuesWithoutDelete)

    val allReminderEventsWithoutDeletedAndAcknowledged: List<ReminderEvent>
        get() = medicineDao.getLimitedReminderEvents(0L, statusValuesWithoutDeletedAndAcknowledged)

    fun getLastDaysReminderEvents(days: Int): List<ReminderEvent> {
        return medicineDao.getLimitedReminderEvents(Instant.now().toEpochMilli() / 1000 - (days.toLong() * 24 * 60 * 60), allStatusValues)
    }

    fun getReminderEventsForScheduling(medicines: List<FullMedicine>): List<ReminderEvent> {
        val reminderEvents: MutableList<ReminderEvent> = LinkedList<ReminderEvent>()
        for (medicine in medicines) {
            for (reminder in medicine.reminders) {
                if (reminder.active) {
                    reminderEvents.addAll(getLastReminderEventsForScheduling(reminder.reminderId))
                }
            }
        }
        return reminderEvents
    }

    private fun getLastReminderEventsForScheduling(reminderId: Int): List<ReminderEvent> {
        var lastReminderEvents = medicineDao.getLastReminderEvents(reminderId, 2)
        if (lastReminderEvents.isNotEmpty() && lastReminderEvents
                .all { reminderEvent -> reminderEvent.remindedTimestamp > Instant.now().toEpochMilli() / 1000 }
        ) {
            lastReminderEvents = medicineDao.getReminderEvents(reminderId)
        }
        return lastReminderEvents
    }


    fun getLastReminderEvent(reminderId: Int): ReminderEvent? {
        return medicineDao.getLastReminderEvent(reminderId)
    }

    suspend fun insertMedicine(medicine: Medicine): Long {
        return medicineDao.insertMedicine(medicine)
    }

    suspend fun deleteMedicine(medicineId: Int) {
        medicineDao.deleteMedicineToTagForMedicine(medicineId)
        medicineDao.getOnlyMedicine(medicineId)?.let { medicineDao.deleteMedicine(it) }
    }

    suspend fun insertReminder(reminder: Reminder): Long {
        return medicineDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) {
        medicineDao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminderId: Int) {
        medicineDao.getReminder(reminderId)?.let { medicineDao.deleteReminder(it) }
    }

    suspend fun insertReminderEvent(reminderEvent: ReminderEvent): Long {
        return medicineDao.insertReminderEvent(reminderEvent)
    }

    fun getReminderEvent(reminderEventId: Int): ReminderEvent? {
        return medicineDao.getReminderEvent(reminderEventId)
    }

    fun getReminderEventFlow(reminderEventId: Int): Flow<ReminderEvent?> {
        return medicineDao.getReminderEventFlow(reminderEventId)
    }

    fun getReminderEvent(reminderId: Int, remindedTimestamp: Long): ReminderEvent? {
        return medicineDao.getReminderEvent(reminderId, remindedTimestamp)
    }

    suspend fun updateReminderEvent(reminderEvent: ReminderEvent) {
        medicineDao.updateReminderEvent(reminderEvent)
    }

    suspend fun updateReminderEvents(reminderEvents: List<ReminderEvent>) {
        medicineDao.updateReminderEvents(reminderEvents)
    }

    suspend fun deleteAll() {
        deleteReminders()
        deleteMedicines()
        deleteReminderEvents()
        deleteTags()
    }

    suspend fun deleteReminders() {
        medicineDao.deleteReminders()
    }

    suspend fun deleteMedicines() {
        medicineDao.deleteMedicines()
    }

    suspend fun deleteReminderEvents() {
        medicineDao.deleteReminderEvents()
    }

    suspend fun deleteTags() {
        medicineDao.deleteTags()
        medicineDao.deleteMedicineToTags()
    }

    suspend fun deleteReminderEvent(reminderEvent: ReminderEvent) {
        medicineDao.deleteReminderEvent(reminderEvent)
    }

    fun getLinkedReminders(reminderId: Int): List<Reminder> {
        return medicineDao.getLinkedReminders(reminderId)
    }

    val tagsFlow: Flow<List<Tag>>
        get() = medicineDao.getTagsFlow()

    suspend fun insertTag(tag: Tag): Long {
        val existingTagId = getTagByName(tag.name)?.tagId?.toLong()

        return existingTagId ?: medicineDao.insertTag(tag)
    }

    fun getTagByName(name: String): Tag? {
        return medicineDao.getTagByName(name)
    }

    suspend fun deleteTag(tag: Tag) {
        medicineDao.deleteMedicineToTagForTag(tag.tagId)
        medicineDao.deleteTag(tag)
    }

    suspend fun insertMedicineToTag(medicineId: Int, tagId: Int) {
        medicineDao.insertMedicineToTag(MedicineToTag(medicineId, tagId))
    }

    suspend fun deleteMedicineToTag(medicineId: Int, tagId: Int) {
        medicineDao.deleteMedicineToTag(MedicineToTag(medicineId, tagId))
    }

    val medicineToTagsFlow: Flow<List<MedicineToTag>>
        get() = medicineDao.medicineToTagsFlow

    fun hasTags(): Boolean {
        return medicineDao.countTags() > 0
    }

    val highestMedicineSortOrder: Double
        get() = medicineDao.highestMedicineSortOrder

    suspend fun moveMedicine(fromPosition: Int, toPosition: Int) {
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

    suspend fun updateMedicine(medicine: Medicine) {
        medicineDao.updateMedicine(medicine)
    }

    suspend fun decreaseStock(medicineId: Int, decreaseAmount: Double): FullMedicine? {
        return medicineDao.decreaseStock(medicineId, decreaseAmount)
    }

    suspend fun updateMedicines(medicines: List<Medicine>) {
        medicineDao.updateMedicines(medicines)
    }

    suspend fun insertReminderEvents(reminderEvents: List<ReminderEvent>) {
        medicineDao.insertReminderEvents(reminderEvents)
    }

    suspend fun insertReminders(reminders: List<Reminder>) {
        medicineDao.insertReminders(reminders)
    }
}
