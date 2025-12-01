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
     * Test: nz(null) should return "".
     *
     * Verifies:
     *  null input handled safely
     */
    @Test
    public void nz_NullString_ReturnsEmpty() {
        assertEquals("", AdminUtils.nz(null));
    }

    /**
     * Test: nz("hello") should return "hello".
     *
     * Verifies:
     *  non-empty string is unchanged
     */
    @Test
    public void nz_ValidString_ReturnsString() {
        assertEquals("hello", AdminUtils.nz("hello"));
    }

    /**
     * Test: nz("") returns "".
     *
     * Verifies:
     *  empty string is passed through
     */
    @Test
    public void nz_EmptyString_ReturnsEmpty() {
        assertEquals("", AdminUtils.nz(""));
    }

    /**
     * Test: capitalize("admin") → "Admin".
     *
     * Verifies:
     *  lowercase input becomes capitalized
     */
    @Test
    public void capitalize_LowerCase_ReturnsCapitalized() {
        assertEquals("Admin", AdminUtils.capitalize("admin"));
    }

    /**
     * Test: capitalize("ADMIN") → "Admin".
     *
     * Verifies:
     *  uppercase input normalized to capitalized
     */
    @Test
    public void capitalize_UpperCase_ReturnsCapitalized() {

        assertEquals("Admin", AdminUtils.capitalize("ADMIN"));
    }

    /**
     * Test: capitalize("eNtRaNt") → "Entrant".
     *
     * Verifies:
     *  mixed case normalized to capitalized
     */
    @Test
    public void capitalize_MixedCase_ReturnsCapitalized() {
        assertEquals("Entrant", AdminUtils.capitalize("eNtRaNt"));
    }

    /**
     * Test: capitalize("") → "".
     *
     * Verifies:
     *  empty input returns empty output
     */
    @Test
    public void capitalize_Empty_ReturnsEmpty() {
        assertEquals("", AdminUtils.capitalize(""));
    }

    /**
     * Test: capitalize(null) → "".
     *
     * Verifies:
     *  null input handled safely
     */
    @Test
    public void capitalize_Null_ReturnsEmpty() {
        assertEquals("", AdminUtils.capitalize(null));
    }

    /**
     * Test: 5 minutes ago should display "5 min ago".
     *
     * Verifies:
     *  minute-level relative calculation
     */
    @Test
    public void relativeTime_JustNow_ReturnsMinutes() {
        long now = 1000000000L;
        long eventTime = now - (5 * 60 * 1000);
        Date d = new Date(eventTime);

        assertEquals("5 min ago", AdminUtils.formatRelativeTime(d, now));
    }

    /**
     * Test: 59 minutes ago returns "59 min ago".
     *
     * Verifies:
     *  upper bound of minute-only formatting
     */
    @Test
    public void relativeTime_UnderOneHour_ReturnsMinutes() {
        long now = 1000000000L;
        long eventTime = now - (59 * 60 * 1000);
        Date d = new Date(eventTime);

        assertEquals("59 min ago", AdminUtils.formatRelativeTime(d, now));
    }
}
