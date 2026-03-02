package com.futsch1.medtimer.database.di

import android.content.Context
import com.futsch1.medtimer.database.MedicineRepository
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
    fun provideMedicineRepository(@ApplicationContext context: Context): MedicineRepository {
        return MedicineRepository(context)
    }
}
