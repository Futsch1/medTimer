package com.futsch1.medtimer.database

import com.futsch1.medtimer.database.ReminderEventEntity.ReminderStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.LinkedList

open class MedicineRepository(
    private val medicineDao: MedicineDao
) {
    val medicinesFlow: Flow<List<FullMedicineEntity>>
        get() = medicineDao.getMedicinesFlow()

    suspend fun getOnlyMedicine(medicineId: Int): MedicineEntity? {
        return medicineDao.getOnlyMedicine(medicineId)
    }

    fun getMedicineFlow(medicineId: Int): Flow<FullMedicineEntity?> {
        return medicineDao.getMedicineFlow(medicineId)
    }

    suspend fun getMedicine(medicineId: Int): FullMedicineEntity? {
        return medicineDao.getMedicine(medicineId)
    }

    fun getRemindersFlow(medicineId: Int): Flow<List<ReminderEntity>> {
        return medicineDao.getRemindersFlow(medicineId)
    }

    suspend fun getReminders(medicineId: Int): List<ReminderEntity> {
        return medicineDao.getReminders(medicineId)
    }

    suspend fun getReminder(reminderId: Int): ReminderEntity? {
        return medicineDao.getReminder(reminderId)
    }

    fun getReminderFlow(reminderId: Int): Flow<ReminderEntity?> {
        return medicineDao.getReminderFlow(reminderId)
    }

    fun getReminderEventsFlow(timeStamp: Long, statusValues: List<ReminderStatus>): Flow<List<ReminderEventEntity>> {
        return medicineDao.getReminderEventsFlowStartingFrom(timeStamp, statusValues)
    }

    suspend fun getAllReminderEventsWithoutDeleted(): List<ReminderEventEntity> {
        return medicineDao.getLimitedReminderEvents(0L, statusValuesWithoutDelete)
    }

    suspend fun getAllReminderEventsWithoutDeletedAndAcknowledged(): List<ReminderEventEntity> {
        return medicineDao.getLimitedReminderEvents(0L, statusValuesWithoutDeletedAndAcknowledged)
    }

    suspend fun getLastDaysReminderEvents(days: Int): List<ReminderEventEntity> {
        return medicineDao.getLimitedReminderEvents(Instant.now().toEpochMilli() / 1000 - (days.toLong() * 24 * 60 * 60), allStatusValues)
    }

    suspend fun getReminderEventsForScheduling(medicines: List<FullMedicineEntity>): List<ReminderEventEntity> {
        val reminderEvents: MutableList<ReminderEventEntity> = LinkedList<ReminderEventEntity>()
        for (medicine in medicines) {
            for (reminder in medicine.reminders) {
                if (reminder.active) {
                    reminderEvents.addAll(getLastReminderEventsForScheduling(reminder.reminderId))
                }
            }
        }
        return reminderEvents
    }

    private suspend fun getLastReminderEventsForScheduling(reminderId: Int): List<ReminderEventEntity> {
        var lastReminderEvents = medicineDao.getLastReminderEvents(reminderId, 2)
        if (lastReminderEvents.isNotEmpty() && lastReminderEvents
                .all { reminderEvent -> reminderEvent.remindedTimestamp > Instant.now().toEpochMilli() / 1000 }
        ) {
            lastReminderEvents = medicineDao.getReminderEvents(reminderId)
        }
        return lastReminderEvents
    }

    suspend fun getLastReminderEvent(reminderId: Int): ReminderEventEntity? {
        return medicineDao.getLastReminderEvent(reminderId)
    }

    suspend fun insertMedicine(medicine: MedicineEntity): Long {
        return medicineDao.insertMedicine(medicine)
    }

    suspend fun deleteMedicine(medicineId: Int) {
        medicineDao.deleteMedicineToTagForMedicine(medicineId)
        medicineDao.getOnlyMedicine(medicineId)?.let { medicineDao.deleteMedicine(it) }
    }

    suspend fun insertReminder(reminder: ReminderEntity): Long {
        return medicineDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        medicineDao.updateReminder(reminder)
    }

    suspend fun updateReminders(reminder: List<ReminderEntity>) {
        medicineDao.updateReminders(reminder)
    }

    suspend fun deleteReminder(reminderId: Int) {
        medicineDao.getReminder(reminderId)?.let { medicineDao.deleteReminder(it) }
    }

    suspend fun insertReminderEvent(reminderEvent: ReminderEventEntity): Long {
        return medicineDao.insertReminderEvent(reminderEvent)
    }

    suspend fun getReminderEvent(reminderEventId: Int): ReminderEventEntity? {
        return medicineDao.getReminderEvent(reminderEventId)
    }

    fun getReminderEventFlow(reminderEventId: Int): Flow<ReminderEventEntity?> {
        return medicineDao.getReminderEventFlow(reminderEventId)
    }

    suspend fun getReminderEvent(reminderId: Int, remindedTimestamp: Long): ReminderEventEntity? {
        return medicineDao.getReminderEvent(reminderId, remindedTimestamp)
    }

    suspend fun updateReminderEvent(reminderEvent: ReminderEventEntity) {
        medicineDao.updateReminderEvent(reminderEvent)
    }

    suspend fun updateReminderEvents(reminderEvents: List<ReminderEventEntity>) {
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

    suspend fun deleteReminderEvent(reminderEvent: ReminderEventEntity) {
        medicineDao.deleteReminderEvent(reminderEvent)
    }

    suspend fun getLinkedReminders(reminderId: Int): List<ReminderEntity> {
        return medicineDao.getLinkedReminders(reminderId)
    }

    val tagsFlow: Flow<List<TagEntity>>
        get() = medicineDao.getTagsFlow()

    suspend fun insertTag(tag: TagEntity): Long {
        val existingTagId = getTagByName(tag.name)?.tagId?.toLong()

        return existingTagId ?: medicineDao.insertTag(tag)
    }

    suspend fun getTagByName(name: String): TagEntity? {
        return medicineDao.getTagByName(name)
    }

    suspend fun deleteTag(tag: TagEntity) {
        medicineDao.deleteMedicineToTagForTag(tag.tagId)
        medicineDao.deleteTag(tag)
    }

    suspend fun insertMedicineToTag(medicineId: Int, tagId: Int) {
        medicineDao.insertMedicineToTag(MedicineToTagEntity(medicineId, tagId))
    }

    suspend fun deleteMedicineToTag(medicineId: Int, tagId: Int) {
        medicineDao.deleteMedicineToTag(MedicineToTagEntity(medicineId, tagId))
    }

    fun getMedicineToTagsFlow(): Flow<List<MedicineToTagEntity>> {
        return medicineDao.getMedicineToTagsFlow()
    }

    suspend fun hasTags(): Boolean {
        return medicineDao.countTags() > 0
    }

    suspend fun getHighestMedicineSortOrder(): Double {
        return medicineDao.getHighestMedicineSortOrder()
    }

    suspend fun moveMedicine(fromPosition: Int, toPosition: Int) {
        val medicines = medicineDao.getMedicines().toMutableList()
        try {
            val moveMedicine = medicines.removeAt(fromPosition)
            medicines.add(toPosition, moveMedicine)
            moveMedicine.medicine.sortOrder = (medicines[toPosition + 1].medicine.sortOrder + medicines[toPosition - 1].medicine.sortOrder) / 2
            updateMedicine(moveMedicine.medicine)
        } catch (_: IndexOutOfBoundsException) {
            // Intentionally left blank
        }
    }

    suspend fun getMedicines(): List<FullMedicineEntity> {
        return medicineDao.getMedicines()
    }

    suspend fun updateMedicine(medicine: MedicineEntity) {
        medicineDao.updateMedicine(medicine)
    }

    suspend fun decreaseStock(medicineId: Int, decreaseAmount: Double): FullMedicineEntity? {
        return medicineDao.decreaseStock(medicineId, decreaseAmount)
    }

    suspend fun updateMedicines(medicines: List<MedicineEntity>) {
        medicineDao.updateMedicines(medicines)
    }

    suspend fun insertReminderEvents(reminderEvents: List<ReminderEventEntity>) {
        medicineDao.insertReminderEvents(reminderEvents)
    }

    suspend fun insertReminders(reminders: List<ReminderEntity>) {
        medicineDao.insertReminders(reminders)
    }
}
