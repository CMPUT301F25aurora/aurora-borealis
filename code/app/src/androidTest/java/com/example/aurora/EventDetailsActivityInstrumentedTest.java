/*
 * References for EventDetailsActivityInstrumentedTest:
 *
 * source: Android Developers — "Espresso"
 * url: https://developer.android.com/training/testing/espresso
 * note: Used for checking that text fields, buttons, and other views in the
 *       event details screen are displayed and clickable.
 *
 * source: Android Developers — "Espresso recipes"
 * url: https://developer.android.com/training/testing/espresso/recipes
 * note: Used for examples of matching text, view visibility, and simple assertions.
 *
 * source: Android Developers — "Test your app's activities"
 * url: https://developer.android.com/guide/components/activities/testing
 * note: Used for using ActivityScenario or ActivityScenarioRule to test EventDetailsActivity.
 *
 * author: Stack Overflow user — "How to assert inside a RecyclerView in Espresso?"
 * url: https://stackoverflow.com/questions/31394569/how-to-assert-inside-a-recyclerview-in-espresso
 * note: Used if the event details screen shows lists (for example, entrants) in a RecyclerView
 *       and the test needs to assert on a specific row.
 *
 * source: ChatGPT (OpenAI assistant)
 * note: Helped refine the ideas for what UI states are worth checking in this test,
 *       such as making sure QR or deep link related views appear when expected.
 */


package com.example.aurora;

import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import com.example.aurora.activities.EventDetailsActivity;

/**
 * Very simple UI tests for EventDetailsActivity.
 *
 * Note for Testing:
 * In EventDetailsActivity.java, comment out the "finish();" line
 * inside bindEvent(DocumentSnapshot doc) before running this test.
 * Otherwise, the test will close the screen right away and fail.
 *
 * Uses a dummy eventId so Firestore does not crash.
 */
@RunWith(AndroidJUnit4.class)
public class EventDetailsActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<EventDetailsActivity> rule =
            new ActivityScenarioRule<>(
                    new Intent(
                            ApplicationProvider.getApplicationContext(),
                            EventDetailsActivity.class
                    ).putExtra("eventId", "dummyEvent123")
            );

    @Test
    public void testEventDetailsScreenVisible() {
        onView(withId(R.id.txtTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.txtSubtitle)).check(matches(isDisplayed()));
        onView(withId(R.id.txtAbout)).check(matches(isDisplayed()));
        onView(withId(R.id.btnJoinLeave)).check(matches(isDisplayed()));
    }

    @Test
    public void testJoinLeaveButtonClickable() {
        onView(withId(R.id.btnJoinLeave)).perform(click());
        onView(withId(R.id.btnJoinLeave)).check(matches(isDisplayed()));
    }

    @Test
    public void testCriteriaButtonClickable() {
        onView(withId(R.id.btnCriteria)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCriteria)).perform(click());
    }

    @Test
    public void testShowQrButtonClickable() {
        onView(withId(R.id.btnShowQr)).check(matches(isDisplayed()));
        onView(withId(R.id.btnShowQr)).perform(click());
    }
}
