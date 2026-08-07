package com.futsch1.medtimer.harness

import android.app.Application
import dagger.hilt.android.testing.CustomTestApplication

@CustomTestApplication(Application::class)
interface MedTimerHiltTestApplication
