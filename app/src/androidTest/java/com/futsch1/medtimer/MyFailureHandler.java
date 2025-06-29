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

import org.hamcrest.Matcher;
import org.junit.rules.TestName;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modified failure handler that includes name of test in screenshot name.
 */
public final class MyFailureHandler implements FailureHandler {

    private static final AtomicInteger failureCount = new AtomicInteger(0);
    private final DefaultFailureHandler defaultFailureHandler;
    private final String testName;
    private final TestName testFunctionName;

    public MyFailureHandler(
            String testName,
            TestName testFunctionName, @TargetContext Context appContext) {
        this.testName = testName;
        this.testFunctionName = testFunctionName;
        // Use the default handler, but manage screenshots ourselves
        defaultFailureHandler = new DefaultFailureHandler(appContext, false);
    }

    @Override
    public void handle(Throwable error, Matcher<View> viewMatcher) {
        int count = failureCount.incrementAndGet();
        try {
            TestOutputEmitter.captureWindowHierarchy("explore-window-hierarchy-" + count + ".xml");
            takeScreenshot("view-op-error-" + this.testName + "." + this.testFunctionName.getMethodName() + "_" + count);
        } catch (RuntimeException screenshotException) {
            // Ensure that the root cause exception is surfaced, not an auxiliary exception that may occur
            // during the capture/screenshot process.
            error.addSuppressed(screenshotException);
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
