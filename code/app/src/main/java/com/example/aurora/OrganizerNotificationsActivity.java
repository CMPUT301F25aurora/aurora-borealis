package com.example.aurora;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
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

        loadNotifications();
    }

    private void loadNotifications() {
        // Fetch organizer notifications from Firestore
        db.collection("notifications")
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
                        if (timeObj != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
                            timeText = sdf.format(((com.google.firebase.Timestamp) timeObj).toDate());
                        }

                        addNotificationCard(title, message, timeText);
                    }
                });
    }

    private void addNotificationCard(String title, String message, String time) {
        View cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_notification_card, notificationsContainer, false);

        TextView notifTitle = cardView.findViewById(R.id.notifTitle);
        TextView notifMessage = cardView.findViewById(R.id.notifMessage);
        TextView notifTime = cardView.findViewById(R.id.notifTime);

        notifTitle.setText(title);
        notifMessage.setText(message);
        notifTime.setText(time);

        notificationsContainer.addView(cardView);
    }
}