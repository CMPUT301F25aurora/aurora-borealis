package com.example.aurora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.aurora.activities.EventDetailsActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * UI tests for EventDetailsActivity that work with the real behaviour:
 * - bindEvent(...) calls finish() when the event doc does NOT exist.
 *
 * We avoid that branch by seeding a dummy event in Firestore before launching.
 */
@RunWith(AndroidJUnit4.class)
public class EventDetailsActivityInstrumentedTest {

    private static final String TEST_EVENT_ID = "test-event-details-123";

    /**
     * Create a minimal Firestore event doc so that doc.exists() == true
     * inside EventDetailsActivity.bindEvent(...).
     */
    private void seedTestEvent() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        // You can add more fields if your UI expects them, but it's not required
        data.put("title", "Test Event");
        data.put("about", "Test about text shown in details.");
        data.put("description", "Test description");
        data.put("capacity", 10L);

        // Write and wait for completion so the doc definitely exists
        Tasks.await(
                db.collection("events").document(TEST_EVENT_ID).set(data),
                5,
                TimeUnit.SECONDS
        );
    }

    /**
     * Sets user_role and seeds the event before launching the activity.
     */
    private void launchEventDetailsScreen() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        // Pretend we are already logged in as entrant (avoid Welcome redirect)
        SharedPreferences sp =
                context.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE);
        sp.edit()
                .putString("user_role", "entrant")
                .apply();

        // Make sure the test event document exists
        seedTestEvent();

        Intent intent = new Intent(context, EventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        ActivityScenario.launch(intent);
    }

    /**
     * Test: Event details screen loads and critical views are visible.
     *
     * Verifies:
     *  Title is visible
     *  About text is visible
     *  Criteria and QR buttons are visible
     *  Sign Up button is GONE by default
     */
    @Test
    public void testEventDetailsScreenVisible() throws Exception {
        launchEventDetailsScreen();

        onView(withId(R.id.txtTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.txtAbout)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCriteria)).check(matches(isDisplayed()));
        onView(withId(R.id.btnShowQr)).check(matches(isDisplayed()));

        // Sign Up button exists in the layout but is GONE by default
        onView(withId(R.id.btnSignUp))
                .check(matches(withEffectiveVisibility(
                        androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
                )));
    }

    /**
     * Test: Sign Up button should always be hidden in this screen.
     *
     * Verifies:
     *  Sign Up button visibility is GONE
     */
    @Test
    public void testSignUpButtonIsHiddenByDefault() throws Exception {
        launchEventDetailsScreen();

        onView(withId(R.id.btnSignUp))
                .check(matches(withEffectiveVisibility(
                        androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
                )));
    }

    /**
     * Test: Criteria button should be clickable.
     *
     * Verifies:
     *  Button is visible
     *  Button can be tapped
     */
    @Test
    public void testCriteriaButtonClickable() throws Exception {
        launchEventDetailsScreen();

        onView(withId(R.id.btnCriteria)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCriteria)).perform(click());
    }

    /**
     * Test: QR button should be clickable.
     *
     * Verifies:
     *  Button is visible
     *  Button responds to click action
     */
    @Test
    public void testShowQrButtonClickable() throws Exception {
        launchEventDetailsScreen();

        onView(withId(R.id.btnShowQr)).check(matches(isDisplayed()));
        onView(withId(R.id.btnShowQr)).perform(click());
    }
}
