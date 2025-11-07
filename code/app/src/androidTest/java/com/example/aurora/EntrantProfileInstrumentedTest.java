/*package com.example.aurora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;


import android.os.RemoteException;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic entrant profile test:
 * - Disables animations automatically
 * - Opens profile
 * - Enables edit mode
 * - Changes text fields
 * - Saves the profile

@RunWith(AndroidJUnit4.class)
public class EntrantProfileInstrumentedTest {

    @Rule
    public ActivityScenarioRule<ProfileActivity> activityRule =
            new ActivityScenarioRule<>(ProfileActivity.class);

    // Disable animations before any test runs


    @Test
    public void testEditAndSaveProfile() throws InterruptedException {
        // Let layout load
        Thread.sleep(1000);

        // Ensure key views are visible
        onView(withId(R.id.inputFullName)).check(matches(isDisplayed()));
        onView(withId(R.id.inputEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.inputPhone)).check(matches(isDisplayed()));
        onView(withId(R.id.editToggle)).check(matches(isDisplayed()));

        // Tap "Edit"
        onView(withId(R.id.editToggle)).perform(click());
        Thread.sleep(500);

        // Fill out the fields
        onView(withId(R.id.inputFullName)).perform(replaceText("Abubakar Shaikh"), closeSoftKeyboard());
        onView(withId(R.id.inputEmail)).perform(replaceText("abu@example.com"), closeSoftKeyboard());
        onView(withId(R.id.inputPhone)).perform(replaceText("7809990000"), closeSoftKeyboard());
        Thread.sleep(500);

        // Click "Save"
        onView(withId(R.id.btnSave)).perform(click());
        Thread.sleep(1000);

        // Verify edit button is visible again (edit mode ended)
        onView(withId(R.id.editToggle)).check(matches(isDisplayed()));*/

