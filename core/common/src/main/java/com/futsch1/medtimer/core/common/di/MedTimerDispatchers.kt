package com.futsch1.medtimer.core.common.di

import javax.inject.Qualifier

enum class MedTimerDispatchers {
    IO,
    Default,
    Main
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class Dispatcher(val dispatcher: MedTimerDispatchers)
