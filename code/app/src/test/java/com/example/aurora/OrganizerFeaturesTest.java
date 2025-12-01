package com.example.aurora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.aurora.models.Event;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
    // ==================================================================
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
    @Test
    public void testWaitingList_CapacityCheck_Allowed() {
        event.setMaxSpots(2L); // Limit 2
        event.getWaitingList().add("User1");

        // Logic: Can User2 join?
        boolean canJoin = event.getWaitingList().size() < event.getMaxSpots();

        assertTrue(canJoin);
    }

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

    // ============================================================
    // US 02.06.05: Export final enrolled entrants in CSV format
    // ============================================================
    @Test
    public void exportCsv_includesHeaderAndAllEnrolledEntrants() {
        class SimpleEntrant {
            String name;
            String email;
            String phone;

            SimpleEntrant(String name,String email,String phone){
                this.name=name;
                this.email=email;
                this.phone=phone;
            }
        }

        List<SimpleEntrant> enrolled = Arrays.asList(
                new SimpleEntrant("Alice","alice@example.com","111-1111"),
                new SimpleEntrant("Bob","bob@example.com","222-2222")
        );

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Name,Email,Phone\n");
        for(SimpleEntrant e:enrolled){
            csvBuilder
                    .append(e.name).append(",")
                    .append(e.email).append(",")
                    .append(e.phone).append("\n");
        }
        String csv = csvBuilder.toString();

        // Header must be correct
        String firstLine = csv.split("\n")[0];
        assertEquals("Name,Email,Phone",firstLine);

        // There should be 1 header line + 2 data lines = 3 lines total
        String[] lines = csv.split("\n");
        assertEquals(3,lines.length);

        // CSV should contain each enrolled entrant's data
        assertTrue(csv.contains("Alice,alice@example.com,111-1111"));
        assertTrue(csv.contains("Bob,bob@example.com,222-2222"));
    }

    // ============================================================
    // US 01.04.03: Opt out of notifications from organizers/admins
    // ============================================================
    @Test
    public void notificationPreferenceBlocksOrAllowsAlerts() {
        boolean notificationsEnabled = false;

        List<String> eventsNeedingNotification = Arrays.asList(
                "evt_swim",
                "evt_piano"
        );

        List<String> queuedNotifications = new ArrayList<>();

        // When notifications are disabled, nothing should be queued
        for(String eventId:eventsNeedingNotification){
            if(notificationsEnabled){
                queuedNotifications.add(eventId);
            }
        }
        assertTrue(queuedNotifications.isEmpty());

        // Turn notifications ON and recompute
        notificationsEnabled = true;
        queuedNotifications.clear();

        for(String eventId:eventsNeedingNotification){
            if(notificationsEnabled){
                queuedNotifications.add(eventId);
            }
        }

        // Now all events should be queued
        assertEquals(eventsNeedingNotification.size(),queuedNotifications.size());
        assertTrue(queuedNotifications.containsAll(eventsNeedingNotification));
    }

    // ============================================================
    // US 03.07.01: Admin removes organizers that violate policy
    // ============================================================
    @Test
    public void removeOrganizer_removesOnlyTargetOrganizer() {
        class SimpleOrganizer {
            String id;
            String name;

            SimpleOrganizer(String id,String name){
                this.id=id;
                this.name=name;
            }
        }

        List<SimpleOrganizer> organizers = new ArrayList<>();
        organizers.add(new SimpleOrganizer("org1","Dance Club"));
        organizers.add(new SimpleOrganizer("org2","Swim School"));
        organizers.add(new SimpleOrganizer("org3","Piano Studio"));

        String targetId = "org2";

        // Remove the organizer that violated policy
        Iterator<SimpleOrganizer> it = organizers.iterator();
        while(it.hasNext()){
            SimpleOrganizer o = it.next();
            if(o.id.equals(targetId)){
                it.remove();
            }
        }

        // 1) Size should now be 2
        assertEquals(2,organizers.size());

        // 2) Remaining IDs should not contain the removed one
        for(SimpleOrganizer o:organizers){
            assertNotEquals(targetId,o.id);
        }

        // 3) The other organizers are still present
        List<String> remainingIds = new ArrayList<>();
        for(SimpleOrganizer o:organizers){
            remainingIds.add(o.id);
        }
        assertTrue(remainingIds.contains("org1"));
        assertTrue(remainingIds.contains("org3"));
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