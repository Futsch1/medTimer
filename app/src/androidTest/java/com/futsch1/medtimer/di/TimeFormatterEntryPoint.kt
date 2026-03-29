package com.futsch1.medtimer.di

import com.futsch1.medtimer.helpers.TimeFormatter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TimeFormatterEntryPoint {
    fun timeFormatter(): TimeFormatter
}
