package com.example.aurora;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AlertsActivity extends AppCompatActivity {

    private LinearLayout alertsContainer;
    private TextView emptyMsg;
    private FirebaseFirestore db;
    private String userEmail;

    private ListenerRegistration notifListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_alerts);

        db = FirebaseFirestore.getInstance();

        userEmail = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_email", "");

        alertsContainer = findViewById(R.id.alertsContainer);
        emptyMsg = findViewById(R.id.alertsMessage);

        ImageButton back = findViewById(R.id.backButtonAlerts);
        if (back != null) back.setOnClickListener(v -> onBackPressed());

        listenNotifications();   // 🔥 REAL-TIME LISTENER (FIXES EVERYTHING)
    }

    // ---------------------------------------------------------
    // 🔥 REAL-TIME SNAPSHOT LISTENER (not .get() anymore)
    // ---------------------------------------------------------
    private void listenNotifications() {

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "No user email stored", Toast.LENGTH_SHORT).show();
            emptyMsg.setVisibility(View.VISIBLE);
            return;
        }

        // Remove old listener if exists
        if (notifListener != null) {
            notifListener.remove();
        }

        notifListener = db.collection("notifications")
                .whereEqualTo("userId", userEmail)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null || snapshot == null) return;

                    alertsContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        emptyMsg.setVisibility(View.VISIBLE);
                        return;
                    }

                    emptyMsg.setVisibility(View.GONE);

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        addNotificationCard(doc);
                    }
                });
    }

    // ---------------------------------------------------------
    // 🔔 UI CARD FOR EACH NOTIFICATION
    // ---------------------------------------------------------
    private void addNotificationCard(DocumentSnapshot doc) {
        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_notification_card, alertsContainer, false);

        TextView title = card.findViewById(R.id.notifTitle);
        TextView msg = card.findViewById(R.id.notifMessage);
        TextView time = card.findViewById(R.id.notifTime);

        View btnAccept = card.findViewById(R.id.btnAccept);
        View btnDecline = card.findViewById(R.id.btnDecline);

        String notifId = doc.getId();
        String eventId = doc.getString("eventId");

        title.setText(doc.getString("title"));
        msg.setText(doc.getString("message"));

        // Support both Timestamp AND Long
        if (doc.get("createdAt") instanceof Timestamp) {
            Timestamp t = doc.getTimestamp("createdAt");
            if (t != null) {
                time.setText(new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        .format(t.toDate()));
            }
        } else if (doc.get("createdAt") instanceof Long) {
            long ms = (long) doc.get("createdAt");
            time.setText(new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    .format(ms));
        }

        btnAccept.setOnClickListener(v -> acceptEvent(eventId, notifId));
        btnDecline.setOnClickListener(v -> declineEvent(eventId, notifId));

        alertsContainer.addView(card);
    }

    // ---------------------------------------------------------
    // ACCEPT / DECLINE HANDLERS
    // ---------------------------------------------------------
    private void acceptEvent(String eventId, String notifId) {
        db.collection("events").document(eventId)
                .update(
                        "finalEntrants", com.google.firebase.firestore.FieldValue.arrayUnion(userEmail),
                        "waitingList", com.google.firebase.firestore.FieldValue.arrayRemove(userEmail),
                        "selectedEntrants", com.google.firebase.firestore.FieldValue.arrayRemove(userEmail)
                )
                .addOnSuccessListener(v -> {
                    markNotificationStatus(notifId, "accepted");
                    Toast.makeText(this, "You've accepted your spot!", Toast.LENGTH_SHORT).show();
                });
    }

    private void declineEvent(String eventId, String notifId) {
        db.collection("events").document(eventId)
                .update(
                        "cancelledEntrants", com.google.firebase.firestore.FieldValue.arrayUnion(userEmail),
                        "waitingList", com.google.firebase.firestore.FieldValue.arrayRemove(userEmail),
                        "selectedEntrants", com.google.firebase.firestore.FieldValue.arrayRemove(userEmail)
                )
                .addOnSuccessListener(v -> {
                    markNotificationStatus(notifId, "declined");
                    Toast.makeText(this, "You've declined your spot", Toast.LENGTH_SHORT).show();
                });
    }


    private void markNotificationStatus(String notifId, String status) {
        db.collection("notifications")
                .document(notifId)
                .update("status", status);
    }

    private void sendDeclineNoticeToOrganizer(String eventId) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    String organizerEmail = doc.getString("organizerEmail");
                    if (organizerEmail == null) return;

                    NotificationModel nm = new NotificationModel(
                            "entrant_declined",
                            "Entrant Declined",
                            userEmail + " declined their spot.",
                            eventId,
                            organizerEmail,
                            System.currentTimeMillis()
                    );

                    db.collection("notifications")
                            .add(nm);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifListener != null) notifListener.remove();
    }
}
