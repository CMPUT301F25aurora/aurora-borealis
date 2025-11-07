package com.example.aurora;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * Pure unit tests for logic used by the admin features.
 * (Covers pieces of US 03.04, 03.05, 03.08 where formatting / helpers are used.)
 */
public class AdminUtilsTest {

    @Test
    public void nz_returnsEmptyStringForNull() {
        assertEquals("", AdminUtils.nz(null));
    }

    @Test
    public void nz_returnsSameStringForNonNull() {
        assertEquals("hello", AdminUtils.nz("hello"));
    }

    @Test
    public void capitalize_handlesNullAndEmpty() {
        assertEquals("", AdminUtils.capitalize(null));
        assertEquals("", AdminUtils.capitalize(""));
    }

    @Test
    public void capitalize_capitalizesFirstLetterAndLowercasesRest() {
        assertEquals("Admin", AdminUtils.capitalize("admin"));
        assertEquals("Admin", AdminUtils.capitalize("ADMIN"));
        assertEquals("Admin", AdminUtils.capitalize("aDmIn"));
    }

    @Test
    public void formatRelativeTime_underOneHour_showsMinutes() {
        long now = System.currentTimeMillis();
        // 15 minutes ago
        Date fifteenMinutesAgo = new Date(now - 15L * 60L * 1000L);
        String result = AdminUtils.formatRelativeTime(fifteenMinutesAgo, now);
        assertEquals("15 min ago", result);
    }

    @Test
    public void formatRelativeTime_underOneDay_showsHours() {
        long now = System.currentTimeMillis();
        // 5 hours ago
        Date fiveHoursAgo = new Date(now - 5L * 60L * 60L * 1000L);
        String result = AdminUtils.formatRelativeTime(fiveHoursAgo, now);
        assertEquals("5 hours ago", result);
    }

    @Test
    public void formatRelativeTime_overOneDay_showsDays() {
        // Pick a fixed "now" so test is deterministic
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 10, 12, 0, 0);
        long now = cal.getTimeInMillis();

        // 3 days earlier
        Date threeDaysAgo = new Date(now - 3L * 24L * 60L * 60L * 1000L);

        String result = AdminUtils.formatRelativeTime(threeDaysAgo, now);
        assertEquals("3 days ago", result);
    }
}
