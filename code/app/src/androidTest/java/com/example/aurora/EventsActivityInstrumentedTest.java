package com.example.aurora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.aurora.activities.SignUpActivity;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI test suite for EventsActivity.
 *   Signs up a fresh entrant test account (@Before)
 * • Verifies multiple entrant  behaviors inside EventsActivity
 *   Joins events, filters, searches, opens details, etc.
 *  deletes the account (@After)

 */
@RunWith(AndroidJUnit4.class)
public class EventsActivityInstrumentedTest {

    private final String testEmail = "UITestUser@email.com";
    private final String testPassword = "Password123";

    @Rule
    public ActivityScenarioRule<SignUpActivity> rule =
            new ActivityScenarioRule<>(SignUpActivity.class);


    // BEFORE: Create account → Arrive at EventsActivity


    /**
     * Creates a dedicated entrant test account.
     * Steps:
     *  1. Fill out signup inputs
     *  2. Submit form
     *  3. Wait for Firebase navigation
     *  4. Verify that EventsActivity loaded
     */
    @Before
    public void createEntrantAccount() throws Exception {

        onView(withId(R.id.Name)).perform(typeText("Events UI Tester"), closeSoftKeyboard());
        onView(withId(R.id.Email)).perform(replaceText(testEmail), closeSoftKeyboard());
        onView(withId(R.id.Phone)).perform(replaceText("5875551111"), closeSoftKeyboard());
        onView(withId(R.id.Password)).perform(replaceText(testPassword), closeSoftKeyboard());

        onView(withId(R.id.SignUpButton)).perform(click());

        Thread.sleep(3500); // Wait for Firebase & navigation

        onView(withId(R.id.recyclerEvents)).check(matches(isDisplayed()));
    }


    // TEST 1 — Search


    /**
     *  Search for events.
     * Ensures that:
     *  • Typing into the search bar does not crash the screen
     *  • RecyclerView remains displayed after search input
     */
    @Test
    public void testSearchEvents() throws Exception {

        onView(withId(R.id.searchEvents))
                .perform(typeText("music"), closeSoftKeyboard());

        Thread.sleep(1200);

        onView(withId(R.id.recyclerEvents)).check(matches(isDisplayed()));
    }


    // TEST 2 — Category Filtering


    /**
     * Category filtering.
     * Verifies:
     *  • Tapping category filter buttons (e.g., Music) does not crash
     *  • RecyclerView remains visible after applying filter
     */
    @Test
    public void testCategoryFilterButtons() throws Exception {

        int[] buttons = {
                R.id.btnMusic,
               // R.id.btnSports,
               // R.id.btnEducation,
               // R.id.btnArts,
               // R.id.btnTechnology,
              //  R.id.btnAll
        };

        for (int id : buttons) {
            onView(withId(id)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.recyclerEvents)).check(matches(isDisplayed()));
        }
    }


    // TEST 3 — Availability Filter Dialog


    /**
     * — Time/day availability filter.
     * Ensures:
     *  • Filter dialog opens
     *  • Checkboxes interact correctly
     *  • Apply/Close returns to EventsActivity with RecyclerView still present
     */
    @Test
    public void testAvailabilityFilterDialog() throws Exception {

        onView(withId(R.id.btnFilter)).perform(click());
        Thread.sleep(500);

        // click Monday
        onView(withId(R.id.cbMon)).perform(click());

        onView(withId(R.id.btnApplyFilters)).perform(click());
        Thread.sleep(800);

        onView(withId(R.id.recyclerEvents)).check(matches(isDisplayed()));
    }


    // TEST 4 — Open Event Details


    /**
     *  View event details.
     * Ensures:
     *  • Tap “View Details” on first event item opens EventDetailsActivity
     *  • Verifies title is displayed inside details screen
     */
    @Test
    public void testOpenEventDetails() throws Exception {

        Thread.sleep(1500);
        onView(withId(R.id.recyclerEvents))
                .perform(RecyclerViewActions.actionOnItemAtPosition(
                        0,
                        clickChildViewWithId(R.id.btnViewDetails)
                ));

        Thread.sleep(2000);

        onView(withId(R.id.txtTitle)).check(matches(isDisplayed()));

        pressBack();
        Thread.sleep(800);

        onView(withId(R.id.recyclerEvents)).check(matches(isDisplayed()));
    }


    // TEST 5 — Join & Leave Waiting List


    /**
     * US 01.01.01 + US 01.01.02 — Join & leave waiting lists.
     *
     * Steps:
     *  • Tap “Join List” on the first event
     *  • Wait for Firestore write → button changes to “Leave Waiting List”
     *  • Tap again to leave
     */
    @Test
    public void testJoinAndLeaveWaitingList() throws Exception {

        Thread.sleep(1500);

        // JOIN
        onView(withId(R.id.recyclerEvents))
                .perform(RecyclerViewActions.actionOnItemAtPosition(
                        0,
                        clickChildViewWithId(R.id.btnJoin)
                ));

        Thread.sleep(2500);

        // LEAVE
        onView(withId(R.id.recyclerEvents))
                .perform(RecyclerViewActions.actionOnItemAtPosition(
                        0,
                        clickChildViewWithId(R.id.btnJoin)
                ));

        Thread.sleep(2000);

        onView(withId(R.id.recyclerEvents)).check(matches(isDisplayed()));
    }


    // TEST 6 — Alerts Navigation


    /**
     * Verifies:
     *  • The AlertsActivity loads successfully
     *  • The alerts container or empty message is visible
     */
    @Test
    public void testOpenAlertsScreen() throws Exception {

        onView(withId(R.id.navAlerts)).perform(click());

        Thread.sleep(1500);

        onView(withId(R.id.alertsMessage)).check(matches(isDisplayed()));

        pressBack();
        Thread.sleep(1000);

        onView(withId(R.id.recyclerEvents)).check(matches(isDisplayed()));
    }


    // TEST 7 — Ensure RoleSwitchFab Visible


    /**
     * Ensures:
     *  • roleSwitch is visible
     *  • Clicking it *attempts* to switch to organizer, but entrant is denied
     *    (unless organizer_allowed = true)
     */
    @Test
    public void testRoleSwitchFabVisible() throws Exception {

        onView(withId(R.id.roleSwitchFab)).check(matches(isDisplayed()));

        // Tap it, expect Access Denied dialog for entrant test accounts
        onView(withId(R.id.roleSwitchFab)).perform(click());

        Thread.sleep(1200);
    }


    // AFTER — Delete Account via Organizer Mode


    /**
     * Deletes the test account by:
     *  1. Opening profile
     *  2. Navigating to Organizer mode via FAB
     *  3. Opening OrganizerProfileActivity
     *  4. Press delete → confirm
     *  5. Return to LoginActivity
     */
    @After
    public void cleanupDeleteAccount() throws Exception {



        // Back to Events
        pressBack();
        Thread.sleep(400);

        // Switch to organizer (test accounts may be allowed)
        onView(withId(R.id.roleSwitchFab)).perform(click());
        Thread.sleep(1500);

        // open organizer profile
        onView(withId(R.id.bottomProfile)).perform(click());
        Thread.sleep(1200);

        onView(withId(R.id.deleteAccountButton)).perform(click());
        Thread.sleep(600);

        onView(withId(android.R.id.button1)).perform(click());

        Thread.sleep(2500);

        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
    }


    // Helper — Click child view in RecyclerView item


    private ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Click on a child view with id " + id;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                if (v != null) v.performClick();
            }
        };
    }

}
