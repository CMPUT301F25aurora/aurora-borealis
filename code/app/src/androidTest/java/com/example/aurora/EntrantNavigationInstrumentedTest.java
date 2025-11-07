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
