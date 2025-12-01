package com.example.aurora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.aurora.models.AdminEventItem;
import com.example.aurora.models.AdminImage;

import org.junit.Test;
/**
 * Unit tests for simple Admin model classes:
 *
 * Verifies:
 *  AdminEventItem getters/setters work correctly
 *  AdminImage constructor assigns fields correctly
 *  AdminImage empty constructor leaves fields null
 */
public class AdminModelTest {

    /**
     * Test: AdminEventItem stores and returns values properly.
     *
     * Verifies:
     *  ID setter/getter works
     *  Title setter/getter works
     *  WaitingCount setter/getter works
     *  MaxSpots setter/getter works
     */
    @Test
    public void testAdminEventItem_GettersSetters() {
        AdminEventItem item = new AdminEventItem();
        item.setId("123");
        item.setTitle("Test Event");
        item.setWaitingCount(50);
        item.setMaxSpots(10);

        assertEquals("123", item.getId());
        assertEquals("Test Event", item.getTitle());
        assertEquals(50, item.getWaitingCount());
        assertEquals(10, item.getMaxSpots());
    }

    /**
     * Test: AdminImage constructor initializes all public fields.
     *
     * Verifies:
     *   eventId is set
     *   eventTitle is set
     *   organizerEmail is set
     *   posterUrl is set
     */
    @Test
    public void testAdminImage_Constructor() {
        AdminImage img = new AdminImage("evt1", "Party", "org@test.com", "http://url.com");

        assertEquals("evt1", img.eventId);
        assertEquals("Party", img.eventTitle);
        assertEquals("org@test.com", img.organizerEmail);
        assertEquals("http://url.com", img.posterUrl);
    }

    /**
     * Test: AdminImage empty constructor leaves all fields null.
     *
     * Verifies:
     *  eventId defaults to null
     *  object construction does not crash
     */
    @Test
    public void testAdminImage_EmptyConstructor() {
        AdminImage img = new AdminImage();
        assertNull(img.eventId);
        // Ensure it doesn't crash on null access if fields are public
    }
}