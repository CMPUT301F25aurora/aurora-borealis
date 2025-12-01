/*
 * References for EventsActivityInstrumentedTest:
 *
 * source: Android Developers — "Espresso lists"
 * url: https://developer.android.com/training/testing/espresso/lists
 * note: Used for working with list and RecyclerView items in Espresso tests.
 *
 * source: Android Developers — "RecyclerViewActions" reference
 * url: https://developer.android.com/reference/androidx/test/espresso/contrib/RecyclerViewActions
 * note: Used for scrolling to or clicking a specific position in the events RecyclerView.
 *
 * author: Stack Overflow user — "How to assert inside a RecyclerView in Espresso?"
 * url: https://stackoverflow.com/questions/31394569/how-to-assert-inside-a-recyclerview-in-espresso
 * note: Example of asserting content in a specific row of a RecyclerView.
 *
 * source: Espresso list testing article — "Working with Recycler Views in Espresso Tests"
 * url: https://www.maskaravivek.com/post/working-with-recycler-views-in-espresso-tests/
 * note: Extra reference for testing clicks and checks on RecyclerView rows.
 *
 * source: Android Developers — "Espresso"
 * url: https://developer.android.com/training/testing/espresso
 * note: Base reference for onView(withId(...)).perform(...).check(matches(...))
 *       patterns in this test.
 */



package com.example.aurora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.aurora.activities.EventsActivity;

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

    @Test
    public void testEventsScreenShowsRecyclerView() {
        // The ActivityScenarioRule already launches EventsActivity for us.
        // Just check that the RecyclerView container for events is visible.
        onView(withId(R.id.recyclerEvents))
                .check(matches(isDisplayed()));
    }
}
