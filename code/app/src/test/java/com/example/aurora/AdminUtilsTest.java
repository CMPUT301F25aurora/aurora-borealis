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
    /**
     * Ensures {@link AdminUtils#nz(String)} returns an empty string when
     * passed null, preventing NullPointerExceptions in admin UI screens.
     */
    @Test
    public void nz_NullString_ReturnsEmpty() {
        assertEquals("", AdminUtils.nz(null));
    }
    /**
     * Verifies {@link AdminUtils#nz(String)} returns the original string
     * when the argument is non-null and non-empty.
     */
    @Test
    public void nz_ValidString_ReturnsString() {
        assertEquals("hello", AdminUtils.nz("hello"));
    }
    /**
     * Ensures that an empty input string remains empty, avoiding accidental
     * conversion to null or placeholder text.
     */
    @Test
    public void nz_EmptyString_ReturnsEmpty() {
        assertEquals("", AdminUtils.nz(""));
    }

    // --- Capitalization Tests ---
    /**
     * Ensures lowercase input is correctly capitalized (first letter uppercase,
     * remaining letters lowercase) for profile role display.
     */
    @Test
    public void capitalize_LowerCase_ReturnsCapitalized() {
        assertEquals("Admin", AdminUtils.capitalize("admin"));
    }
    /**
     * Ensures uppercase inputs are normalized into proper display form.
     * The method lowercases the tail substring, so "ADMIN" becomes "Admin".
     */
    @Test
    public void capitalize_UpperCase_ReturnsCapitalized() {

        assertEquals("Admin", AdminUtils.capitalize("ADMIN"));
    }
    /**
     * Ensures mixed-case roles or names are normalized for UI readability.
     */
    @Test
    public void capitalize_MixedCase_ReturnsCapitalized() {
        assertEquals("Entrant", AdminUtils.capitalize("eNtRaNt"));
    }
    /**
     * Verifies that empty strings return empty output with no exceptions.
     */
    @Test
    public void capitalize_Empty_ReturnsEmpty() {
        assertEquals("", AdminUtils.capitalize(""));
    }
    /**
     * Ensures null input is safely handled and returns an empty string,
     * preventing crashes in RecyclerViews that display roles.
     */
    @Test
    public void capitalize_Null_ReturnsEmpty() {
        assertEquals("", AdminUtils.capitalize(null));
    }

    // --- Relative Time Tests ---
    /**
     * Verifies that timestamps representing events 5 minutes ago are formatted
     * correctly as "5 min ago". Supports Admin log viewing (US 03.08.01).
     */
    @Test
    public void relativeTime_JustNow_ReturnsMinutes() {
        long now = 1000000000L;
        long eventTime = now - (5 * 60 * 1000);
        Date d = new Date(eventTime);

        assertEquals("5 min ago", AdminUtils.formatRelativeTime(d, now));
    }
    /**
     * Ensures values under one hour (e.g., 59 minutes) return a minutes-only
     * string instead of switching prematurely to hours.
     */
    @Test
    public void relativeTime_UnderOneHour_ReturnsMinutes() {
        long now = 1000000000L;
        long eventTime = now - (59 * 60 * 1000);
        Date d = new Date(eventTime);

        assertEquals("59 min ago", AdminUtils.formatRelativeTime(d, now));
    }
    /**
     * Ensures timestamps older than one hour are reported using hour units
     * rather than raw minutes (e.g., “1 hours ago”).
     */
    @Test
    public void relativeTime_OverOneHour_ReturnsHours() {
        long now = 1000000000L;
        long eventTime = now - (61 * 60 * 1000); // 1 hr 1 min
        Date d = new Date(eventTime);

        assertEquals("1 hours ago", AdminUtils.formatRelativeTime(d, now));
    }
    /**
     * Ensures timestamps older than 24 hours are displayed in whole days.
     * For 25 hours difference, result should be "1 days ago".
     */
    @Test
    public void relativeTime_OverOneDay_ReturnsDays() {
        long now = 1000000000L;
        long eventTime = now - (25 * 60 * 60 * 1000); // 25 hours
        Date d = new Date(eventTime);

        assertEquals("1 days ago", AdminUtils.formatRelativeTime(d, now));
    }
}
