package com.futsch1.medtimer

import android.os.LocaleList
import com.futsch1.medtimer.helpers.TimeHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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

        assertEquals(englishDataSecondOfJan2023, TimeHelper.localDateToString(context, LocalDate.of(2023, 1, 2)))
        assertEquals(englishDataSecondOfJan2023, TimeHelper.secondSinceEpochToDateString(context, Instant.parse("2023-01-02T12:00:00Z").epochSecond))
        assertEquals(englishDataSecondOfJan2023, TimeHelper.daysSinceEpochToDateString(context, LocalDate.of(2023, 1, 2).toEpochDay()))

        TimeHelper.onChangedUseSystemLocale()
        assertEquals(germanDateSecondOfJan2023, TimeHelper.localDateToString(context, LocalDate.of(2023, 1, 2)))
        assertEquals(germanDateSecondOfJan2023, TimeHelper.secondSinceEpochToDateString(context, Instant.parse("2023-01-02T12:00:00Z").epochSecond))
        assertEquals(germanDateSecondOfJan2023, TimeHelper.daysSinceEpochToDateString(context, LocalDate.of(2023, 1, 2).toEpochDay()))
    }
}