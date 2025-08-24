package com.futsch1.medtimer;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.test.core.app.DeviceCapture;
import androidx.test.core.graphics.BitmapStorage;
import androidx.test.espresso.FailureHandler;
import androidx.test.espresso.base.DefaultFailureHandler;
import androidx.test.espresso.internal.inject.TargetContext;
import androidx.test.internal.platform.util.TestOutputEmitter;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.platform.io.PlatformTestStorageRegistry;
import androidx.test.uiautomator.UiDevice;

import org.hamcrest.Matcher;
import org.junit.rules.TestName;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modified failure handler that includes name of test in screenshot name.
 */
public final class MyFailureHandler implements FailureHandler {

    private static final AtomicInteger failureCount = new AtomicInteger(0);
    private final DefaultFailureHandler defaultFailureHandler;
    private final String testName;
    private final TestName testFunctionName;
    private final UiDevice device;

    public MyFailureHandler(
            String testName,
            TestName testFunctionName, @TargetContext Context appContext) {
        this.testName = testName;
        this.testFunctionName = testFunctionName;
        // Use the default handler, but manage screenshots ourselves
        defaultFailureHandler = new DefaultFailureHandler(appContext, false);
        this.device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    @Override
    public void handle(Throwable error, Matcher<View> viewMatcher) {
        int count = failureCount.incrementAndGet();
        String fileNameBase = this.testName + "." + this.testFunctionName.getMethodName() + "_" + count;
        try {
            takeScreenshot("view-op-error-" + fileNameBase);
        } catch (RuntimeException screenshotException) {
            // Ensure that the root cause exception is surfaced, not an auxiliary exception that may occur
            // during the capture/screenshot process.
            error.addSuppressed(screenshotException);
        }
        try {
            OutputStream os = PlatformTestStorageRegistry.getInstance().openOutputFile("view-hierarchy-" + fileNameBase + ".txt");
            device.dumpWindowHierarchy(os);
        } catch (IOException e) {
            error.addSuppressed(e);
        }

        defaultFailureHandler.handle(error, viewMatcher);
    }

    private void takeScreenshot(String outputName) {
        try {
            if (DeviceCapture.canTakeScreenshot()) {
                BitmapStorage.writeToTestStorage(DeviceCapture.takeScreenshotNoSync(), outputName);
            } else {
                TestOutputEmitter.takeScreenshot(outputName + ".png");
            }
        } catch (RuntimeException | Error | IOException e) {
            Log.w("MyFailureHandler", "Failed to take screenshot", e);
        }
    }
}
