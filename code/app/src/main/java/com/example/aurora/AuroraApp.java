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
