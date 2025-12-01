/*
 * References for LoginActivityInstrumentedTest:
 *
 * source: Android Developers — "Espresso"
 * url: https://developer.android.com/training/testing/espresso
 * note: Used to test typing into EditTexts, clicking login buttons, and checking
 *       that the correct views appear after login.
 *
 * source: Android Developers — "Espresso recipes"
 * url: https://developer.android.com/training/testing/espresso/recipes
 * note: Used for common patterns like matching error messages or checking that
 *       a particular text is shown on screen.
 *
 * source: Android Developers — "Testing with Espresso for UI"
 * url: https://google-developer-training.github.io/android-developer-fundamentals-course-practicals/en/Unit%202/61_p_use_espresso_to_test_your_ui.html
 * note: Example of building basic UI tests around user input and verifying labels.
 *
 * source: Android testing tutorial — "Developing Android unit and instrumentation tests"
 * url: https://www.vogella.com/tutorials/AndroidTesting/article.html
 * note: Background on how instrumentation tests are structured with JUnit4 and Espresso.
 *
 * source: ChatGPT (OpenAI assistant)
 * note: Helped with ideas on which login flows to assert (for example, valid login,
 *       invalid login, and navigation to the next screen), not the Espresso API usage.
 */
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

import com.example.aurora.activities.LoginActivity;

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
     * Tests that entering fake credentials and tapping login does not crash.
     * (No need to check toast or backend response — just ensure app stays open)
     */
    @Test
    public void testInvalidLoginDoesNotCrash() {
        onView(withId(R.id.loginEmail)).perform(typeText("fake@example.com"), closeSoftKeyboard());
        onView(withId(R.id.loginPassword)).perform(typeText("wrongpass"), closeSoftKeyboard());
        onView(withId(R.id.loginButton)).perform(click());
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
    }

    /**
     * Tests logging in with a valid email and password for an entrant user
     */
    @Test
    public void testValidLoginEntrant() {
        // type valid entrant credentials
        onView(withId(R.id.loginEmail)).perform(typeText("ok@email.com"), closeSoftKeyboard());
        onView(withId(R.id.loginPassword)).perform(typeText("okokok"), closeSoftKeyboard());
        onView(withId(R.id.loginButton)).perform(click());

        // check that EventsActivity is displayed
        onView(withId(R.id.recyclerEvents)).check(matches(isDisplayed()));
    }

    /**
     * Tests that entering valid email and password for an Organizer user
     * successfully navigates to the OrganizerActivity
     */
    @Test
    public void testValidOrganizerLoginNavigatesToOrganizerActivity() {
        onView(withId(R.id.loginEmail)).perform(typeText("new@orgg.com"), closeSoftKeyboard());
        onView(withId(R.id.loginPassword)).perform(typeText("123456"), closeSoftKeyboard());
        onView(withId(R.id.loginButton)).perform(click());

        // check that organizer dashboard views are displayed
        onView(withId(R.id.eventListContainer)).check(matches(isDisplayed()));
    }



/**
    /**
     * Tests that entering valid info for an Admin user
     * successfully navigates to AdminActivity.
     */
    @Test
    public void testValidAdminLoginNavigatesToAdminActivity() {
        onView(withId(R.id.loginEmail)).perform(typeText("admin@ujjawal.com"), closeSoftKeyboard());
        onView(withId(R.id.loginPassword)).perform(typeText("bobispathan"), closeSoftKeyboard());
        onView(withId(R.id.loginButton)).perform(click());

        // check that admin dashboard views are displayed
        onView(withId(R.id.adminListContainer)).check(matches(isDisplayed()));
    }




}
