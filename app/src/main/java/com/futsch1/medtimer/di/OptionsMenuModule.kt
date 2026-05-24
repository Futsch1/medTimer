package com.futsch1.medtimer.di

import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.feature.ui.OptionsMenuFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class OptionsMenuModule {
    @Binds
    abstract fun bindOptionsMenuFactory(factory: OptionsMenu.Factory): OptionsMenuFactory
}
