package com.futsch1.medtimer.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultPreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MedTimerPreferencess

@Module
@InstallIn(SingletonComponent::class)
object DatastoreModule {

    @Provides
    @Singleton
    @DefaultPreferences
    fun providesDefaultSharedPreferences(@ApplicationContext ctx: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(ctx)
    }

    @Provides
    @Singleton
    @MedTimerPreferencess
    fun providesMedTimerSharedPreferences(@ApplicationContext ctx: Context): SharedPreferences {
        return ctx.getSharedPreferences("medTimer", Context.MODE_PRIVATE)
    }
}