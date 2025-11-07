package com.example.aurora;

import android.content.Context;
import android.provider.Settings;

public class UserSession {

    // Returns a stable device-based ID for this user on this app
    public static String getUserId(Context ctx) {
        return Settings.Secure.getString(
                ctx.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }
}
