package com.futsch1.medtimer.di

import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataSourcesEntryPoint {
    fun getPreferencesDataSource(): PreferencesDataSource
    fun getPersistentDataDataSource(): PersistentDataDataSource
}
