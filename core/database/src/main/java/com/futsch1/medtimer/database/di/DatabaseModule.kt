package com.futsch1.medtimer.database.di

import android.content.Context
import androidx.room.Room
import com.futsch1.medtimer.core.domain.repository.BackupRepository
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.domain.repository.TagRepository
import com.futsch1.medtimer.database.BackupRepositoryImpl
import com.futsch1.medtimer.database.DatabaseManager
import com.futsch1.medtimer.database.MedicineRepositoryImpl
import com.futsch1.medtimer.database.MedicineRoomDatabase
import com.futsch1.medtimer.database.ReminderEventRepositoryImpl
import com.futsch1.medtimer.database.ReminderRepositoryImpl
import com.futsch1.medtimer.database.TagRepositoryImpl
import com.futsch1.medtimer.database.dao.MedicineDao
import com.futsch1.medtimer.database.dao.ReminderDao
import com.futsch1.medtimer.database.dao.ReminderEventDao
import com.futsch1.medtimer.database.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMedicineRoomDatabase(@ApplicationContext context: Context): MedicineRoomDatabase =
        Room.databaseBuilder(context, MedicineRoomDatabase::class.java, "medTimer")
            .build()

    @Provides
    fun provideMedicineDao(database: MedicineRoomDatabase): MedicineDao =
        database.medicineDao()

    @Provides
    fun provideReminderDao(database: MedicineRoomDatabase): ReminderDao =
        database.reminderDao()

    @Provides
    fun provideReminderEventDao(database: MedicineRoomDatabase): ReminderEventDao =
        database.reminderEventDao()

    @Provides
    fun provideTagDao(database: MedicineRoomDatabase): TagDao =
        database.tagDao()

    @Provides
    @Singleton
    fun provideMedicineRepository(
        medicineDao: MedicineDao
    ): MedicineRepository = MedicineRepositoryImpl(medicineDao)

    @Provides
    @Singleton
    fun provideReminderRepository(
        reminderDao: ReminderDao
    ): ReminderRepository = ReminderRepositoryImpl(reminderDao)

    @Provides
    @Singleton
    fun provideReminderEventRepository(
        reminderEventDao: ReminderEventDao
    ): ReminderEventRepository = ReminderEventRepositoryImpl(reminderEventDao)

    @Provides
    @Singleton
    fun provideTagRepository(
        tagDao: TagDao
    ): TagRepository = TagRepositoryImpl(tagDao)

    @Provides
    @Singleton
    fun provideDatabaseManager(
        medicineRepository: MedicineRepository,
        reminderRepository: ReminderRepository,
        reminderEventRepository: ReminderEventRepository,
        tagRepository: TagRepository
    ): DatabaseManager = DatabaseManager(medicineRepository, reminderRepository, reminderEventRepository, tagRepository)

    @Provides
    @Singleton
    fun provideBackupRepository(
        medicineDao: MedicineDao,
        reminderDao: ReminderDao,
        reminderEventDao: ReminderEventDao,
        tagDao: TagDao,
        database: MedicineRoomDatabase
    ): BackupRepository = BackupRepositoryImpl(medicineDao, reminderDao, reminderEventDao, tagDao, database)
}
