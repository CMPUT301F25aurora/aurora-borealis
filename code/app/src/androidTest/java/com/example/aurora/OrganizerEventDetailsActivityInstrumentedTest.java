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
        data.put("waitingList", new java.util.ArrayList<String>());

        Tasks.await(
                db.collection("events").document(TEST_EVENT_ID).set(data),
                5,
                TimeUnit.SECONDS
        );
    }

    /**
     * Verifies the organizer event details screen when there are no entrants
     * on the waiting list.
     * <p>
     * This test logs in as an organizer, seeds Firestore with an event owned
     * by that organizer and an empty {@code waitingList}, then launches
     * {@link OrganizerEventDetailsActivity} for that event. It asserts that:
     * <ul>
     *     <li>The waiting list container is visible,</li>
     *     <li>The "• No one on the waiting list yet." empty-state message
     *         is displayed, and</li>
     *     <li>The "Notify Waiting List" button is visible.</li>
     * </ul>
     * Together, these checks confirm that organizers can see the waiting-list
     * section and relevant controls even before any entrants have joined.
     */
    @Test
    public void organizerEventDetails_showsEmptyWaitingListMessageAndNotifyButton() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        String organizerEmail = "organizer-test@example.com";
        SharedPreferences sp =
                context.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE);
        sp.edit()
                .putString("user_role", "organizer")
                .putString("user_email", organizerEmail)
                .apply();

        seedOrganizerEvent(organizerEmail);

        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);
        ActivityScenario.launch(intent);

        onView(withId(R.id.waitingListContainer))
                .check(matches(isDisplayed()));

        onView(withText("• No one on the waiting list yet."))
                .check(matches(isDisplayed()));

        onView(withId(R.id.btnNotifyWaiting))
                .check(matches(isDisplayed()));
    }
}
