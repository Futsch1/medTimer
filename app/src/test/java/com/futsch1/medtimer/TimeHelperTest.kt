package com.futsch1.medtimer

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.LocaleList
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.preferences.PreferencesNames.SYSTEM_LOCALE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [34])
@SuppressWarnings("java:S5786") // Required for Robolectric extension
class TimeHelperTest {

    @Test
    fun testLocaleHandling() {
        val localeList = mock(LocaleList::class.java)
        Mockito.`when`(localeList.size()).thenReturn(2)
        Mockito.`when`(localeList.get(0)).thenReturn(Locale.US)
        Mockito.`when`(localeList.get(1)).thenReturn(Locale.GERMAN)
        val configuration = mock(android.content.res.Configuration::class.java)
        Mockito.`when`(configuration.locales).thenReturn(localeList)
        val resources = mock(Resources::class.java)
        Mockito.`when`(resources.configuration).thenReturn(configuration)
        val resourcesStatic = mockStatic(Resources::class.java)
        resourcesStatic.`when`<Any> { Resources.getSystem() }.thenReturn(resources)
        val context = mock(Context::class.java)
        Mockito.`when`(context.resources).thenReturn(resources)
        val preferencesMock = mock(SharedPreferences::class.java)
        Mockito.`when`(preferencesMock.getBoolean(SYSTEM_LOCALE, false)).thenReturn(false)
        val preferencesManager = mockStatic(PreferenceManager::class.java)
        preferencesManager.`when`<Any> { PreferenceManager.getDefaultSharedPreferences(context) }.thenReturn(preferencesMock)
        assertEquals("1/2/23", TimeHelper.toLocalizedDateString(context, LocalDate.of(2023, 1, 2)))
        assertEquals("1/2/23", TimeHelper.toLocalizedDateString(context, Instant.parse("2023-01-02T12:00:00Z").epochSecond))
        assertEquals("1/2/23", TimeHelper.daysSinceEpochToDateString(context, LocalDate.of(2023, 1, 2).toEpochDay()))
        Mockito.`when`(preferencesMock.getBoolean(SYSTEM_LOCALE, false)).thenReturn(true)
        assertEquals("02.01.23", TimeHelper.toLocalizedDateString(context, LocalDate.of(2023, 1, 2)))
        assertEquals("02.01.23", TimeHelper.toLocalizedDateString(context, Instant.parse("2023-01-02T12:00:00Z").epochSecond))
        assertEquals("02.01.23", TimeHelper.daysSinceEpochToDateString(context, LocalDate.of(2023, 1, 2).toEpochDay()))
    }
}