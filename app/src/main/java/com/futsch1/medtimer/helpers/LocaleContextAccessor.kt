package com.futsch1.medtimer.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LocaleContextAccessor @Inject constructor(
    @param:ApplicationContext private val base: Context
) {
    @SuppressLint("AppBundleLocaleChanges")
    fun getLocaleAwareContext(): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocales(LocaleList(base.resources.configuration.getLocales()[0]))
        return base.createConfigurationContext(configuration)
    }
}