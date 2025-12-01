package com.example.aurora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.aurora.activities.OrganizerEventDetailsActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * UI tests for OrganizerEventDetailsActivity:
 * - Organizer can see the waiting list section.
 * - When the waiting list is empty, a "no one on the waiting list" row appears.
 * - The "Notify Waiting List" button is visible.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerEventDetailsActivityInstrumentedTest {

    private static final String TEST_EVENT_ID = "organizer-event-test-123";

    /**
     * Seeds a minimal event document owned by the test organizer,
     * with an empty waiting list.
     */
    private void seedOrganizerEvent(String organizerEmail) throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("title", "Organizer Test Event");
        data.put("date", "2025-01-01");
        data.put("location", "Test Pool");
        data.put("category", "Swimming");
        data.put("registrationStart", "2025-01-01");
        data.put("registrationEnd", "2025-01-10");
        data.put("organizerEmail", organizerEmail);
        // Explicitly empty waiting list
        data.put("waitingList", new java.util.ArrayList<String>());

        // Ensure the doc exists before launching the Activity
        Tasks.await(
                db.collection("events").document(TEST_EVENT_ID).set(data),
                5,
                TimeUnit.SECONDS
        );
    }

    @Test
    public void organizerEventDetails_showsEmptyWaitingListMessageAndNotifyButton() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        // Pretend logged in as this organizer
        String organizerEmail = "organizer-test@example.com";
        SharedPreferences sp =
                context.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE);
        sp.edit()
                .putString("user_role", "organizer")
                .putString("user_email", organizerEmail)
                .apply();

        // Seed the event owned by this organizer with an empty waiting list
        seedOrganizerEvent(organizerEmail);

        // Launch OrganizerEventDetailsActivity for this event
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);
        ActivityScenario.launch(intent);

        // 1) Waiting list container should exist
        onView(withId(R.id.waitingListContainer))
                .check(matches(isDisplayed()));

        // 2) Because waitingList is empty, the special "no one" row should appear
        onView(withText("• No one on the waiting list yet."))
                .check(matches(isDisplayed()));

        // 3) The "Notify Waiting List" button should be visible for the organizer
        onView(withId(R.id.btnNotifyWaiting))
                .check(matches(isDisplayed()));
    }
}
