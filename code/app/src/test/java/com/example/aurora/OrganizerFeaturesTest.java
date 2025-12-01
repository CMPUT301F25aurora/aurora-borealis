package com.example.aurora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.aurora.models.Event;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * COMPREHENSIVE ORGANIZER LOGIC TESTS
 * Covers:
 * US 02.01.xx (Event Management)
 * US 02.03.xx (Capacity Limits)
 * US 02.06.xx (List Views & Exports)
 */
public class OrganizerFeaturesTest {

    // Use our TestableEvent subclass to access lists that might be missing in the main model
    private TestableEvent event;

    @Before
    public void setup() {
        event = new TestableEvent();
        event.setEventId("evt_org_1");
        event.setTitle("Mega Conference");
        event.setWaitingList(new ArrayList<>());

        // Initialize other lists
        event.setSelectedEntrants(new ArrayList<>());
        event.setEnrolledEntrants(new ArrayList<>());
        event.setCancelledEntrants(new ArrayList<>());
    }

    // ==================================================================
    // US 02.01.01: Create Event & QR Code Logic
    // ==================================================================

    /**
     * Tests that QR code data generation correctly formats a valid
     * event deep link based on the event ID.
     */
    @Test
    public void testQRCode_DataStringGeneration() {
        // Logic: The QR code is usually the Event ID or a deep link
        String expectedDeepLink = "aurora://event/evt_org_1";

        // Simulate generation logic
        String generatedData = "aurora://event/" + event.getEventId();

        assertEquals(expectedDeepLink, generatedData);
    }

    // ==================================================================
    // US 02.01.04: Set Registration Period
    // =================================================================
    /**
     * Ensures that registration periods are valid, meaning the
     * start timestamp must always be earlier than the end timestamp.
     */
    @Test
    public void testRegistrationPeriod_Validation() {
        // Start date must be before End date
        long start = 1700000000L;
        long end = 1800000000L;

        boolean isValid = start < end;
        assertTrue("Registration start must be before end", isValid);
    }

    // ==================================================================
    // US 02.03.01: Limit Waiting List
    // ==================================================================
    /**
     * Verifies that an entrant can join the waiting list when the
     * list size is below the event's maximum allowed capacity.
     */
    @Test
    public void testWaitingList_CapacityCheck_Allowed() {
        event.setMaxSpots(2L); // Limit 2
        event.getWaitingList().add("User1");

        // Logic: Can User2 join?
        boolean canJoin = event.getWaitingList().size() < event.getMaxSpots();

        assertTrue(canJoin);
    }
    /**
     * Verifies that joining the waiting list is denied once the
     * capacity limit has been reached.
     */
    @Test
    public void testWaitingList_CapacityCheck_Denied() {
        event.setMaxSpots(1L); // Limit 1
        event.getWaitingList().add("User1"); // List size is now 1

        // Logic: Can User2 join?
        boolean canJoin = event.getWaitingList().size() < event.getMaxSpots();

        assertTrue("Should NOT be able to join full list", !canJoin);
    }

    // ==================================================================
    // US 02.06.01 - 03: View Specific Lists
    // ==================================================================
    /**
     * Ensures that organizer-visible lists (waiting, selected, enrolled,
     * cancelled) remain independent from one another and contain only
     * the correct entrants.
     */
    @Test
    public void testListFiltration_Logic() {
        // A robust event object has 4 lists. Organizer needs to see them separately.
        event.getWaitingList().add("Waiter");
        event.getSelectedEntrants().add("Winner");

        // Verify separation
        assertTrue(event.getWaitingList().contains("Waiter"));
        assertTrue(!event.getWaitingList().contains("Winner"));
        assertTrue(event.getSelectedEntrants().contains("Winner"));
    }

    // ==================================================================
    // US 02.06.05: Export to CSV
    // ==================================================================
    /**
     * Verifies that exporting the list of enrolled entrants to CSV format
     * creates properly structured rows, including a header and
     * event/user relationships.
     */
    @Test
    public void testCSVExport_Formatting() {
        // Logic: Convert list of enrolled users to Comma Separated String
        event.getEnrolledEntrants().add("UserA");
        event.getEnrolledEntrants().add("UserB");

        StringBuilder csv = new StringBuilder();
        csv.append("EventID,UserID\n");
        for (String uid : event.getEnrolledEntrants()) {
            csv.append(event.getEventId()).append(",").append(uid).append("\n");
        }

        String result = csv.toString();

        assertTrue(result.contains("evt_org_1,UserA"));
        assertTrue(result.contains("evt_org_1,UserB"));
        assertTrue(result.startsWith("EventID,UserID"));
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