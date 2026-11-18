package com.futsch1.medtimer.feature.ui.helpers

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import kotlin.test.assertEquals

/** Back and a tap outside count as Cancel, so a caller waiting on the dialog always gets an answer. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TextInputDialogBuilderTest {

    private lateinit var activity: FragmentActivity
    private var cancelCount = 0

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
        TextInputDialogBuilder(activity).textSink { }.cancelCallback { cancelCount++ }.show()
    }

    @Test
    fun backCallsTheCancelCallback() {
        ShadowDialog.getLatestDialog().cancel()
        shadowOf(activity.mainLooper).idle()

        assertEquals(1, cancelCount)
    }

    @Test
    fun theCancelButtonCallsTheCancelCallbackOnce() {
        (ShadowDialog.getLatestDialog() as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        shadowOf(activity.mainLooper).idle()

        assertEquals(1, cancelCount)
    }

    @Test
    fun okDoesNotCallTheCancelCallback() {
        (ShadowDialog.getLatestDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(activity.mainLooper).idle()

        assertEquals(0, cancelCount)
    }
}
