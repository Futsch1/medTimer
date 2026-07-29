package com.futsch1.medtimer.harness

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class MedTimerHiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application =
        super.newApplication(cl, MedTimerHiltTestApplication_Application::class.java.name, context)
}
