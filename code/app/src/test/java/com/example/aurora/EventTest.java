/*
 * References for EventTest:
 *
 * source: JUnit 4 tutorial — "Unit Testing with JUnit 4"
 * url: https://www.vogella.com/tutorials/JUnit4/article.html
 * note: Used for the pattern of a JUnit 4 test class with fields,
 *       a @Before setup method, and @Test methods using assertEquals.
 *
 *
 * source: JUnit 4 Javadoc — "Assert"
 * url: https://junit.org/junit4/javadoc/4.8/org/junit/Assert.html
 * note: Used as a reference for assertEquals and the static import pattern
 *       import static org.junit.Assert.*; in JUnit tests.
 *
 * source: Java SE docs — "Arrays"
 * url: https://docs.oracle.com/javase/8/docs/api/java/util/Arrays.html
 * note: Used for Arrays.asList(...) which returns a fixed-size List backed by
 *       the array, similar to how the waitingList test data is created.
 *
 */

package com.example.aurora;

import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

import com.example.aurora.models.Event;

/**
 * Unit tests for Event.java model.
 *
 * Verifies:
 *  correct default values
 *  all getters and setters work properly
 *  waitingList returns a non-null list
 */
public class EventTest {

    private Event event;

    /**
     * Sets up a default Event instance used by the tests.
     * Runs before each test method in this class.
     */
    @Before
    public void setUp() {
        event = new Event();
        event.setEventId("e1");
        event.setTitle("Aurora Night");
        event.setDate("Jan 1, 2025");
        event.setLocation("Edmonton");
        event.setPosterUrl("poster.png");
    }

    /**
     * Test: Event defaults should be initialized properly.
     *
     * Verifies:
     *  eventId is stored correctly
     *  title matches
     *  waitingList is non-null
     *  waitingList starts empty
     */
    @Test
    public void testConstructorAndDefaults() {
        assertEquals("e1", event.getEventId());
        assertEquals("Aurora Night", event.getTitle());
        assertNotNull(event.getWaitingList());
        assertTrue(event.getWaitingList().isEmpty());
    }

    /**
     * Test: All setters should update fields correctly.
     *
     * Verifies:
     *  description, location, start/end dates, deepLink set properly
     *  waitingList accepts provided list
     */
    @Test
    public void testGettersAndSetters() {
        event.setDescription("A cool event");
        event.setLocation("Calgary");
        event.setStartDate("2025-01-01");
        event.setEndDate("2025-01-02");
        event.setDeepLink("aurora://event/e1");

        List<String> list = Arrays.asList("u1", "u2");
        event.setWaitingList(list);

        assertEquals("A cool event", event.getDescription());
        assertEquals("Calgary", event.getLocation());
        assertEquals("2025-01-01", event.getStartDate());
        assertEquals("2025-01-02", event.getEndDate());
        assertEquals("aurora://event/e1", event.getDeepLink());
        assertEquals(2, event.getWaitingList().size());
    }

    /**
     * Tests that the event correctly stores waiting list values.
     */
    @Test
    public void testSetWaitingListValues() {
        List<String> ids = Arrays.asList("u1", "u2", "u3");
        event.setWaitingList(ids);

        assertEquals(3, event.getWaitingList().size());
        assertEquals("u1", event.getWaitingList().get(0));
    }

    /**
     * Tests that categories  work
     */
    @Test
    public void testCategoryField() {
        event.setCategory("Sports");
        assertEquals("Sports", event.getCategory());
    }



    /**
     * Tests registration start/end date fields.
     */
    @Test
    public void testRegistrationPeriod() {
        event.setRegistrationStart("2025-02-01");
        event.setRegistrationEnd("2025-02-10");

        assertEquals("2025-02-01", event.getRegistrationStart());
        assertEquals("2025-02-10", event.getRegistrationEnd());
    }
    /**
     * Verifies posterUrl is stored
     */
    @Test
    public void testPosterUrl() {
        event.setPosterUrl("https://example.com/poster.png");
        assertEquals("https://example.com/poster.png", event.getPosterUrl());
    }

    /**
     * Ensures geoRequired handles if true or false and returns false by default
     */
    @Test
    public void testGeoRequired() {
        Event e = new Event();
        assertFalse(e.getGeoRequired());  // default is false

        e.setGeoRequired(true);
        assertTrue(e.getGeoRequired());

        e.setGeoRequired(false);
        assertFalse(e.getGeoRequired());
    }
    /**
     * tests selected entrants list is working
     */
    @Test
    public void testSelectedEntrantsList() {
        assertNotNull(event.getSelectedEntrants());
        event.setSelectedEntrants(Arrays.asList("u1", "u2"));
        assertEquals(2, event.getSelectedEntrants().size());
    }

    /**
     * Test final entrants list
     */
    @Test
    public void testFinalEntrantsList() {
        assertNotNull(event.getFinalEntrants());
        event.setFinalEntrants(Arrays.asList("a", "b", "c"));
        assertEquals(3, event.getFinalEntrants().size());
    }

    /**
     * Tests that the maxSpots capacity is stored correctly.
     */
    @Test
    public void testMaxSpotsField() {
        event.setMaxSpots(50L);
        assertEquals(Long.valueOf(50), event.getMaxSpots());
    }

}
