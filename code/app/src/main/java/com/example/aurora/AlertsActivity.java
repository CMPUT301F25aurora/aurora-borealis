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

        listenNotifications();   // 🔥 Real-time updates
    }

    // ---------------------------------------------------------
    // 🔥 REAL-TIME FIRESTORE LISTENER
    // ---------------------------------------------------------
    private void listenNotifications() {

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "No user email stored", Toast.LENGTH_SHORT).show();
            emptyMsg.setVisibility(View.VISIBLE);
            return;
        }

        // Clean old listener if reopening the screen
        if (notifListener != null) notifListener.remove();

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
    // 🔔 BUILD NOTIFICATION CARD UI
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

        // Timestamp or Long
        if (doc.get("createdAt") instanceof Timestamp) {
            Timestamp t = doc.getTimestamp("createdAt");
            time.setText(formatTime(t != null ? t.toDate().getTime() : 0));
        } else if (doc.get("createdAt") instanceof Long) {
            time.setText(formatTime((Long) doc.get("createdAt")));
        }

        btnAccept.setOnClickListener(v -> acceptEvent(eventId, notifId));
        btnDecline.setOnClickListener(v -> declineEvent(eventId, notifId));

        alertsContainer.addView(card);
    }

    private String formatTime(long ms) {
        return new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                .format(ms);
    }

    // ---------------------------------------------------------
    // ACCEPT / DECLINE ACTIONS
    // ---------------------------------------------------------
    private void acceptEvent(String eventId, String notifId) {

        db.collection("events").document(eventId)
                .update(
                        "finalEntrants", FieldValue.arrayUnion(userEmail),
                        "waitingList", FieldValue.arrayRemove(userEmail),
                        "selectedEntrants", FieldValue.arrayRemove(userEmail)
                )
                .addOnSuccessListener(v -> {

                    deleteNotification(notifId);
                    Toast.makeText(this,
                            "You've accepted your spot!",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void declineEvent(String eventId, String notifId) {

        db.collection("events").document(eventId)
                .update(
                        "cancelledEntrants", FieldValue.arrayUnion(userEmail),
                        "waitingList", FieldValue.arrayRemove(userEmail),
                        "selectedEntrants", FieldValue.arrayRemove(userEmail)
                )
                .addOnSuccessListener(v -> {

                    deleteNotification(notifId);
                    Toast.makeText(this,
                            "You've declined your spot",
                            Toast.LENGTH_SHORT).show();

                    // OPTIONAL: notify organizer
                    sendDeclineNoticeToOrganizer(eventId);
                });
    }

    // ---------------------------------------------------------
    // DELETE NOTIFICATION (listener removes it from UI)
    // ---------------------------------------------------------
    private void deleteNotification(String notifId) {
        db.collection("notifications")
                .document(notifId)
                .delete();
    }

    private void sendDeclineNoticeToOrganizer(String eventId) {
        db.collection("events")
                .document(eventId)
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

                    db.collection("notifications").add(nm);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifListener != null) notifListener.remove();
    }
}
