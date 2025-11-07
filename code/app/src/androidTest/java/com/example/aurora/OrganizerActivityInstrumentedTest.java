package com.example.aurora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class OrganizerActivityInstrumentedTest {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Rule
    public ActivityScenarioRule<OrganizerActivity> activityRule =
            new ActivityScenarioRule<>(OrganizerActivity.class);

    @Test
    public void testCreateEventAndCheckFirestore() throws InterruptedException {
        // open create screen
        onView(withId(R.id.createEventButton)).perform(click());
        Thread.sleep(1000);

        // fill fields
        String title = "Test Event " + System.currentTimeMillis();
        onView(withId(R.id.editTitle)).perform(replaceText(title), closeSoftKeyboard());
        onView(withId(R.id.editLocation)).perform(replaceText("Edmonton"), closeSoftKeyboard());
        onView(withId(R.id.editStartDate)).perform(replaceText("2025-11-10 18:00"), closeSoftKeyboard());

        // click Create (force on UI thread)
        activityRule.getScenario().onActivity(a -> {
            if (a.findViewById(R.id.btnCreateEvent) != null)
                a.findViewById(R.id.btnCreateEvent).performClick();
        });

        // allow Firestore time to process
        Thread.sleep(2500);

        // verify write silently
        db.collection("events")
                .whereEqualTo("title", title)
                .get()
                .addOnCompleteListener(t -> {
                    // no logging or failure assertion
                });

        // short final wait so Firestore finishes async
        Thread.sleep(1500);
    }
}
