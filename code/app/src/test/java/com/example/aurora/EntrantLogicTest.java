package com.example.aurora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.aurora.models.Event;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pure-Java logic tests for entrant-related behaviour.
 *
 * These tests deliberately avoid Mockito, Android, or Firestore types so that
 * they run as regular local unit tests with no extra dependencies.
 */
public class EntrantLogicTest {

    /**
     * When an entrant joins the waiting list, their ID should be added
     * if it is not already present.
     */
    @Test
    public void joinWaitingList_addsUserWhenNotPresent() {
        Event event = new Event();
        List<String> waiting = new ArrayList<>();
        event.setWaitingList(waiting);

        String userId = "user123";

        if (!event.getWaitingList().contains(userId)) {
            event.getWaitingList().add(userId);
        }

        assertTrue(event.getWaitingList().contains(userId));
        assertEquals(1, event.getWaitingList().size());
    }

    /**
     * Joining twice should not create duplicates in the waiting list.
     */
    @Test
    public void joinWaitingList_doesNotDuplicateUser() {
        Event event = new Event();
        List<String> waiting = new ArrayList<>(Arrays.asList("user123"));
        event.setWaitingList(waiting);

        String userId = "user123";

        if (!event.getWaitingList().contains(userId)) {
            event.getWaitingList().add(userId);
        }

        assertEquals(1, event.getWaitingList().size());
        assertTrue(event.getWaitingList().contains(userId));
    }

    /**
     * Leaving the waiting list should remove the user if present.
     */
    @Test
    public void leaveWaitingList_removesUser() {
        Event event = new Event();
        List<String> waiting = new ArrayList<>(Arrays.asList("user123", "user999"));
        event.setWaitingList(waiting);

        String userId = "user123";
        event.getWaitingList().remove(userId);

        assertFalse(event.getWaitingList().contains(userId));
        assertEquals(1, event.getWaitingList().size());
    }

    /**
     * If geoRequired is true on an event, we can use that as a simple
     * flag to block joining when the entrant has no location.
     */
    @Test
    public void geoRequiredFlag_blocksJoinWhenNoLocation() {
        Event event = new Event();
        event.setGeoRequired(true);

        boolean entrantHasLocation = false;
        boolean allowedToJoin = !event.getGeoRequired() || entrantHasLocation;

        assertTrue(event.getGeoRequired());
        assertFalse("Entrant without location should not be allowed when geoRequired is true",
                allowedToJoin);
    }
}
