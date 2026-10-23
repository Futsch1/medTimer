package com.futsch1.medtimer

import android.app.Application
import android.content.ComponentName
import android.content.pm.ActivityInfo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class MainActivityLaunchModeTest {
    // #1328: with singleInstance, older Android versions start the document pickers in a new task and cancel their result at once,
    // so restoring a backup and selecting the automatic backup folder silently do nothing.
    @Test
    fun `main activity keeps pickers in its own task`() {
        val context = RuntimeEnvironment.getApplication()
        val activityInfo = context.packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0)

        assertEquals(ActivityInfo.LAUNCH_SINGLE_TASK, activityInfo.launchMode)
    }
}
