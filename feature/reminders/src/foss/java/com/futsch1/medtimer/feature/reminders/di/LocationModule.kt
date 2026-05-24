package com.futsch1.medtimer.feature.reminders.di

import com.futsch1.medtimer.feature.reminders.location.GeofenceRegistrar
import com.futsch1.medtimer.feature.reminders.location.LocationProvider
import com.futsch1.medtimer.feature.reminders.location.NoOpGeofenceRegistrar
import com.futsch1.medtimer.feature.reminders.location.NoOpLocationProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface LocationModule {

    @Binds
    @Singleton
    fun bindGeofenceRegistrar(impl: NoOpGeofenceRegistrar): GeofenceRegistrar

    @Binds
    @Singleton
    fun bindLocationProvider(impl: NoOpLocationProvider): LocationProvider
}
