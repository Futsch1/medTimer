package com.futsch1.medtimer.feature.ui.preferences

import android.content.Context
import android.media.RingtoneManager
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.feature.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Alarm settings crashed on API 28-30 because the attribute read in RingtonePreference closed its TypedArray
 * through AutoCloseable, which TypedArray implements only from API 31. Inflate the real screen on the affected levels.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 29, 30])
class RingtonePreferenceTest {
    private val context: Context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        androidx.appcompat.R.style.Theme_AppCompat_Light
    )

    private fun inflateRingtonePreference(): RingtonePreference {
        val screen = PreferenceManager(context).inflateFromResource(context, R.xml.alarm_settings, null)
        val preference = screen.findPreference<RingtonePreference>("alarm_ringtone")
        assertNotNull(preference)
        return preference!!
    }

    @Test
    fun alarmSettingsInflateWithTheRingtoneAttributes() {
        val preference = inflateRingtonePreference()

        assertEquals(RingtoneManager.TYPE_ALARM, preference.ringtoneType)
        assertTrue(preference.showDefault)
        assertEquals(false, preference.showSilent)
    }

    @Test
    fun alarmSettingsWithoutAStoredRingtoneShowTheFallbackSummary() {
        val preference = inflateRingtonePreference()

        assertNull(preference.ringtone)
        assertEquals(context.getString(android.R.string.unknownName), preference.summary.toString())
    }
}
