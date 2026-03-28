package com.futsch1.medtimer.di

import android.app.ActivityManager
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.PowerManager
import android.os.Vibrator
import android.view.inputmethod.InputMethodManager
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import org.mockito.kotlin.mock
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SystemServicesModule::class])
object TestSystemServicesModule {

    // AlarmManager and NotificationManager are always bound per-test via @BindValue
    // (either specific mocks for verification or plain mocks for other tests).

    @Provides
    @Singleton
    fun provideAudioManager(): AudioManager = mock()

    @Provides
    @Singleton
    fun providePowerManager(): PowerManager = mock()

    @Provides
    @Singleton
    fun provideActivityManager(): ActivityManager = mock()

    @Provides
    @Singleton
    fun provideVibrator(): Vibrator = mock()

    @Provides
    @Singleton
    fun provideInputMethodManager(): InputMethodManager = mock()

    @Provides
    @Singleton
    fun provideSharedPreferences(): SharedPreferences = mock()
}