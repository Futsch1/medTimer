package com.futsch1.medtimer.di

import com.futsch1.medtimer.reminders.SystemTimeAccess
import com.futsch1.medtimer.reminders.TimeAccess
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
fun interface TimeAccessModule {
    @Binds
    fun bindTimeAccess(impl: SystemTimeAccess): TimeAccess
}
