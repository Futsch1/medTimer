package com.futsch1.medtimer.harness

import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Test-only access to the repositories, for seeding data directly instead of via the UI. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepositoryEntryPoint {
    fun medicineRepository(): MedicineRepository
    fun reminderRepository(): ReminderRepository
    fun reminderEventRepository(): ReminderEventRepository
}
