package com.futsch1.medtimer.di

import com.futsch1.medtimer.helpers.TimeFormatter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Entry point for instrumented tests to access TimeFormatter without @HiltAndroidTest.
// Must live in the main source set so it is installed in the real app's SingletonComponent.
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TimeFormatterEntryPoint {
    fun timeFormatter(): TimeFormatter
}
