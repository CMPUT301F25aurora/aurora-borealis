/*
 * References for EntrantNavigationInstrumentedTest:
 *
 * source: Android Developers — "Espresso"
 * url: https://developer.android.com/training/testing/espresso
 * note: Used for basic Espresso patterns to find views, click buttons, and check text.
 *
 * source: Android Developers — "Espresso recipes"
 * url: https://developer.android.com/training/testing/espresso/recipes
 * note: Used as a reference for common navigation checks, matching views next to other views,
 *       and simple UI flows.
 *
 * source: Android Developers — "Test your app's activities"
 * url: https://developer.android.com/guide/components/activities/testing
 * note: Used for testing that navigation between entrant screens behaves correctly
 *       when activities are launched in tests.
 *
 * author: Stack Overflow user — "Instrumented Tests in Android Studio"
 * url: https://stackoverflow.com/questions/32348695/instrumented-tests-in-android-studio
 * note: General example of how to structure instrumentation tests and run them from Android Studio.
 */


package com.example.aurora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for EntrantNavigationActivity.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantNavigationInstrumentedTest {

    @Rule
    public ActivityScenarioRule<EntrantNavigationActivity> rule =
            new ActivityScenarioRule<>(EntrantNavigationActivity.class);

    @Test
    public void testTabsDisplayedAndClickable() {
        onView(withId(R.id.navEvents)).check(matches(isDisplayed()));
        onView(withId(R.id.navProfile)).perform(click());
        onView(withId(R.id.navAlerts)).perform(click());
    }
}
