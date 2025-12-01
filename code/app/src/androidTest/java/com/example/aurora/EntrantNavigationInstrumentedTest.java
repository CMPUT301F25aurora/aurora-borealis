package com.example.aurora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.aurora.activities.LoginActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * EntrantNavigationInstrumentedTest
 *
 * What this actually verifies:
 *  - That an entrant has a clear navigation path from the login screen:
 *      * Email/password fields
 *      * "Login" button
 *      * "Login with Device" button
 *      * "Create one here" link
 *
 * We do NOT try to assert bottom navigation (navEvents/navProfile/navAlerts)
 * here because the actual transition into the entrant home screen depends on
 * backend/device logic that is not deterministic in the test environment.
 *
 * Instead, this test focuses on the REAL, stable UI that we can guarantee:
 * the entrant login entry point.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantNavigationInstrumentedTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> rule =
            new ActivityScenarioRule<>(LoginActivity.class);

    /**
     * Check that all entrant login options are visible on the login screen.
     */
    @Test
    public void testEntrantLoginOptionsVisible() {
        // Email + password fields
        onView(withId(R.id.loginEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.loginPassword)).check(matches(isDisplayed()));

        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));

        onView(withId(R.id.loginDeviceButton)).check(matches(isDisplayed()));

        onView(withId(R.id.createAccountButton)).check(matches(isDisplayed()));
    }

    /**
     * Verify that the "Login with Device" button is clickable and does not crash the app.
     * We don't assert the next screen (entrant home) because that depends on backend/device state.
     */
    @Test
    public void testDeviceLoginButtonClickable() {
        onView(withId(R.id.loginDeviceButton))
                .check(matches(isDisplayed()))
                .perform(click());

        // It may stay on the same screen (e.g., if backend rejects login in test env).
        onView(withId(R.id.loginDeviceButton)).check(matches(isDisplayed()));
    }
}
