package com.futsch1.medtimer.di

import android.content.Context
import com.futsch1.medtimer.location.GeofenceRegistrar
import com.futsch1.medtimer.location.GmsGeofenceRegistrar
import com.futsch1.medtimer.location.GmsLocationProvider
import com.futsch1.medtimer.location.LocationProvider
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindGeofenceRegistrar(impl: GmsGeofenceRegistrar): GeofenceRegistrar

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: GmsLocationProvider): LocationProvider

    companion object {
        @Provides
        @Singleton
        fun provideGeofencingClient(@ApplicationContext context: Context): GeofencingClient =
            LocationServices.getGeofencingClient(context)

        @Provides
        @Singleton
        fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        @Provides
        @Singleton
        fun provideGoogleApiAvailability(): GoogleApiAvailability = GoogleApiAvailability.getInstance()
    }
}
