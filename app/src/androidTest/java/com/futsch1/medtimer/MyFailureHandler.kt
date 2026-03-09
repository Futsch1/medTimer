package com.futsch1.medtimer

import android.content.Context
import android.util.Log
import android.view.View
import androidx.test.core.app.canTakeScreenshot
import androidx.test.core.app.takeScreenshotNoSync
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.FailureHandler
import androidx.test.espresso.base.DefaultFailureHandler
import androidx.test.espresso.internal.inject.TargetContext
import androidx.test.internal.platform.util.TestOutputEmitter
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorageRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.Matcher
import org.junit.rules.TestName
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Modified failure handler that includes name of test in screenshot name.
 */
class MyFailureHandler(
    private val testName: String,
    private val testFunctionName: TestName,
    @TargetContext appContext: Context
) : FailureHandler {
    // Use the default handler but manage screenshots ourselves
    private val defaultFailureHandler: DefaultFailureHandler =
        DefaultFailureHandler(appContext, false)
    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    override fun handle(error: Throwable, viewMatcher: Matcher<View?>?) {
        val count = failureCount.incrementAndGet()
        val fileNameBase = "${this.testName}.${this.testFunctionName.methodName}_$count"
        try {
            takeScreenshot("view-op-error-$fileNameBase")
        } catch (screenshotException: RuntimeException) {
            // Ensure that the root cause exception is surfaced, not an auxiliary exception that may occur
            // during the capture/screenshot process.
            error.addSuppressed(screenshotException)
        }
        try {
            val os = PlatformTestStorageRegistry.getInstance()
                .openOutputFile("view-hierarchy-$fileNameBase.txt")
            device.dumpWindowHierarchy(os)
        } catch (e: IOException) {
            error.addSuppressed(e)
        }

        defaultFailureHandler.handle(error, viewMatcher)
    }

    private fun takeScreenshot(outputName: String) {
        try {
            if (canTakeScreenshot()) {
                takeScreenshotNoSync().writeToTestStorage(outputName)
            } else {
                TestOutputEmitter.takeScreenshot("$outputName.png")
            }
        } catch (e: RuntimeException) {
            logWarning()
        } catch (e: Error) {
            logWarning()
        } catch (e: IOException) {
            logWarning()
        }
    }

    private fun logWarning() {
        Log.w("MyFailureHandler", "Failed to take screenshot", e)
    }

    companion object {
        private val failureCount = AtomicInteger(0)
    }
}
