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
