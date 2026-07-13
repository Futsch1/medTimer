package com.futsch1.medtimer.feature.reminders.command

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CommandBusModule {
    @Binds
    abstract fun bindReminderCommandBus(impl: ReminderCommandBusImpl): ReminderCommandBus
}
