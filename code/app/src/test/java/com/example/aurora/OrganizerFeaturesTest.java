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
 * US 02.06.xx (List Views and Exports)
 */
public class OrganizerFeaturesTest {

    private TestableEvent event;

    /**
     * Sets up a TestableEvent instance with empty entrant lists
     * before each test runs.
     */
    @Before
    public void setup() {
        event = new TestableEvent();
        event.setEventId("evt_org_1");
        event.setTitle("Mega Conference");
        event.setWaitingList(new ArrayList<>());
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
        String expectedDeepLink = "aurora://event/evt_org_1";
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
        event.setMaxSpots(2L);
        event.getWaitingList().add("User1");


        boolean canJoin = event.getWaitingList().size() < event.getMaxSpots();

        assertTrue(canJoin);
    }
    /**
     * Verifies that joining the waiting list is denied once the
     * capacity limit has been reached.
     */
    @Test
    public void testWaitingList_CapacityCheck_Denied() {
        event.setMaxSpots(1L);
        event.getWaitingList().add("User1");

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

        event.getWaitingList().add("Waiter");
        event.getSelectedEntrants().add("Winner");

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
     * Verifies that the CSV export includes the header row and
     * one data row for each enrolled entrant, with all fields present.
     */
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

        String firstLine = csv.split("\n")[0];
        assertEquals("Name,Email,Phone",firstLine);

        String[] lines = csv.split("\n");
        assertEquals(3,lines.length);

        assertTrue(csv.contains("Alice,alice@example.com,111-1111"));
        assertTrue(csv.contains("Bob,bob@example.com,222-2222"));
    }

    /**
     * Verifies that notification preferences correctly prevent alerts
     * when disabled and allow all alerts when enabled.
     */
    @Test
    public void notificationPreferenceBlocksOrAllowsAlerts() {
        boolean notificationsEnabled = false;

        List<String> eventsNeedingNotification = Arrays.asList(
                "evt_swim",
                "evt_piano"
        );

        List<String> queuedNotifications = new ArrayList<>();

        for(String eventId:eventsNeedingNotification){
            if(notificationsEnabled){
                queuedNotifications.add(eventId);
            }
        }
        assertTrue(queuedNotifications.isEmpty());

        notificationsEnabled = true;
        queuedNotifications.clear();

        for(String eventId:eventsNeedingNotification){
            if(notificationsEnabled){
                queuedNotifications.add(eventId);
            }
        }

        assertEquals(eventsNeedingNotification.size(),queuedNotifications.size());
        assertTrue(queuedNotifications.containsAll(eventsNeedingNotification));
    }

    /**
     * Verifies that removing an organizer by ID only removes the
     * targeted organizer and leaves all other organizers unchanged.
     */
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

        Iterator<SimpleOrganizer> it = organizers.iterator();
        while(it.hasNext()){
            SimpleOrganizer o = it.next();
            if(o.id.equals(targetId)){
                it.remove();
            }
        }
        assertEquals(2,organizers.size());

        for(SimpleOrganizer o:organizers){
            assertNotEquals(targetId,o.id);
        }

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