package com.futsch1.medtimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.MedicineToTag
import com.futsch1.medtimer.database.MedicineWithReminders
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import java.util.stream.Collectors

class MedicineViewModel(application: Application) : AndroidViewModel(application) {
    @JvmField
    val medicineRepository: MedicineRepository = MedicineRepository(application)
    private val liveMedicines: LiveData<List<MedicineWithReminders>> =
        medicineRepository.liveMedicines

    @JvmField
    val validTagIds: MutableLiveData<List<Int>> = MutableLiveData(listOf())
    private lateinit var medicineToTags: List<MedicineToTag>

    init {
        medicineRepository.liveMedicineToTags.observeForever {
            medicineToTags = it
        }
    }

    val medicines: LiveData<List<MedicineWithReminders>>
        get() {
            return liveMedicines.map {
                it.stream()
                    .filter { medicineWithReminders -> filterMedicineId(medicineWithReminders.medicine.medicineId) }
                    .collect(Collectors.toList())
            }
        }

    private fun filterMedicineId(medicineId: Int): Boolean {
        if (validTagIds.value == null || validTagIds.value!!.isEmpty()) {
            return true
        }
        val medicineTags: List<Int> = medicineToTags.stream()
            .filter { medicineToTag -> medicineToTag.medicineId == medicineId }
            .map { medicineToTag -> medicineToTag.tagId }
            .collect(Collectors.toList())
        for (tagId in validTagIds.value!!) {
            if (medicineTags.contains(tagId)) {
                return true
            }
        }
        return false
    }

    fun getMedicine(medicineId: Int): Medicine {
        return medicineRepository.getMedicine(medicineId)
    }

    fun insertMedicine(medicine: Medicine?): Int {
        return medicineRepository.insertMedicine(medicine).toInt()
    }

    fun updateMedicine(medicine: Medicine?) {
        medicineRepository.updateMedicine(medicine)
    }

    fun deleteMedicine(medicineId: Int) {
        medicineRepository.deleteMedicine(medicineId)
    }

    fun getLiveReminders(medicineId: Int): LiveData<List<Reminder>> {
        return medicineRepository.getLiveReminders(medicineId)
    }

    fun getReminders(medicineId: Int): List<Reminder> {
        return medicineRepository.getReminders(medicineId)
    }

    fun getReminder(reminderId: Int): Reminder {
        return medicineRepository.getReminder(reminderId)
    }

    fun insertReminder(reminder: Reminder?): Int {
        return medicineRepository.insertReminder(reminder).toInt()
    }

    fun updateReminder(reminder: Reminder?) {
        medicineRepository.updateReminder(reminder)
    }

    fun deleteReminder(reminderId: Int) {
        medicineRepository.deleteReminder(reminderId)
    }

    fun getLinkedReminders(reminderId: Int): List<Reminder> {
        return medicineRepository.getLinkedReminders(reminderId)
    }

    fun getLiveReminderEvents(
        limit: Int,
        timeStamp: Long,
        withDeleted: Boolean
    ): LiveData<List<ReminderEvent>> {
        return medicineRepository.getLiveReminderEvents(limit, timeStamp, withDeleted)
    }

    fun deleteReminderEvents() {
        medicineRepository.deleteReminderEvents()
    }

    fun deleteAll() {
        medicineRepository.deleteAll()
    }

    fun getReminderEvent(reminderEventId: Int): ReminderEvent? {
        return medicineRepository.getReminderEvent(reminderEventId)
    }

    fun updateReminderEvent(reminderEvent: ReminderEvent?) {
        medicineRepository.updateReminderEvent(reminderEvent)
    }
}