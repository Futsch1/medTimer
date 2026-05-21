package com.futsch1.medtimer.database

import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.domain.repository.TagRepository

open class DatabaseManager(
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val tagRepository: TagRepository
) {
    suspend fun deleteAll() {
        reminderRepository.deleteAll()
        medicineRepository.deleteAll()
        reminderEventRepository.deleteAll()
        tagRepository.deleteAll()
    }
}
