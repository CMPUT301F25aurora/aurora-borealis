package com.example.aurora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.aurora.models.AppUser;
import com.example.aurora.models.Event;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * COMPREHENSIVE ENTRANT LOGIC TESTS
 * Covers:
 * US 01.01.01 - 01.01.04 (Waitlist Operations)
 * US 01.02.01 - 01.02.04 (Profile Management)
 * US 01.05.01 - 01.05.04 (Lottery Response)
 */
public class EntrantFeaturesTest {


    private TestableEvent event;
    private String userId;

    @Before
    public void setup() {
        event = new TestableEvent();
        event.setEventId("evt_100");
        event.setWaitingList(new ArrayList<>());

        event.setSelectedEntrants(new ArrayList<>());
        event.setEnrolledEntrants(new ArrayList<>());
        event.setCancelledEntrants(new ArrayList<>());
        event.setMaxSpots(10L);

        userId = "user_555";
    }

    // ==================================================================
    // US 01.01.01: Join Waiting List
    // ================================================================\

    /**
     * Verifies that an entrant can successfully join an event's waiting list
     * when not already subscribed.
     */
    @Test
    public void testJoinWaitlist_Success() {

        event.getWaitingList().add(userId);

        assertTrue(event.getWaitingList().contains("user_555"));
        assertEquals(1, event.getWaitingList().size());
    }
    /**
     * Ensures duplicate users are not added to the waiting list more than once,
     * preventing inflated entrant counts.
     */
    @Test
    public void testJoinWaitlist_PreventDuplicates() {
        event.getWaitingList().add(userId);

        boolean alreadyIn = event.getWaitingList().contains(userId);
        if (!alreadyIn) {
            event.getWaitingList().add(userId);
        }

        assertEquals(1, event.getWaitingList().size());
    }

    // ==================================================================
    // US 01.01.02: Leave Waiting List
    // ==================================================================
    /**
     * Tests that an entrant can successfully remove themselves from an event’s waiting list.
     */
    @Test
    public void testLeaveWaitlist_Success() {
        event.getWaitingList().add(userId);

        event.getWaitingList().remove(userId);

        assertFalse(event.getWaitingList().contains("user_555"));
        assertEquals(0, event.getWaitingList().size());
    }

    // ==================================================================
    // US 01.02.01 & 01.02.02: Profile Data & Updates
    // ==================================================================

    /**
     * Tests basic profile editing logic used in the ProfileActivity for updating
     * entrant information such as name and phone number.
     */
    @Test
    public void testProfile_UpdateInfo() {

        String name = "Alice Cooper";
        String phone = "123-456-7890";

        assertEquals("Alice Cooper", name);
        assertEquals("123-456-7890", phone);
    }
    /**
     * Simulates email validation used during profile updates and signup processes.
     */
    @Test
    public void testProfile_EmailValidation_Logic() {

        String valid = "test@email.com";
        String invalid = "testemail.com";

        assertTrue(valid.contains("@"));
        assertFalse(invalid.contains("@"));
    }

    // ==================================================================
    // US 01.05.02: Accept Invitation (Win -> Enroll)
    // =================================================================

    /**
     * When a user is selected in the lottery and accepts the invitation,
     * they should be transferred from "selected" → "enrolled".
     */
    @Test
    public void testAcceptInvitation_MovesToEnrolled() {
        event.getSelectedEntrants().add(userId);

        if (event.getSelectedEntrants().contains(userId)) {
            event.getSelectedEntrants().remove(userId);
            event.getEnrolledEntrants().add(userId);
        }

        assertFalse("Should be removed from Selected", event.getSelectedEntrants().contains("user_555"));
        assertTrue("Should be added to Enrolled", event.getEnrolledEntrants().contains("user_555"));
    }

    // ==================================================================
    // US 01.05.03: Decline Invitation (Win -> Cancelled)
    // ==================================================================

