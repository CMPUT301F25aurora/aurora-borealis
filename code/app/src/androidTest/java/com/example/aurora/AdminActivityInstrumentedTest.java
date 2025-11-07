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

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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
                // IMPORTANT: increment *after* the comparison so only one view returns true
                if (matcher.matches(view)) {
                    return currentIndex++ == index;
                }
                return false;
            }
        };
    }

    // Browsing tests
    @Test
    public void testBrowseEventsTabShowsList() {
        onView(withId(R.id.tabEvents)).perform(click());
        onView(withId(R.id.adminListContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testBrowseProfilesTabShowsList() {
        onView(withId(R.id.tabProfiles)).perform(click());
        onView(withId(R.id.adminListContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testLogsTabShowsActivityLogsTitle() {
        onView(withId(R.id.tabLogs)).perform(click());
        onView(withText("Activity Logs")).check(matches(isDisplayed()));
    }

    // Remove Event confirmation
    @Test
    public void testRemoveEventShowsConfirmationDialog() {
        // Go to Events tab
        onView(withId(R.id.tabEvents)).perform(click());

        // Click the FIRST remove button in the list of events
        onView(withIndex(withId(R.id.adminEventRemoveButton), 0)).perform(click());

        // Verify the confirmation dialog appears
        onView(withText("Remove Event")).check(matches(isDisplayed()));
        onView(withText("Remove")).check(matches(isDisplayed()));
        onView(withText("Cancel")).check(matches(isDisplayed()));

        // Close dialog so test ends cleanly
        onView(withText("Cancel")).perform(click());
    }
    // Remove Profile confirmation

    @Test
    public void testRemoveProfileShowsConfirmationDialog() {
        // Go to Profiles tab
        onView(withId(R.id.tabProfiles)).perform(click());

        // Click the FIRST remove button in the list of profiles
        onView(withIndex(withId(R.id.adminProfileRemoveButton), 0)).perform(click());

        // Verify the confirmation dialog appears
        onView(withText("Remove Profile")).check(matches(isDisplayed()));
        onView(withText("Remove")).check(matches(isDisplayed()));
        onView(withText("Cancel")).check(matches(isDisplayed()));

        // Close dialog
        onView(withText("Cancel")).perform(click());
    }
}
