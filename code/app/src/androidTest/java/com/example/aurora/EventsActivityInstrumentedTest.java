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
 * UI tests for EventsActivity.
 */
@RunWith(AndroidJUnit4.class)
public class EventsActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<EventsActivity> rule =
            new ActivityScenarioRule<>(EventsActivity.class);

    @Test
    public void testCategoryButtonsDisplayed() {
        onView(withId(R.id.btnAll)).check(matches(isDisplayed()));
        onView(withId(R.id.btnMusic)).check(matches(isDisplayed()));
        onView(withId(R.id.btnSports)).check(matches(isDisplayed()));
    }

    @Test
    public void testFilterButtonOpensDialog() {
        onView(withId(R.id.btnFilter)).perform(click());
    }

    @Test
    public void testScanQrButtonDisplayed() {
        onView(withId(R.id.btnScanQr)).check(matches(isDisplayed()));
    }
}
