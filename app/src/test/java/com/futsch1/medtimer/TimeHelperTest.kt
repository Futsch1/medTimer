package com.futsch1.medtimer

import android.os.LocaleList
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
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
@Config(sdk = [36], application = HiltTestApplication::class)
@HiltAndroidTest
class TimeHelperTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val mockPreferenceDataSource: PreferencesDataSource = mock()

    @Before
    fun setUp() {
        hiltRule.inject()
        TimeHelper.onChangedUseSystemLocale()
    }

    @Test
    fun testLocaleHandling() {
        val context = RuntimeEnvironment.getApplication()
        context.resources.configuration.setLocales(LocaleList(Locale.US, Locale.GERMAN))
        val preferences = MutableStateFlow(UserPreferences.default())
        Mockito.`when`(mockPreferenceDataSource.preferences).thenReturn(preferences)

        assertEquals(englishDataSecondOfJan2023, TimeHelper.localDateToString(context, LocalDate.of(2023, 1, 2)))
        assertEquals(englishDataSecondOfJan2023, TimeHelper.secondSinceEpochToDateString(context, Instant.parse("2023-01-02T12:00:00Z").epochSecond))
        assertEquals(englishDataSecondOfJan2023, TimeHelper.daysSinceEpochToDateString(context, LocalDate.of(2023, 1, 2).toEpochDay()))

        TimeHelper.onChangedUseSystemLocale()
        preferences.value = preferences.value.copy(systemLocale = true)
        Mockito.`when`(mockPreferenceDataSource.preferences).thenReturn(preferences)
        assertEquals(germanDateSecondOfJan2023, TimeHelper.localDateToString(context, LocalDate.of(2023, 1, 2)))
        assertEquals(germanDateSecondOfJan2023, TimeHelper.secondSinceEpochToDateString(context, Instant.parse("2023-01-02T12:00:00Z").epochSecond))
        assertEquals(germanDateSecondOfJan2023, TimeHelper.daysSinceEpochToDateString(context, LocalDate.of(2023, 1, 2).toEpochDay()))
    }
}