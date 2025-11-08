//
// 1. Android Developers — "Create and manage notification channels"
//    https://developer.android.com/develop/ui/views/notifications/channels
//    Used for creating a NotificationChannel on Android 8.0 (API 26) and higher.
//
//
// 2. Android Developers — "Application class"
//    https://developer.android.com/reference/android/app/Application
//    Used for extending Application to initialize app-wide resources in onCreate().


package com.example.aurora;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
/**
 * Application class for the Aurora app.
 *
 * <p>Used to initialize global settings and create the notification
 * channel for sending lottery result updates to users.</p>
 */



public class AuroraApp extends Application {
    public static final String CHANNEL_WINNER = "winner_updates";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_WINNER,
                    "Lottery Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}
