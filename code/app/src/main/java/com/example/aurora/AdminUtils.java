package com.example.aurora;

import java.util.Date;
/**
 * A small set of helper methods used across admin-related classes.
 *
 * Provides simple utilities for working with strings and formatting time
 * differences in a readable way. These methods help keep the admin UI
 * code cleaner and easier to maintain.
 *
 * Includes:
 * - nz(String): returns a non-null string (empty if null)
 * - capitalize(String): capitalizes the first letter of a string
 * - formatRelativeTime(Date, long): converts a date into a readable
 *   relative time like "5 min ago" or "2 hours ago"
 */


public class AdminUtils {

    /** Null-safe string: never returns null. */
    public static String nz(String s) {
        return s == null ? "" : s;
    }

    /** Capitalize first letter, lower-case the rest. */
    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /**
     * Same logic as AdminActivity.formatRelativeTime, but with an injectable "now"
     * so we can unit test deterministically.
     */
    public static String formatRelativeTime(Date date, long nowMillis) {
        long diff = nowMillis - date.getTime();
        long minutes = diff / (60 * 1000);
        if (minutes < 60) {
            return minutes + " min ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " hours ago";
        }
        long days = hours / 24;
        return days + " days ago";
    }
}
