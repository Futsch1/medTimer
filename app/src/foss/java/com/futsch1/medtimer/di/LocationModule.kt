package com.futsch1.medtimer.di

import com.futsch1.medtimer.location.GeofenceRegistrar
import com.futsch1.medtimer.location.LocationProvider
import com.futsch1.medtimer.location.NoOpGeofenceRegistrar
import com.futsch1.medtimer.location.NoOpLocationProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindGeofenceRegistrar(impl: NoOpGeofenceRegistrar): GeofenceRegistrar

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: NoOpLocationProvider): LocationProvider
}
