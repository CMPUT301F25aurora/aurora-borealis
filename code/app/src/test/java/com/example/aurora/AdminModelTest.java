package com.example.aurora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.aurora.models.AdminEventItem;
import com.example.aurora.models.AdminImage;

import org.junit.Test;

public class AdminModelTest {

    /**
     * Verifies that {@link AdminEventItem} correctly stores and returns values
     * through its getter and setter methods.

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
     * Ensures the full constructor of {@link AdminImage} assigns all fields
     * correctly. This is used by the Admin interface when viewing or deleting
     * uploaded event posters.
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
     * Ensures the empty constructor of {@link AdminImage} initializes an object
     * without throwing errors, and fields default to null. The Admin UI may
     * instantiate these before populating fields from Firestore.
     */
    @Test
    public void testAdminImage_EmptyConstructor() {
        AdminImage img = new AdminImage();
        assertNull(img.eventId);
        // Ensure it doesn't crash on null access if fields are public
    }
}