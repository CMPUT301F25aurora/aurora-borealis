/*
 * References for this screen:
 *
 * 1) source: Firebase Cloud Messaging docs — "Firebase Cloud Messaging"
 *    https://firebase.google.com/docs/cloud-messaging
 *    Used as the general model for sending push notifications to entrants.
 *
 * 2) author: Stack Overflow user — "Firebase Cloud Messaging notification from Android app"
 *    https://stackoverflow.com/questions/66656265/firebase-cloud-messaging-notification-from-android-app
 *    Used for the idea of building a JSON payload and sending it to FCM for a topic or token.
 *
 * 3) source: Firebase docs — "Add data to Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/manage-data/add-data
 *    Used for logging notification info in Firestore so the admin log view can show it later.
 */

/**
 * This activity shows a list of notifications for the organizer.
 * - Loads notifications from Firestore (most recent first).
 * - Displays each notification’s title, message, and time.
 * - If there are no notifications, shows a default message.
 */




package com.example.aurora.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class OrganizerNotificationsActivity extends AppCompatActivity {
    private LinearLayout notificationsContainer;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_notifications);

        db = FirebaseFirestore.getInstance();
        notificationsContainer = findViewById(R.id.notificationsContainer);
        // In onCreate:
        ImageButton btnBack = findViewById(R.id.btnBackNotifications);
        btnBack.setOnClickListener(v -> finish());

        loadNotifications();
    }
    private void loadNotifications() {
        // Fetch organizer notifications from Firestore
        db.collection("notificationLogs")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        addNotificationCard("No notifications yet", "You're all caught up!", "");
                        return;
                    }

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("title");
                        String message = doc.getString("message");
                        Object timeObj = doc.get("timestamp");

                        String timeText = "";
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

                        if (timeObj instanceof com.google.firebase.Timestamp) {
                            // Proper Firestore timestamp
                            timeText = sdf.format(((com.google.firebase.Timestamp) timeObj).toDate());
                        }
                        else if (timeObj instanceof Long) {
                            // Old notifications stored as long (System.currentTimeMillis)
                            timeText = sdf.format(new java.util.Date((Long) timeObj));
                        }


                        addNotificationCard(title, message, timeText);
                    }
                });
    }
    private void addNotificationCard(String title, String message, String time) {
        View cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_notification_card_organizer, notificationsContainer, false);


        TextView notifTitle = cardView.findViewById(R.id.notifTitle);
        TextView notifMessage = cardView.findViewById(R.id.notifMessage);
        TextView notifTime = cardView.findViewById(R.id.notifTime);

        notifTitle.setText(title);
        notifMessage.setText(message);
        notifTime.setText(time);

        notificationsContainer.addView(cardView);
    }
}
