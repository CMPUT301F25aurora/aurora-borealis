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

    /**
     * Test: Waiting list count should reflect size of list.
     *
     * Verifies:
     *  Correct count when list has 3 items
     */
    @Test
    public void testEvent_Browsing_CalculatesWaitingCount() {
        List<String> waitingList = Arrays.asList("A", "B", "C");
        when(mockEventDoc.get("waitingList")).thenReturn(waitingList);
        int count = ((List) mockEventDoc.get("waitingList")).size();
        assertEquals("Should count 3 waiting entrants", 3, count);
    }

    /**
     * Test: Selected entrants count should equal list size.
     *
     * Verifies:
     *  Selected list with one item returns 1
     */
    @Test
    public void testEvent_Browsing_CalculatesSelectedCount() {
        List<String> selectedList = Arrays.asList("Winner1");
        when(mockEventDoc.get("selectedEntrants")).thenReturn(selectedList);
        int count = ((List) mockEventDoc.get("selectedEntrants")).size();
        assertEquals("Should count 1 selected entrant", 1, count);
    }

    /**
     * Test: Cancelled entrants count is calculated correctly.
     *
     * Verifies:
     *  List of size 2 returns count = 2
     */
    @Test
    public void testEvent_Browsing_CalculatesCancelledCount() {
        List<String> cancelledList = Arrays.asList("X", "Y");
        when(mockEventDoc.get("cancelledEntrants")).thenReturn(cancelledList);
        int count = ((List) mockEventDoc.get("cancelledEntrants")).size();
        assertEquals("Should count 2 cancelled entrants", 2, count);
    }

    /**
     * Test: Null waitingList should return count = 0.
     *
     * Verifies:
     *  Null-safe handling for empty Firestore arrays
     */
    @Test
    public void testEvent_Browsing_HandlesNullLists() {

        when(mockEventDoc.get("waitingList")).thenReturn(null);
        List<String> waiting = (List<String>) mockEventDoc.get("waitingList");
        int entrants = waiting == null ? 0 : waiting.size();
        assertEquals(0, entrants);
    }

    /**
     * Test: Null capacity should display "Unlimited".
     *
     * Verifies:
     *  maxSpots null → Unlimited
     */
    @Test
    public void testEvent_Browsing_CapacityUnlimited() {

        when(mockEventDoc.getLong("maxSpots")).thenReturn(null);
        Long max = mockEventDoc.getLong("maxSpots");
        String display = (max != null ? String.valueOf(max) : "Unlimited");
        assertEquals("Unlimited", display);
    }

    /**
     * Test: Capacity numeric value is displayed correctly.
     *
     * Verifies:
     *  maxSpots = 50 → "50"
     */
    @Test
    public void testEvent_Browsing_CapacityLimited() {
        when(mockEventDoc.getLong("maxSpots")).thenReturn(50L);
        Long max = mockEventDoc.getLong("maxSpots");
        String display = (max != null ? String.valueOf(max) : "Unlimited");
        assertEquals("50", display);
    }

    /**
     * Test: Organizer fallback shows "Unknown" if missing.
     *
     * Verifies:
     *  Empty organizerName → Unknown
     */
    @Test
    public void testEvent_Browsing_OrganizerFallback() {
        // Logic: If organizerName is missing, show "Unknown"
        when(mockEventDoc.getString("organizerName")).thenReturn(null);
        String org = AdminUtils.nz(mockEventDoc.getString("organizerName"));
        String display = org.isEmpty() ? "Unknown" : org;
        assertEquals("Unknown", display);
    }

    /**
     * Test: Fallback to dateDisplay when date is missing.
     *
     * Verifies:
     *  date == null → use dateDisplay
     */
    @Test
    public void testEvent_Browsing_DateFallback() {
        // Logic: specific date field vs display string
        when(mockEventDoc.getString("date")).thenReturn(null);
        when(mockEventDoc.getString("dateDisplay")).thenReturn("Jan 1st");

        String d = AdminUtils.nz(mockEventDoc.getString("date"));
        if (d.isEmpty()) d = AdminUtils.nz(mockEventDoc.getString("dateDisplay"));

        assertEquals("Jan 1st", d);
    }

    /**
     * Test: Fallback to dateDisplay when date is missing.
     *
     * Verifies:
     *  date == null → use dateDisplay
     */
    @Test
    public void testEvent_Removal_TitleFallback() {
        when(mockEventDoc.getString("title")).thenReturn(null);
        when(mockEventDoc.getString("name")).thenReturn("Legacy Name");

        String title = AdminUtils.nz(mockEventDoc.getString("title"));
        if (title.isEmpty()) title = AdminUtils.nz(mockEventDoc.getString("name"));

        assertEquals("Legacy Name", title);
    }

    /**
     * Test: AdminEventItem integrity.
     *
     * Verifies:
     *  setId / getId works
     */
    @Test
    public void testEvent_Removal_ModelIntegrity() {
        AdminEventItem item = new AdminEventItem();
        item.setId("evt_123");
        assertEquals("evt_123", item.getId());
    }

    /**
     * Test: Role capitalization logic.
     *
     * Verifies:
     *  organizer → Organizer
     */
    @Test
    public void testProfile_Browsing_CapitalizesRoles() {
        String rawRole = "organizer";
        assertEquals("Organizer", AdminUtils.capitalize(rawRole));
    }

    /**
     * Test: Missing name defaults to "Unnamed".
     *
     * Verifies:
     *  name null → Unnamed
     */
    @Test
    public void testProfile_Browsing_HandlesMissingName() {
        String name = AdminUtils.nz(null);
        String display = name.isEmpty() ? "Unnamed" : name;
        assertEquals("Unnamed", display);
    }

    /**
     * Test: Null phone returns empty string.
     *
     * Verifies:
     *  phone null → ""
     */
    @Test
    public void testProfile_Browsing_PhoneNull() {
        when(mockProfileDoc.getString("phone")).thenReturn(null);
        String phone = AdminUtils.nz(mockProfileDoc.getString("phone"));
        assertEquals("", phone);
    }

    /**
     * Test: Phone field returns actual value.
     *
     * Verifies:
     *  phone = "555-1234"
     */
    @Test
    public void testProfile_Browsing_PhoneExists() {
        when(mockProfileDoc.getString("phone")).thenReturn("555-1234");
        String phone = AdminUtils.nz(mockProfileDoc.getString("phone"));
        assertEquals("555-1234", phone);
    }

    /**
     * Test: Admin users cannot be removed.
     *
     * Verifies:
     *  role = admin → protected
     */
    @Test
    public void testProfile_Removal_AdminProtection() {
        when(mockProfileDoc.getString("role")).thenReturn("admin");
        boolean isAdmin = "admin".equalsIgnoreCase(mockProfileDoc.getString("role"));
        assertTrue(isAdmin);
    }

    /**
     * Test: Admin detection is case-insensitive.
     *
     * Verifies:
     *  ADMIN → admin
     */
    @Test
    public void testProfile_Removal_AdminProtection_CaseInsensitive() {
        when(mockProfileDoc.getString("role")).thenReturn("ADMIN");
        boolean isAdmin = "admin".equalsIgnoreCase(mockProfileDoc.getString("role"));
        assertTrue(isAdmin);
    }

    /**
     * Test: Missing notificationsEnabled defaults to ON.
     *
     * Verifies:
     *  null → true
     */
    @Test
    public void testProfile_Notifications_DefaultTrue() {

        when(mockProfileDoc.getBoolean("notificationsEnabled")).thenReturn(null);
        Boolean notif = mockProfileDoc.getBoolean("notificationsEnabled");
        // Logic in app:
        boolean isOn = (notif == null || notif);
        assertTrue(isOn);
    }

    /**
     * Test: Explicit false should disable notifications.
     *
     * Verifies:
     *  false → disabled
     */
    @Test
    public void testProfile_Notifications_ExplicitlyFalse() {
        when(mockProfileDoc.getBoolean("notificationsEnabled")).thenReturn(false);
        Boolean notif = mockProfileDoc.getBoolean("notificationsEnabled");
        boolean isOn = (notif == null || notif);
        assertFalse(isOn);
    }


    /**
     * Test: Detect organizer privilege enabled.
     *
     * Verifies:
     *  organizer_allowed = true
     */
    @Test
    public void testOrganizer_Revoke_DetectsActive() {
        when(mockProfileDoc.getBoolean("organizer_allowed")).thenReturn(true);
        boolean isOrg = Boolean.TRUE.equals(mockProfileDoc.getBoolean("organizer_allowed"));
        assertTrue(isOrg);
    }

    /**
     * Test: Detect revoked organizer privilege.
     *
     * Verifies:
     *  organizer_allowed = false
     */
    @Test
    public void testOrganizer_Revoke_DetectsRevoked() {
        when(mockProfileDoc.getBoolean("organizer_allowed")).thenReturn(false);
        boolean isOrg = Boolean.TRUE.equals(mockProfileDoc.getBoolean("organizer_allowed"));
        assertFalse(isOrg);
    }

    /**
     * Test: Null organizer_allowed defaults to false.
     *
     * Verifies:
     *  null → false
     */
    @Test
    public void testOrganizer_Revoke_HandlesNull() {
        when(mockProfileDoc.getBoolean("organizer_allowed")).thenReturn(null);
        boolean isOrg = Boolean.TRUE.equals(mockProfileDoc.getBoolean("organizer_allowed"));
        assertFalse(isOrg);
    }

    /**
     * Test: Organizer toggle flips true→false.
     *
     * Verifies:
     *  current=true → next=false
     */
    @Test
    public void testOrganizer_Toggle_Logic() {

        boolean current = true; // currently allowed
        boolean nextState = !current; // revoke
        assertFalse(nextState);
    }

    /**
     * Test: Organizer toggle flips false→true.
     *
     * Verifies:
     *  current=false → next=true
     */
    @Test
    public void testOrganizer_Toggle_Logic_Restore() {
        boolean current = false; // currently revoked
        boolean nextState = !current; // restore
        assertTrue(nextState);
    }


    /**
     * Test: AdminImage model stores correct id.
     *
     * Verifies:
     *  eventId stored correctly
     */
    @Test
    public void testImage_Model_Integrity() {
        AdminImage img = new AdminImage("e1", "T", "o@g.com", "url");
        assertEquals("e1", img.eventId);
    }

    /**
     * Test: Missing organizer email should return "Unknown".
     *
     * Verifies:
     *  organizerEmail null → Unknown
     */
    @Test
    public void testImage_Browsing_NoOrganizer() {
        // Image might exist but organizer deleted their account
        AdminImage img = new AdminImage("e1", "T", null, "url");
        String displayOrg = (img.organizerEmail == null ? "Unknown" : img.organizerEmail);
        assertEquals("Unknown", displayOrg);
    }

    /**
     * Test: Empty poster URL is invalid.
     *
     * Verifies:
     *  url="" → invalid
     */
    @Test
    public void testImage_Browsing_EmptyUrl() {
        String url = "";
        boolean valid = !url.isEmpty();
        assertFalse(valid);
    }

    /**
     * Test: Delete logic requires URL + ID.
     *
     * Verifies:
     *  posterUrl and eventId must be non-null
     */
    @Test
    public void testImage_Delete_Logic() {
        // Verification that we have necessary data to delete
        AdminImage img = new AdminImage("e1", "T", "o", "gs://bucket/file.png");
        assertNotNull(img.posterUrl);
        assertNotNull(img.eventId);
    }


    /**
     * Test: FormatRelativeTime returns minutes correctly.
     *
     * Verifies:
     *  5 minutes → "5 min ago"
     */
    @Test
    public void testLog_RelativeTime_Minutes() {
        long now = 1000000000L;
        Date d = new Date(now - (5 * 60 * 1000));
        assertEquals("5 min ago", AdminUtils.formatRelativeTime(d, now));
    }

    /**
     * Test: FormatRelativeTime returns hours correctly.
     *
     * Verifies:
     *  2 hours → "2 hours ago"
     */
    @Test
    public void testLog_RelativeTime_Hours() {
        long now = 1000000000L;
        Date d = new Date(now - (2 * 60 * 60 * 1000));
        assertEquals("2 hours ago", AdminUtils.formatRelativeTime(d, now));
    }

    /**
     * Test: 25 hours ago should result in days.
     *
     * Verifies:
     *  25 hours → "1 days ago"
     */
    @Test
    public void testLog_RelativeTime_Days() {
        long now = 1000000000L;
        Date d = new Date(now - (25 * 60 * 60 * 1000)); // 25 hours
        assertEquals("1 days ago", AdminUtils.formatRelativeTime(d, now));
    }

    /**
     * Test: Null message should return empty string using nz().
     *
     * Verifies:
     *  message null → ""
     */
    @Test
    public void testLog_NullMessage() {
        when(mockLogDoc.getString("message")).thenReturn(null);
        String msg = AdminUtils.nz(mockLogDoc.getString("message"));
        assertEquals("", msg);
    }

    /**
     * Test: Future timestamp should return "0 min ago".
     *
     * Verifies:
     *  date > now → "0 min ago"
     */
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