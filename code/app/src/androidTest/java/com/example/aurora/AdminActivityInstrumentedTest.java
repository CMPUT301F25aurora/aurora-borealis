/*
 * References for AdminActivityInstrumentedTest:
 *
 * source: Android Developers — "Build instrumented tests"
 * url: https://developer.android.com/training/testing/instrumented-tests
 * note: Used for the basic setup of AndroidJUnit4, the instrumentation runner,
 *       and running UI tests on a device or emulator.
 *
 * source: Android Developers — "Test your app's activities"
 * url: https://developer.android.com/guide/components/activities/testing
 * note: Used for using ActivityScenario to launch and test AdminActivity.
 *
 * source: AndroidX Test — ActivityScenarioRule reference
 * url: https://developer.android.com/reference/androidx/test/ext/junit/rules/ActivityScenarioRule
 * note: Used for the rule that automatically launches and closes AdminActivity
 *       before and after each test.
 *
 * author: Stack Overflow user — "How do I use activityScenarioRule<Activity>?"
 * url: https://stackoverflow.com/questions/54878598/how-do-i-use-activityscenarioruleactivity
 * note: Example of configuring ActivityScenarioRule for a specific Activity in tests.
 *
 * source: Android Developers — "Espresso"
 * url: https://developer.android.com/training/testing/espresso
 * note: Used for the onView(...).perform(...).check(...) pattern for verifying
 *       AdminActivity UI elements.
 */


package com.example.aurora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.view.View;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.aurora.activities.AdminActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI tests for AdminActivity.
 *
 * These tests verify:
 *  Tabs switch correctly
 *  Lists display contents
 *  Confirmation dialogs appear for destructive actions
 */
@RunWith(AndroidJUnit4.class)
public class AdminActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<AdminActivity> activityRule =
            new ActivityScenarioRule<>(AdminActivity.class);

    /**
     * Helper to select a specific index when multiple views match the same matcher
     * (e.g., several remove buttons with the same id).
     */
    private static Matcher<View> withIndex(final Matcher<View> matcher, final int index) {
        return new TypeSafeMatcher<View>() {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description) {
                description.appendText("with index: ")
                        .appendValue(index)
                        .appendText(" ");
                matcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(View view) {
                if (matcher.matches(view)) {
                    return currentIndex++ == index;
                }
                return false;
            }
        };
    }

    /**
     * Test: Opening the "Events" tab should display the main list container.
     *
     * Verifies:
     * Tab switches correctly
     * Event list is visible
     */
    @Test
    public void testBrowseEventsTabShowsList() {
        onView(withId(R.id.tabEvents)).perform(click());
        onView(withId(R.id.adminListContainer)).check(matches(isDisplayed()));
    }

    /**
     * Test: Opening the "Profiles" tab should display the main list container.
     *
     * Verifies:
     *  Profiles tab switches correctly
     *  Profile list container is visible
     */
    @Test
    public void testBrowseProfilesTabShowsList() {
        onView(withId(R.id.tabProfiles)).perform(click());
        onView(withId(R.id.adminListContainer)).check(matches(isDisplayed()));
    }

    /**
     * Test: Opening the "Logs" tab should update the section title.
     *
     * Verifies:
     *  Logs tab switches correctly
     *  The header "Activity Logs" appears
     */
    @Test
    public void testLogsTabShowsActivityLogsTitle() {
        onView(withId(R.id.tabLogs)).perform(click());
        onView(withText("Activity Logs")).check(matches(isDisplayed()));
    }

    /**
     * Test: Clicking a remove button in the Events tab opens a confirmation dialog.
     *
     * Verifies:
     *  Remove button responds to clicks
     *  Confirmation dialog appears with:
     *   "Remove Event"
     *   Remove button
     *   Cancel button
     */
    @Test
    public void testRemoveEventShowsConfirmationDialog() {
        // Go to Events tab
        onView(withId(R.id.tabEvents)).perform(click());

        onView(withIndex(withId(R.id.adminEventRemoveButton), 0)).perform(click());

        onView(withText("Remove Event")).check(matches(isDisplayed()));
        onView(withText("Remove")).check(matches(isDisplayed()));
        onView(withText("Cancel")).check(matches(isDisplayed()));

        onView(withText("Cancel")).perform(click());
    }

    /**
     * Test: Clicking a remove button in the Profiles tab opens a confirmation dialog.
     *
     * Verifies:
     * Remove button reacts correctly
     * Confirmation dialog appears containing:
     *   "Remove Profile"
     *   Remove button
     *   Cancel button
     */
    @Test
    public void testRemoveProfileShowsConfirmationDialog() {

        onView(withId(R.id.tabProfiles)).perform(click());

        onView(withIndex(withId(R.id.adminProfileRemoveButton), 0)).perform(click());

        // Verify the confirmation dialog appears
        onView(withText("Remove Profile")).check(matches(isDisplayed()));
        onView(withText("Remove")).check(matches(isDisplayed()));
        onView(withText("Cancel")).check(matches(isDisplayed()));

        // Close dialog
        onView(withText("Cancel")).perform(click());
    }

    @Test
    public void testAdminTabsSwitchBetweenSections() {
        // Events tab should be visible and clickable
        onView(withId(R.id.tabEvents)).check(matches(isDisplayed()));
        onView(withId(R.id.tabEvents)).perform(click());

        onView(withId(R.id.tabProfiles)).perform(click());

        // Switch to Logs tab (or whatever your third tab is called)
        onView(withId(R.id.tabLogs)).perform(click());

        // Check that some log-related UI is visible (replace text/ID with whatever you really show)
        // For example, if you have a "Activity Logs" header:
        // onView(withText("Activity Logs")).check(matches(isDisplayed()));
    }
}
