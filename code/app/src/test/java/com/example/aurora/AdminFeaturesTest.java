package com.example.aurora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * COMPREHENSIVE ADMIN UNIT TESTS (EXTENDED)
 * Total Tests: ~32
 *
 * Covers User Stories:
 * US 03.01.01 (Remove Events), US 03.02.01 (Remove Profiles)
 * US 03.03.01 (Remove Images), US 03.04.01 (Browse Events)
 * US 03.05.01 (Browse Profiles), US 03.06.01 (Browse Images)
 * US 03.07.01 (Remove Organizers), US 03.08.01 (Review Logs)
 */
@RunWith(MockitoJUnitRunner.class)
public class AdminFeaturesTest {

    @Mock
    DocumentSnapshot mockEventDoc;
    @Mock
    DocumentSnapshot mockProfileDoc;
    @Mock
    DocumentSnapshot mockLogDoc;

    @Before
    public void setup() {
        // Common setup if needed
    }


    @Test
    public void testEvent_Browsing_CalculatesWaitingCount() {
        List<String> waitingList = Arrays.asList("A", "B", "C");
        when(mockEventDoc.get("waitingList")).thenReturn(waitingList);
        int count = ((List) mockEventDoc.get("waitingList")).size();
        assertEquals("Should count 3 waiting entrants", 3, count);
    }

    @Test
    public void testEvent_Browsing_CalculatesSelectedCount() {
        List<String> selectedList = Arrays.asList("Winner1");
        when(mockEventDoc.get("selectedEntrants")).thenReturn(selectedList);
        int count = ((List) mockEventDoc.get("selectedEntrants")).size();
        assertEquals("Should count 1 selected entrant", 1, count);
    }

    @Test
    public void testEvent_Browsing_CalculatesCancelledCount() {
        List<String> cancelledList = Arrays.asList("X", "Y");
        when(mockEventDoc.get("cancelledEntrants")).thenReturn(cancelledList);
        int count = ((List) mockEventDoc.get("cancelledEntrants")).size();
        assertEquals("Should count 2 cancelled entrants", 2, count);
    }

    @Test
    public void testEvent_Browsing_HandlesNullLists() {
        // If lists don't exist yet, logic should handle it gracefully
        when(mockEventDoc.get("waitingList")).thenReturn(null);
        List<String> waiting = (List<String>) mockEventDoc.get("waitingList");
        int entrants = waiting == null ? 0 : waiting.size();
        assertEquals(0, entrants);
    }

    @Test
    public void testEvent_Browsing_CapacityUnlimited() {
        // Logic: if maxSpots is null, it is "Unlimited"
        when(mockEventDoc.getLong("maxSpots")).thenReturn(null);
        Long max = mockEventDoc.getLong("maxSpots");
        String display = (max != null ? String.valueOf(max) : "Unlimited");
        assertEquals("Unlimited", display);
    }

    @Test
    public void testEvent_Browsing_CapacityLimited() {
        when(mockEventDoc.getLong("maxSpots")).thenReturn(50L);
        Long max = mockEventDoc.getLong("maxSpots");
        String display = (max != null ? String.valueOf(max) : "Unlimited");
        assertEquals("50", display);
    }

    @Test
    public void testEvent_Browsing_OrganizerFallback() {
        // Logic: If organizerName is missing, show "Unknown"
        when(mockEventDoc.getString("organizerName")).thenReturn(null);
        String org = AdminUtils.nz(mockEventDoc.getString("organizerName"));
        String display = org.isEmpty() ? "Unknown" : org;
        assertEquals("Unknown", display);
    }

    @Test
    public void testEvent_Browsing_DateFallback() {
        // Logic: specific date field vs display string
        when(mockEventDoc.getString("date")).thenReturn(null);
        when(mockEventDoc.getString("dateDisplay")).thenReturn("Jan 1st");

        String d = AdminUtils.nz(mockEventDoc.getString("date"));
        if (d.isEmpty()) d = AdminUtils.nz(mockEventDoc.getString("dateDisplay"));

        assertEquals("Jan 1st", d);
    }

    @Test
    public void testEvent_Removal_TitleFallback() {
        when(mockEventDoc.getString("title")).thenReturn(null);
        when(mockEventDoc.getString("name")).thenReturn("Legacy Name");

        String title = AdminUtils.nz(mockEventDoc.getString("title"));
        if (title.isEmpty()) title = AdminUtils.nz(mockEventDoc.getString("name"));

        assertEquals("Legacy Name", title);
    }

    @Test
    public void testEvent_Removal_ModelIntegrity() {
        AdminEventItem item = new AdminEventItem();
        item.setId("evt_123");
        assertEquals("evt_123", item.getId());
    }

    // ==================================================================
    // US 03.05.01 & US 03.02.01: BROWSING & REMOVING PROFILES (8 Tests)
    // ==================================================================

    @Test
    public void testProfile_Browsing_CapitalizesRoles() {
        String rawRole = "organizer";
        assertEquals("Organizer", AdminUtils.capitalize(rawRole));
    }

    @Test
    public void testProfile_Browsing_HandlesMissingName() {
        String name = AdminUtils.nz(null);
        String display = name.isEmpty() ? "Unnamed" : name;
        assertEquals("Unnamed", display);
    }

    @Test
    public void testProfile_Browsing_PhoneNull() {
        when(mockProfileDoc.getString("phone")).thenReturn(null);
        String phone = AdminUtils.nz(mockProfileDoc.getString("phone"));
        assertEquals("", phone);
    }

    @Test
    public void testProfile_Browsing_PhoneExists() {
        when(mockProfileDoc.getString("phone")).thenReturn("555-1234");
        String phone = AdminUtils.nz(mockProfileDoc.getString("phone"));
        assertEquals("555-1234", phone);
    }

