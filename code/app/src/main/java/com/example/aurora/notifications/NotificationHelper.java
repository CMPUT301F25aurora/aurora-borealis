package com.example.aurora.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.aurora.R;

/**
 * Helper class for creating and managing notification channels and
 * building notifications for the Aurora app.
 *
 * <p>This wrapper ensures that a notification channel exists (Android O+),
 * and provides a clean API for building notifications with a title,
 * message, and optional tap intent.</p>
 */
public class NotificationHelper extends ContextWrapper {

    public static final String CHANNEL_ID = "aurora_notifications";
    public static final String CHANNEL_NAME = "Aurora Lottery Notifications";

    private NotificationManager manager;

    public NotificationHelper(Context base) {
        super(base);
        createChannel();
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH
                    );

            getManager().createNotificationChannel(channel);
        }
    }
    public NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    public NotificationCompat.Builder getNotification(String title, String message, PendingIntent intent) {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.aurora_logo)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
    }
}
