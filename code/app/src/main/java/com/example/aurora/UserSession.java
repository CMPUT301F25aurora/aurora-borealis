/*
 * References for this helper:
 *
 * 1) source: Android Developers — "Save key-value data"
 *    https://developer.android.com/training/data-storage/shared-preferences
 *    https://developer.android.com/topic/libraries/architecture/datastore
 *    https://developer.android.com/kotlin/multiplatform/datastore
 *    Used for storing simple session values like user id, email, and role in SharedPreferences.
 *
 *
 * 2) source: Android Developers — "The activity lifecycle"
 *    https://developer.android.com/guide/components/activities/activity-lifecycle
 *    Used to understand when to read or clear session data as Activities start or finish.
 */


package com.example.aurora;

import android.content.Context;
import android.provider.Settings;
/**
 * UserSession.java
 *
 * Utility class for retrieving a unique, device-based user ID.
 * - Uses Android's Secure ANDROID_ID to generate a consistent identifier.
 * - Provides a simple way to reference the current user across app sessions.
 */

public class UserSession {

    // Returns a stable device-based ID for this user on this app
    public static String getUserId(Context ctx) {
        return Settings.Secure.getString(
                ctx.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }
}
