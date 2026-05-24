package com.futsch1.medtimer.feature.reminders.di

import com.futsch1.medtimer.feature.reminders.SystemTimeAccess
import com.futsch1.medtimer.feature.reminders.TimeAccess
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
