package com.futsch1.medtimer.di

import javax.inject.Qualifier

/**
 * Qualifies `BuildConfig.DEBUG`. Injected rather than read directly because it is a compile-time
 * constant: logic that reads it can only ever be exercised in one build type.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsDebugBuild
