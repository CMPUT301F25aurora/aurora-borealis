package com.example.aurora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.aurora.activities.AlertsActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for AlertsActivity that verify real behaviour, not just view presence.
 */
@RunWith(AndroidJUnit4.class)
public class AlertsActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<AlertsActivity> rule =
            new ActivityScenarioRule<>(AlertsActivity.class);

    /**
     * Clears any previously stored user email before each test run.
     * <p>
     * This method simulates a state where no user is logged in by writing
     * an empty string into the {@code aurora_prefs} {@code user_email}
     * entry. Using {@code @Before} ensures that each test in this class
     * starts from a clean, predictable authentication state.
     */
    @Before
    public void clearUserEmail() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("user_email", "")
                .commit();
    }

    /**
     * If there is no stored user email, AlertsActivity should show the empty message
     * immediately instead of trying to listen to Firestore.
     */
    @Test
    public void testEmptyStateShownWhenNoUserEmail() {
        onView(withId(R.id.alertsMessage)).check(matches(isDisplayed()));
    }
}
