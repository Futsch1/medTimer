package com.futsch1.medtimer.di

import android.content.Context
import androidx.room.Room
import com.futsch1.medtimer.database.DatabaseManager
import com.futsch1.medtimer.database.dao.MedicineDao
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.MedicineRoomDatabase
import com.futsch1.medtimer.database.MedicineRoomDatabase.Migration22To23
import com.futsch1.medtimer.database.dao.ReminderDao
import com.futsch1.medtimer.database.dao.ReminderEventDao
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.database.dao.TagDao
import com.futsch1.medtimer.database.TagRepository
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
            .addMigrations(Migration22To23)
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
    ): MedicineRepository = MedicineRepository(medicineDao)

    @Provides
    @Singleton
    fun provideReminderRepository(
        reminderDao: ReminderDao
    ): ReminderRepository = ReminderRepository(reminderDao)

    @Provides
    @Singleton
    fun provideReminderEventRepository(
        reminderEventDao: ReminderEventDao
    ): ReminderEventRepository = ReminderEventRepository(reminderEventDao)

    @Provides
    @Singleton
    fun provideTagRepository(
        tagDao: TagDao
    ): TagRepository = TagRepository(tagDao)

    @Provides
    @Singleton
    fun provideDatabaseManager(
        medicineRepository: MedicineRepository,
        reminderRepository: ReminderRepository,
        reminderEventRepository: ReminderEventRepository,
        tagRepository: TagRepository
    ): DatabaseManager = DatabaseManager(medicineRepository, reminderRepository, reminderEventRepository, tagRepository)
}
