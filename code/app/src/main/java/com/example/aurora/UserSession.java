package com.example.aurora;

import android.content.Context;
import android.provider.Settings;

public class UserSession {
    public static String getUserId(Context ctx) {
        return Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