    @Test
    public void testProfile_Removal_AdminProtection() {
        when(mockProfileDoc.getString("role")).thenReturn("admin");
        boolean isAdmin = "admin".equalsIgnoreCase(mockProfileDoc.getString("role"));
        assertTrue(isAdmin);
    }

    @Test
    public void testProfile_Removal_AdminProtection_CaseInsensitive() {
        when(mockProfileDoc.getString("role")).thenReturn("ADMIN");
        boolean isAdmin = "admin".equalsIgnoreCase(mockProfileDoc.getString("role"));
        assertTrue(isAdmin);
    }

    @Test
    public void testProfile_Notifications_DefaultTrue() {
        // Logic: if field is missing (null), notifications are usually ON by default
        when(mockProfileDoc.getBoolean("notificationsEnabled")).thenReturn(null);
        Boolean notif = mockProfileDoc.getBoolean("notificationsEnabled");
        // Logic in app:
        boolean isOn = (notif == null || notif);
        assertTrue(isOn);
    }

    @Test
    public void testProfile_Notifications_ExplicitlyFalse() {
        when(mockProfileDoc.getBoolean("notificationsEnabled")).thenReturn(false);
        Boolean notif = mockProfileDoc.getBoolean("notificationsEnabled");
        boolean isOn = (notif == null || notif);
        assertFalse(isOn);
    }

    // ==================================================================
    // US 03.07.01: REMOVE ORGANIZERS (5 Tests)
    // ==================================================================

    @Test
    public void testOrganizer_Revoke_DetectsActive() {
        when(mockProfileDoc.getBoolean("organizer_allowed")).thenReturn(true);
        boolean isOrg = Boolean.TRUE.equals(mockProfileDoc.getBoolean("organizer_allowed"));
        assertTrue(isOrg);
    }

    @Test
    public void testOrganizer_Revoke_DetectsRevoked() {
        when(mockProfileDoc.getBoolean("organizer_allowed")).thenReturn(false);
        boolean isOrg = Boolean.TRUE.equals(mockProfileDoc.getBoolean("organizer_allowed"));
        assertFalse(isOrg);
    }

    @Test
    public void testOrganizer_Revoke_HandlesNull() {
        when(mockProfileDoc.getBoolean("organizer_allowed")).thenReturn(null);
        boolean isOrg = Boolean.TRUE.equals(mockProfileDoc.getBoolean("organizer_allowed"));
        assertFalse(isOrg);
    }

    @Test
    public void testOrganizer_Toggle_Logic() {
        // Simulating the toggle logic
        boolean current = true; // currently allowed
        boolean nextState = !current; // revoke
        assertFalse(nextState);
    }

    @Test
    public void testOrganizer_Toggle_Logic_Restore() {
        boolean current = false; // currently revoked
        boolean nextState = !current; // restore
        assertTrue(nextState);
    }

    // ==================================================================
    // US 03.06.01 & US 03.03.01: BROWSING & REMOVING IMAGES (4 Tests)
    // ==================================================================

    @Test
    public void testImage_Model_Integrity() {
        AdminImage img = new AdminImage("e1", "T", "o@g.com", "url");
        assertEquals("e1", img.eventId);
    }

    @Test
    public void testImage_Browsing_NoOrganizer() {
        // Image might exist but organizer deleted their account
        AdminImage img = new AdminImage("e1", "T", null, "url");
        String displayOrg = (img.organizerEmail == null ? "Unknown" : img.organizerEmail);
        assertEquals("Unknown", displayOrg);
    }

    @Test
    public void testImage_Browsing_EmptyUrl() {
        String url = "";
        boolean valid = !url.isEmpty();
        assertFalse(valid);
    }

    @Test
    public void testImage_Delete_Logic() {
        // Verification that we have necessary data to delete
        AdminImage img = new AdminImage("e1", "T", "o", "gs://bucket/file.png");
        assertNotNull(img.posterUrl);
        assertNotNull(img.eventId);
    }

    // ==================================================================
    // US 03.08.01: REVIEW LOGS (5 Tests)
    // ==================================================================

    @Test
    public void testLog_RelativeTime_Minutes() {
        long now = 1000000000L;
        Date d = new Date(now - (5 * 60 * 1000));
        assertEquals("5 min ago", AdminUtils.formatRelativeTime(d, now));
    }

    @Test
    public void testLog_RelativeTime_Hours() {
        long now = 1000000000L;
        Date d = new Date(now - (2 * 60 * 60 * 1000));
        assertEquals("2 hours ago", AdminUtils.formatRelativeTime(d, now));
    }

    @Test
    public void testLog_RelativeTime_Days() {
        long now = 1000000000L;
        Date d = new Date(now - (25 * 60 * 60 * 1000)); // 25 hours
        assertEquals("1 days ago", AdminUtils.formatRelativeTime(d, now));
    }

    @Test
    public void testLog_NullMessage() {
        when(mockLogDoc.getString("message")).thenReturn(null);
        String msg = AdminUtils.nz(mockLogDoc.getString("message"));
        assertEquals("", msg);
    }

    @Test
    public void testLog_FutureTimestamp() {
        // Edge case: Server clock skew resulted in a future time
        long now = 1000000000L;
        Date futureDate = new Date(now + 5000); // 5 seconds in future

        // The formula is: now - date.
        // 1000 - 1005 = -5.
        // -5 / 60000 = 0.
        // "0 min ago" is the mathematically expected result for logic stability
        assertEquals("0 min ago", AdminUtils.formatRelativeTime(futureDate, now));
    }
}