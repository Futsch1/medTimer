package com.futsch1.medtimer.di

import com.futsch1.medtimer.AppOptionsActionsImpl
import com.futsch1.medtimer.feature.ui.AppOptionsActionsFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
fun interface AppOptionsModule {
    @Binds
    fun bindAppOptionsActionsFactory(factory: AppOptionsActionsImpl.Factory): AppOptionsActionsFactory
}
