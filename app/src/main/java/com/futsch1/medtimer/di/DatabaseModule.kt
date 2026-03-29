package com.futsch1.medtimer.di

import android.content.Context
import androidx.room.Room
import com.futsch1.medtimer.database.MedicineDao
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.MedicineRoomDatabase
import com.futsch1.medtimer.database.MedicineRoomDatabase.Migration22To23
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
    @Singleton
    fun provideMedicineRepository(
        medicineDao: MedicineDao
    ): MedicineRepository = MedicineRepository(medicineDao)
}
