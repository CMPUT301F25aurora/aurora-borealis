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
