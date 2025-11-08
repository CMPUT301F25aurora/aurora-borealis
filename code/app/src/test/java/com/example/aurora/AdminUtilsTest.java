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
