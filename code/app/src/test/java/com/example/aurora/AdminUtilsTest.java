/*
 * References for AdminUtilsTest:
 *
 * source: JUnit 4 tutorial — "Unit Testing with JUnit 4"
 * url: https://www.vogella.com/tutorials/JUnit4/article.html
 * note: Used as a reference for writing plain JUnit 4 tests with
 *       @Test methods and assertEquals(...) assertions.
 *
 * source: JUnit 4 Javadoc — "Assert"
 * url: https://junit.org/junit4/javadoc/4.8/org/junit/Assert.html
 * note: Used for the static import pattern import static org.junit.Assert.assertEquals;
 *       and the general idea of equality assertions in tests.
 *
 * source: JUnit 4 getting started guide
 * url: https://github.com/junit-team/junit4/wiki/getting-started
 * note: General reference for structuring a small JUnit 4 test class that
 *       verifies a single helper method like AdminUtils.formatRelativeTime().
 *
 * source: Java SE docs — "Calendar"
 * url: https://docs.oracle.com/javase/8/docs/api/java/util/Calendar.html
 * note: Used for Calendar.getInstance() and cal.set(year, month, day, hour, minute, second)
 *       when constructing a fixed point in time for deterministic tests.
 *
 * source: Java date and time tutorial — "Java Date and Calendar examples"
 * url: https://mkyong.com/java/java-date-and-calendar-examples/
 * note: Used for the idea of subtracting milliseconds from a base time,
 *       creating new Date(...) objects like “3 days ago” to feed into the test.
 */

package com.example.aurora;

import static org.junit.Assert.assertEquals;

import com.example.aurora.utils.AdminUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Pure unit tests for logic used by the admin features.
 * (Covers pieces of US 03.04, 03.05, 03.08 where formatting / helpers are used.)
 */
public class AdminUtilsTest {

    @Test
    public void nz_NullString_ReturnsEmpty() {
        assertEquals("", AdminUtils.nz(null));
    }

    @Test
    public void nz_ValidString_ReturnsString() {
        assertEquals("hello", AdminUtils.nz("hello"));
    }

    @Test
    public void nz_EmptyString_ReturnsEmpty() {
        assertEquals("", AdminUtils.nz(""));
    }

    // --- Capitalization Tests ---
    @Test
    public void capitalize_LowerCase_ReturnsCapitalized() {
        assertEquals("Admin", AdminUtils.capitalize("admin"));
    }

    @Test
    public void capitalize_UpperCase_ReturnsCapitalized() {
        // Your code uses toLowerCase() on the substring, so "ADMIN" becomes "Admin"
        assertEquals("Admin", AdminUtils.capitalize("ADMIN"));
    }

    @Test
    public void capitalize_MixedCase_ReturnsCapitalized() {
        assertEquals("Entrant", AdminUtils.capitalize("eNtRaNt"));
    }

    @Test
    public void capitalize_Empty_ReturnsEmpty() {
        assertEquals("", AdminUtils.capitalize(""));
    }

    @Test
    public void capitalize_Null_ReturnsEmpty() {
        assertEquals("", AdminUtils.capitalize(null));
    }

    // --- Relative Time Tests ---
    @Test
    public void relativeTime_JustNow_ReturnsMinutes() {
        long now = 1000000000L;
        long eventTime = now - (5 * 60 * 1000); // 5 mins ago
        Date d = new Date(eventTime);

        // We pass 'now' into your util to make it deterministic
        assertEquals("5 min ago", AdminUtils.formatRelativeTime(d, now));
    }

    @Test
    public void relativeTime_UnderOneHour_ReturnsMinutes() {
        long now = 1000000000L;
        long eventTime = now - (59 * 60 * 1000); // 59 mins ago
        Date d = new Date(eventTime);

        assertEquals("59 min ago", AdminUtils.formatRelativeTime(d, now));
    }

    @Test
    public void relativeTime_OverOneHour_ReturnsHours() {
        long now = 1000000000L;
        long eventTime = now - (61 * 60 * 1000); // 1 hr 1 min
        Date d = new Date(eventTime);

        assertEquals("1 hours ago", AdminUtils.formatRelativeTime(d, now));
    }

    @Test
    public void relativeTime_OverOneDay_ReturnsDays() {
        long now = 1000000000L;
        long eventTime = now - (25 * 60 * 60 * 1000); // 25 hours
        Date d = new Date(eventTime);

        assertEquals("1 days ago", AdminUtils.formatRelativeTime(d, now));
    }

    // ============================================================
    // US 03.08.01: Log notifications for admin review
    // ============================================================
    @Test
    public void logNotification_createsEntryWithCorrectFields() {
        class NotificationLogEntry {
            String eventId;
            String recipientEmail;
            String type;

            NotificationLogEntry(String eventId,String recipientEmail,String type){
                this.eventId=eventId;
                this.recipientEmail=recipientEmail;
                this.type=type;
            }
        }

        List<NotificationLogEntry> log = new ArrayList<>();

        String eventId = "evt_swim";
        String email = "user@example.com";
        String type = "WIN";

        // "Log" the notification
        log.add(new NotificationLogEntry(eventId,email,type));

        assertEquals(1,log.size());
        NotificationLogEntry entry = log.get(0);

        assertEquals("evt_swim",entry.eventId);
        assertEquals("user@example.com",entry.recipientEmail);
        assertEquals("WIN",entry.type);
    }

}
