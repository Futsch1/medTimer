package com.futsch1.medtimer.core.common.di

import com.futsch1.medtimer.core.common.time.SystemTimeAccess
import com.futsch1.medtimer.core.common.time.TimeAccess
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
