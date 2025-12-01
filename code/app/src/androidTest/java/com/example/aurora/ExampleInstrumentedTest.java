package com.example.aurora;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    /**
     * Verifies that the test is running against the correct application
     * context.
     * <p>
     * This sanity-check test obtains the target context from
     * {@link InstrumentationRegistry} and asserts that its package name
     * matches the app's expected ID {@code "com.example.aurora"}. It helps
     * confirm that instrumented tests are bound to the right APK.
     */
    @Test
    public void useAppContext() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.aurora", appContext.getPackageName());
    }
}