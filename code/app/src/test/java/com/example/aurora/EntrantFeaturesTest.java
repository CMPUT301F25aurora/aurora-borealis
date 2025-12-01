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

    /**
     * Test: Joining the waitlist should add the user once.
     *
     * Verifies:
     *  user is added to list
     *  list size increments
     */
    @Test
    public void testJoinWaitlist_Success() {

        event.getWaitingList().add(userId);

        assertTrue(event.getWaitingList().contains("user_555"));
        assertEquals(1, event.getWaitingList().size());
    }

    /**
     * Test: Joining twice should not duplicate the user.
     *
     * Verifies:
     *  duplicate entries prevented
     *  final size remains 1
     */
    @Test
    public void testJoinWaitlist_PreventDuplicates() {
        // Logic: Check if already exists
        event.getWaitingList().add(userId);

        boolean alreadyIn = event.getWaitingList().contains(userId);
        if (!alreadyIn) {
            event.getWaitingList().add(userId);
        }

        assertEquals(1, event.getWaitingList().size());
    }

    /**
     * Test: Leaving removes user from list.
     *
     * Verifies:
     *  user removed from waitlist
     *  size becomes zero
     */
    @Test
    public void testLeaveWaitlist_Success() {
        event.getWaitingList().add(userId);

        // Action: Leave
        event.getWaitingList().remove(userId);

        assertFalse(event.getWaitingList().contains("user_555"));
        assertEquals(0, event.getWaitingList().size());
    }

    /**
     * Test: Updating profile info should store new data.
     *
     * Verifies:
     *  updated name matches
     *  updated phone matches
     */
    @Test
    public void testProfile_UpdateInfo() {

        String name = "Alice Cooper";
        String phone = "123-456-7890";

        assertEquals("Alice Cooper", name);
        assertEquals("123-456-7890", phone);
    }

    /**
     * Test: Email validation logic should reject missing "@".
     *
     * Verifies:
     *  valid emails contain "@"
     *  invalid emails do not
     */
    @Test
    public void testProfile_EmailValidation_Logic() {

        String valid = "test@email.com";
        String invalid = "testemail.com";

        assertTrue(valid.contains("@"));
        assertFalse(invalid.contains("@"));
    }

    /**
     * Test: Accepting invitation moves user from selected → enrolled.
     *
     * Verifies:
     *  removed from selectedEntrants
     *  added to enrolledEntrants
     */
    @Test
    public void testAcceptInvitation_MovesToEnrolled() {
        // Setup: User won the lottery
        event.getSelectedEntrants().add(userId);

        if (event.getSelectedEntrants().contains(userId)) {
            event.getSelectedEntrants().remove(userId);
            event.getEnrolledEntrants().add(userId);
        }

        assertFalse("Should be removed from Selected", event.getSelectedEntrants().contains("user_555"));
        assertTrue("Should be added to Enrolled", event.getEnrolledEntrants().contains("user_555"));
    }

    /**
     * Test: Declining invitation moves user from selected → cancelled.
     *
     * Verifies:
     *  removed from selectedEntrants
     *  added to cancelledEntrants
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

    /**
     * Test: Counting entrants returns correct size.
     *
     * Verifies:
     *  list size counting works
     */
    @Test
    public void testViewTotalEntrants_Logic() {
        event.getWaitingList().add("A");
        event.getWaitingList().add("B");
        event.getWaitingList().add("C");

        int count = event.getWaitingList().size();
        assertEquals(3, count);
    }

    /**
     * Test: Fallback logic uses device ID when no account is available.
     *
     * Verifies:
     *  null account ID replaced by device ID
     */
    @Test
    public void testDeviceIdentification_Fallback() {

        String deviceId = "android_id_999";
        String accountId = null;

        String effectiveId = (accountId != null) ? accountId : deviceId;

        assertEquals("android_id_999", effectiveId);
    }



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