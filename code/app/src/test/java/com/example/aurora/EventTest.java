package com.example.aurora;

import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Unit tests for Event.java data model.
 */
public class EventTest {

    private Event event;

    @Before
    public void setUp() {
        event = new Event("e1", "Aurora Night", "Jan 1, 2025", "Edmonton", "poster.png");
    }

    @Test
    public void testConstructorAndDefaults() {
        assertEquals("e1", event.getEventId());
        assertEquals("Aurora Night", event.getTitle());
        assertNotNull(event.getWaitingList());
    }

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
}
