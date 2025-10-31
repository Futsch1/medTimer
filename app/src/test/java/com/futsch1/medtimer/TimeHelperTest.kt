package com.futsch1.medtimer

import android.content.SharedPreferences
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
import org.robolectric.RuntimeEnvironment
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
        val context = RuntimeEnvironment.getApplication()
        context.resources.configuration.setLocales(LocaleList(Locale.US, Locale.GERMAN))

        val preferencesMock = mock(SharedPreferences::class.java)
        Mockito.`when`(preferencesMock.getBoolean(SYSTEM_LOCALE, false)).thenReturn(false)
        val preferencesManager = mockStatic(PreferenceManager::class.java)
        preferencesManager.`when`<Any> { PreferenceManager.getDefaultSharedPreferences(context) }.thenReturn(preferencesMock)
        assertEquals("1/2/23", TimeHelper.localDateToString(context, LocalDate.of(2023, 1, 2)))
        assertEquals("1/2/23", TimeHelper.secondSinceEpochToDateString(context, Instant.parse("2023-01-02T12:00:00Z").epochSecond))
        assertEquals("1/2/23", TimeHelper.daysSinceEpochToDateString(context, LocalDate.of(2023, 1, 2).toEpochDay()))
        Mockito.`when`(preferencesMock.getBoolean(SYSTEM_LOCALE, false)).thenReturn(true)
        assertEquals("02.01.23", TimeHelper.localDateToString(context, LocalDate.of(2023, 1, 2)))
        assertEquals("02.01.23", TimeHelper.secondSinceEpochToDateString(context, Instant.parse("2023-01-02T12:00:00Z").epochSecond))
        assertEquals("02.01.23", TimeHelper.daysSinceEpochToDateString(context, LocalDate.of(2023, 1, 2).toEpochDay()))

        preferencesManager.close()
    }
}