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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

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
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    List<String> waiting = (List<String>) snapshot.get("waitingList");
                    List<String> selected = (List<String>) snapshot.get("selectedEntrants");

                    // Remove declining user from selected + add to cancelled
                    db.collection("events").document(eventId)
                            .update(
                                    "cancelledEntrants", FieldValue.arrayUnion(userEmail),
                                    "selectedEntrants", FieldValue.arrayRemove(userEmail)
                            )
                            .addOnSuccessListener(v -> {

                                deleteNotification(notifId);
                                sendDeclineNoticeToOrganizer(eventId);

                                // ---- PICK NEXT USER ----
                                if (waiting != null && !waiting.isEmpty()) {

                                    String nextUser = waiting.get(0);

                                    db.collection("events").document(eventId)
                                            .update(
                                                    "selectedEntrants", FieldValue.arrayUnion(nextUser),
                                                    "waitingList", FieldValue.arrayRemove(nextUser)
                                            )
                                            .addOnSuccessListener(r -> {

                                                // Notify the next user
                                                sendReplacementNotification(eventId, nextUser);

                                                Toast.makeText(
                                                        this,
                                                        "You’ve declined your spot. Another entrant has been selected.",
                                                        Toast.LENGTH_SHORT
                                                ).show();
                                            });
                                }
                            });
                });
    }



    private void sendReplacementNotification(String eventId, String userEmail) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("userEmail", userEmail);
        notif.put("eventId", eventId);
        notif.put("type", "replacement_chance");
        notif.put("message", "A spot has opened for an event you joined. You have a new chance to register!");
        notif.put("status", "unread");
        notif.put("timestamp", Timestamp.now());

        db.collection("notifications").add(notif);
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

    private void pickReplacementEntrant(String eventId) {

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    List<String> waiting = (List<String>) doc.get("waitingList");
                    List<String> selected = (List<String>) doc.get("selectedEntrants");

                    if (waiting == null || waiting.isEmpty()) {
                        return;
                    }

                    String replacement = waiting.get(0);

                    db.collection("events").document(eventId)
                            .update(
                                    "selectedEntrants", FieldValue.arrayUnion(replacement),
                                    "waitingList", FieldValue.arrayRemove(replacement)
                            )
                            .addOnSuccessListener(v -> sendReplacementNotification(eventId, replacement));
                });
    }


    private void createReplacementNotification(String eventId, String userEmail) {

        String notifId = db.collection("notifications").document().getId();

        Map<String, Object> data = new HashMap<>();
        data.put("id", notifId);
        data.put("eventId", eventId);
        data.put("userEmail", userEmail);
        data.put("type", "replacement_offer");
        data.put("title", "You have been selected!");
        data.put("message", "A spot opened up for an event you joined.");
        data.put("timestamp", System.currentTimeMillis());
        data.put("status", "unread");

        db.collection("notifications").document(notifId).set(data);
    }

    private void markNotificationStatus(String notifId, String status) {
        db.collection("notifications")
                .document(notifId)
                .update("status", status);
    }
}
