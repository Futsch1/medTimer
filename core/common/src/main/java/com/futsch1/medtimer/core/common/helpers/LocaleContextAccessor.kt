package com.futsch1.medtimer.core.common.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * A context carrying only the primary locale: with several locales in the list, the platform
 * formatters can resolve their pattern from a fallback locale rather than the one the user reads.
 */
@SuppressLint("AppBundleLocaleChanges")
fun Context.withPrimaryLocale(): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList(configuration.locales[0]))
    return createConfigurationContext(configuration)
}

class LocaleContextAccessor @Inject constructor(
    @param:ApplicationContext private val base: Context
) {
    fun getLocaleAwareContext(): Context = base.withPrimaryLocale()
}
