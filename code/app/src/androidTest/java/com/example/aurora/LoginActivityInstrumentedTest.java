package com.example.aurora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple beginner-friendly UI tests for LoginActivity.
 * Checks that buttons and basic navigation work correctly.
 */
@RunWith(AndroidJUnit4.class)
public class LoginActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> rule =
            new ActivityScenarioRule<>(LoginActivity.class);

    /**
     * Tests that the Login screen loads and shows the main buttons.
     */
    @Test
    public void testLoginScreenIsVisible() {
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
        onView(withId(R.id.createAccountButton)).check(matches(isDisplayed()));
    }

    /**
     * Tests that clicking the "Create Account" button opens the SignUpActivity.
     */
    @Test
    public void testCreateAccountNavigatesToSignUp() {
        onView(withId(R.id.createAccountButton)).perform(click());
        onView(withId(R.id.SignUpButton)).check(matches(isDisplayed()));
    }

    /**
     * Tests that entering fake credentials and tapping login does not crash.
     * (No need to check toast or backend response — just ensure app stays open.)
     */
    @Test
    public void testInvalidLoginDoesNotCrash() {
        onView(withId(R.id.loginEmail)).perform(typeText("fake@example.com"), closeSoftKeyboard());
        onView(withId(R.id.loginPassword)).perform(typeText("wrongpass"), closeSoftKeyboard());
        onView(withId(R.id.loginButton)).perform(click());
        // Verify that the login screen is still visible (no crash or change)
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
    }
}