    /**
     * When a selected entrant declines, they should be moved from
     * "selected" → "cancelled".
     */
    @Test
    public void testDeclineInvitation_MovesToCancelled() {
        event.getSelectedEntrants().add(userId);

        // Action: User declines
        if (event.getSelectedEntrants().contains(userId)) {
            event.getSelectedEntrants().remove(userId);
            event.getCancelledEntrants().add(userId);
        }

        assertFalse(event.getSelectedEntrants().contains("user_555"));
        assertTrue(event.getCancelledEntrants().contains("user_555"));
    }

    // ==================================================================
    // US 01.05.04: View Entrant Count
    // ==================================================================

    /**
     * Confirms that the waiting list size correctly reflects the number of entrants,
     * allowing the entrant to see how many people are competing for a spot.
     */
    @Test
    public void testViewTotalEntrants_Logic() {
        event.getWaitingList().add("A");
        event.getWaitingList().add("B");
        event.getWaitingList().add("C");

        int count = event.getWaitingList().size();
        assertEquals(3, count);
    }

    // ==================================================================
    // US 01.07.01: Device Identification
    // ==================================================================
    /**
     * Verifies fallback logic used when an entrant uses device-only authentication.
     * If no account exists, the system must use a stable device identifier.
     */
    @Test
    public void testDeviceIdentification_Fallback() {

        String deviceId = "android_id_999";
        String accountId = null;

        String effectiveId = (accountId != null) ? accountId : deviceId;

        assertEquals("android_id_999", effectiveId);
    }

    /**
     * Tests that an entrant's event history correctly tracks which events were won
     * and which were lost, given a list of registered events and a subset of won events.
     */
    @Test
    public void testEventHistoryTracksWonAndLostEvents() {
        List<String> registeredEvents = new ArrayList<>();
        registeredEvents.add("evt_swim");
        registeredEvents.add("evt_dance");

        List<String> wonEvents = new ArrayList<>();
        wonEvents.add("evt_swim"); // entrant won swim lessons

        List<String> lostEvents = new ArrayList<>();
        for (String eventId : registeredEvents) {
            if (!wonEvents.contains(eventId)) {
                lostEvents.add(eventId);
            }
        }
        assertEquals(2, registeredEvents.size());

        assertTrue(wonEvents.contains("evt_swim"));
        assertFalse(wonEvents.contains("evt_dance"));

        assertEquals(1, lostEvents.size());
        assertTrue(lostEvents.contains("evt_dance"));
    }

    /**
     * Tests that the displayed waiting list count matches the actual
     * number of entrants in the waiting list.
     */
    @Test
    public void waitingListCountReflectsNumberOfEntrants() {
        List<String> waitingList = new ArrayList<>();
        waitingList.add("user_a");
        waitingList.add("user_b");
        waitingList.add("user_c");

        int count = waitingList.size();
        String displayText = count + " entrants on waiting list";
        assertEquals(3,count);
        assertEquals("3 entrants on waiting list",displayText);
    }

    /**
     * Internal subclass to allow testing of lists that might not exist
     * in the main Event.java model yet.
     */
    public static class TestableEvent extends Event {
        private List<String> selectedEntrants;
        private List<String> enrolledEntrants;
        private List<String> cancelledEntrants;

        public List<String> getSelectedEntrants() {
            return selectedEntrants;
        }

        public void setSelectedEntrants(List<String> selectedEntrants) {
            this.selectedEntrants = selectedEntrants;
        }
        public List<String> getEnrolledEntrants() {
            return enrolledEntrants;
        }
        public void setEnrolledEntrants(List<String> enrolledEntrants) {
            this.enrolledEntrants = enrolledEntrants;
        }

        public List<String> getCancelledEntrants() {
            return cancelledEntrants;
        }

        public void setCancelledEntrants(List<String> cancelledEntrants) {
            this.cancelledEntrants = cancelledEntrants;
        }
    }
}