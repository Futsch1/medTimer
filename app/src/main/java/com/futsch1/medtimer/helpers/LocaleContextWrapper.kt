package com.futsch1.medtimer.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList

class LocaleContextWrapper(base: Context) : ContextWrapper(base) {
    init {
        buildContext(base)
    }

    override fun getResources(): Resources {
        return instance!!.resources
    }

    companion object {
        // No leak because this context is just a single copy of the enclosing context
        @SuppressLint("StaticFieldLeak")
        private var instance: Context? = null

        // This is ok because the locale is not changed for the complete context, only wrapped for the DateFormat calls
        @SuppressLint("AppBundleLocaleChanges")
        @Synchronized
        private fun buildContext(base: Context) {
            if (instance != null) {
                return
            }

            if (base.resources != null && base.resources.configuration != null) {
                val configuration = Configuration(base.resources.configuration)
                configuration.setLocales(LocaleList(base.resources.configuration.getLocales()[0]))
                instance = base.createConfigurationContext(configuration)
            } else {
                instance = base
            }
        }

        @Synchronized
        fun resetLocaleContextWrapper() {
            instance = null
        }
    }
}