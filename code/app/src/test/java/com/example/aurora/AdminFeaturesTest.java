package com.example.aurora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.aurora.models.AdminEventItem;
import com.example.aurora.models.AdminImage;
import com.example.aurora.utils.AdminUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * COMPREHENSIVE ADMIN UNIT TESTS
 *
 * This class tests the "Business Logic" behind the Admin User Stories.
 * Since we cannot click buttons in a Unit Test, we test the data logic
 * that those buttons rely on.
 */
@RunWith(MockitoJUnitRunner.class)
public class AdminFeaturesTest {

    @Mock
    DocumentSnapshot mockEventDoc;
    @Mock
    DocumentSnapshot mockProfileDoc;

    @Before
    public void setup() {
        // Prepare mock data
    }

    // ==================================================================
    // US 03.04.01: As an administrator, I want to be able to browse events.
    // US 03.01.01: As an administrator, I want to be able to remove events.
    // ==================================================================
    @Test
    public void testEventDataParsing_ForBrowsing() {
        // 1. Simulate a Firestore Event Document
        when(mockEventDoc.getString("title")).thenReturn("Gala Night");
        // REMOVED UNUSED STUBS (ID and Date) to fix UnnecessaryStubbingException

        // Simulate waiting list logic (Counting entrants)
        List<String> waitingList = Arrays.asList("user1", "user2", "user3");
        when(mockEventDoc.get("waitingList")).thenReturn(waitingList);

        // 2. Extract Data (Simulating AdminActivity logic)
        String title = mockEventDoc.getString("title");
        int count = ((List) mockEventDoc.get("waitingList")).size();

        // 3. Verify logic
        assertEquals("Gala Night", title);
        assertEquals(3, count); // logic must correctly count entrants
    }

    @Test
    public void testEventRemoval_DataIntegrity() {
        // When we remove an event, we need the exact ID.
        // Test that our AdminEventItem model holds the ID safely.
        AdminEventItem item = new AdminEventItem();
        item.setId("delete_me_123");
        item.setTitle("Spam Event");

        // Verify the ID is retrievable for the delete function
        assertEquals("delete_me_123", item.getId());
        assertEquals("Spam Event", item.getTitle());
    }

    // ==================================================================
    // US 03.05.01: As an administrator, I want to be able to browse profiles.
    // US 03.02.01: As an administrator, I want to be able to remove profiles.
    // ==================================================================
    @Test
    public void testProfileLogic_OrganizerDetection() {
        // Logic: Admin needs to know if a user is an organizer or entrant
        // to show the correct "Remove Privileges" button.

        // REMOVED UNUSED STUB (Name) to fix UnnecessaryStubbingException
        when(mockProfileDoc.getString("role")).thenReturn("entrant");
        when(mockProfileDoc.getBoolean("organizer_allowed")).thenReturn(true);

        // Simulate logic:
        boolean isOrganizer = Boolean.TRUE.equals(mockProfileDoc.getBoolean("organizer_allowed"));
        String displayRole = AdminUtils.capitalize(mockProfileDoc.getString("role"));

        assertTrue("User should be identified as organizer", isOrganizer);
        assertEquals("Entrant", displayRole); // Capitalization logic check
    }

    @Test
    public void testProfileRemoval_SafeHandling() {
        // Verify null handling logic used in Profile List
        String nullName = null;
        String result = AdminUtils.nz(nullName); // nz = Null Zero (Safe String)

        // If profile name is corrupted/null, app shouldn't crash
        assertEquals("", result);
    }

    // ==================================================================
    // US 03.06.01: Browse images.
    // US 03.03.01: Remove images.
    // ==================================================================
    @Test
    public void testImageModel_HoldsCorrectDeleteInfo() {
        // To remove an image, we need the posterURL and the eventID.
        String expectedUrl = "https://firebase.storage/image.png";
        String expectedEventId = "evt_555";

        AdminImage img = new AdminImage(expectedEventId, "Party", "org@mail.com", expectedUrl);

        // Verify data is ready for the "Delete" button
        assertEquals(expectedUrl, img.posterUrl);
        assertEquals(expectedEventId, img.eventId);
    }

    // ==================================================================
    // US 03.07.01: Remove organizers that violate app policy.
    // ==================================================================
    @Test
    public void testOrganizerViolation_Logic() {
        // Simulating the check: Is this user actually an organizer?
        when(mockProfileDoc.getBoolean("organizer_allowed")).thenReturn(false);

        boolean canRevoke = Boolean.TRUE.equals(mockProfileDoc.getBoolean("organizer_allowed"));

        // If they are NOT an organizer, the "Remove Privileges" button logic should be false
        assertEquals(false, canRevoke);
    }

    // ==================================================================
    // US 03.08.01: Review logs of notifications.
    // ==================================================================
    @Test
    public void testLogTimeFormatting_RelativeTime() {
        // Admin needs to see "5 min ago", not "16899933221".
        // This tests the core logic of the Log Viewer.

        long now = 1000000000L; // Fixed time
        long fiveMinAgo = now - (5 * 60 * 1000);

        Date logDate = new Date(fiveMinAgo);

        String display = AdminUtils.formatRelativeTime(logDate, now);
        assertEquals("5 min ago", display);
    }

    @Test
    public void testLogMessage_NullSafety() {
        // Logs often have missing fields. Logic must not crash.
        String nullMsg = null;
        String safeMsg = AdminUtils.nz(nullMsg);

        assertNotNull(safeMsg);
        assertEquals("", safeMsg);
    }
}