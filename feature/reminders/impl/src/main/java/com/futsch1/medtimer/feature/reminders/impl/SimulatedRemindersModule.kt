package com.futsch1.medtimer.feature.reminders.impl

import com.futsch1.medtimer.feature.reminders.api.SimulatedReminders
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SimulatedRemindersModule {
    @Binds
    @Singleton
    abstract fun bindSimulatedReminders(impl: SimulatedRemindersRepository): SimulatedReminders
}
