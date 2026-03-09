package com.futsch1.medtimer

import android.content.SharedPreferences
import android.os.LocaleList
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.preferences.PreferencesNames.SYSTEM_LOCALE
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import kotlin.test.assertEquals

private const val englishDataSecondOfJan2023 = "1/2/23"

private const val germanDateSecondOfJan2023 = "02.01.23"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimeHelperTest {

    @Before
    fun setUp() {
        TimeHelper.onChangedUseSystemLocale()
    }

    @Test
    fun testLocaleHandling() {
        val context = RuntimeEnvironment.getApplication()
        context.resources.configuration.setLocales(LocaleList(Locale.US, Locale.GERMAN))

        val preferencesMock = mock(SharedPreferences::class.java)
        Mockito.`when`(preferencesMock.getBoolean(SYSTEM_LOCALE, false)).thenReturn(false)
        val preferencesManager = mockStatic(PreferenceManager::class.java)
        preferencesManager.`when`<Any> { PreferenceManager.getDefaultSharedPreferences(context) }.thenReturn(preferencesMock)
        assertEquals(englishDataSecondOfJan2023, TimeHelper.localDateToString(context, LocalDate.of(2023, 1, 2)))
        assertEquals(englishDataSecondOfJan2023, TimeHelper.secondSinceEpochToDateString(context, Instant.parse("2023-01-02T12:00:00Z").epochSecond))
        assertEquals(englishDataSecondOfJan2023, TimeHelper.daysSinceEpochToDateString(context, LocalDate.of(2023, 1, 2).toEpochDay()))

        TimeHelper.onChangedUseSystemLocale()
        Mockito.`when`(preferencesMock.getBoolean(SYSTEM_LOCALE, false)).thenReturn(true)
        assertEquals(germanDateSecondOfJan2023, TimeHelper.localDateToString(context, LocalDate.of(2023, 1, 2)))
        assertEquals(germanDateSecondOfJan2023, TimeHelper.secondSinceEpochToDateString(context, Instant.parse("2023-01-02T12:00:00Z").epochSecond))
        assertEquals(germanDateSecondOfJan2023, TimeHelper.daysSinceEpochToDateString(context, LocalDate.of(2023, 1, 2).toEpochDay()))

        preferencesManager.close()
    }
}