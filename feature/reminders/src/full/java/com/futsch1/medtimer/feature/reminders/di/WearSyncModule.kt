package com.futsch1.medtimer.feature.reminders.di

import com.futsch1.medtimer.feature.reminders.wear.GmsWearSyncController
import com.futsch1.medtimer.feature.reminders.wear.WearSyncController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WearSyncModule {
    @Binds
    @Singleton
    abstract fun bindWearSyncController(impl: GmsWearSyncController): WearSyncController
}
