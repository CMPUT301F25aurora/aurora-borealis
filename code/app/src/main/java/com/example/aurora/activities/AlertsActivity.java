/**
 * Shows all notifications for the logged-in user.
 * Listens to Firestore in real time and displays each alert.
 * Supports accepting/declining event invites and dismissing messages.
 */

package com.example.aurora.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.models.NotificationModel;
import com.example.aurora.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
;
import java.util.HashMap;

import java.util.List;
import java.util.Map;


/**
 * Displays real-time notifications for the logged-in user.
 * Listens to Firestore for new notifications, sorts them by time,
 * and shows actionable buttons such as Accept, Decline, or Dismiss.
 */
public class AlertsActivity extends AppCompatActivity {

    private LinearLayout alertsContainer;
    private TextView emptyMsg;
    private FirebaseFirestore db;
    private String userEmail;

    private ListenerRegistration notifListener;

    /**
     * Initializes the Alerts screen, binds UI components, loads the user's email,
     * and begins listening for live notification updates.
     */
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

        listenNotifications();
    }


    /**
     * Sets a real-time Firestore listener on the user's notifications.
     * Automatically refreshes the UI whenever notifications are added, removed,
     * or updated. Sorts notifications by timestamp (newest first).
     */
    private void listenNotifications() {

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "No user email stored", Toast.LENGTH_SHORT).show();
            emptyMsg.setVisibility(View.VISIBLE);
            return;
        }

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

                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    docs.sort((a, b) -> {
                        long t1 = getDocTime(a);
                        long t2 = getDocTime(b);
                        return Long.compare(t2, t1);
                    });

                    for (DocumentSnapshot doc : docs) {
                        addNotificationCard(doc);
                    }
                });
    }

    /**
     * Extracts a timestamp from a notification document.
     * Supports both 'createdAt' and 'timestamp' fields for compatibility.
     *
     * @return time in milliseconds, or 0 if missing.
     */
    private long getDocTime(DocumentSnapshot doc) {
        Long createdAt = doc.getLong("createdAt");
        if (createdAt != null) return createdAt;

        Long ts = doc.getLong("timestamp");
        if (ts != null) return ts;

        return 0L;
    }

    /**
     * Inflates a notification card into the list and populates it with title,
     * message, timestamp, and action buttons. Decides which action buttons to show
     * based on notification type (winner, waiting info, custom message, etc.).
     */
    private void addNotificationCard(DocumentSnapshot doc) {

        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_notification_card, alertsContainer, false);

        TextView title = card.findViewById(R.id.notifTitle);
        TextView msg = card.findViewById(R.id.notifMessage);
        TextView time = card.findViewById(R.id.notifTime);

        View btnAccept = card.findViewById(R.id.btnAccept);
        View btnDecline = card.findViewById(R.id.btnDecline);

        View btnDismiss = card.findViewById(R.id.btnDismiss);

        String notifId = doc.getId();
        String eventId = doc.getString("eventId");
        String notifType = doc.getString("type");

        title.setText(doc.getString("title"));
        msg.setText(doc.getString("message"));

            Object t = doc.get("createdAt");
            if (t == null) t = doc.get("timestamp");

            if (t instanceof Long) {
                time.setText(getRelativeTime((Long) t));
            } else if (t instanceof com.google.firebase.Timestamp) {
                long ms = ((com.google.firebase.Timestamp) t).toDate().getTime();
                time.setText(getRelativeTime(ms));
            } else {
                time.setVisibility(View.GONE);
            }

        if ("waiting_list_info".equals(notifType)
                || "selected_list_info".equals(notifType)
                || "cancelled_list_info".equals(notifType)) {

            btnAccept.setVisibility(View.GONE);
            btnDecline.setVisibility(View.GONE);

            btnDismiss.setVisibility(View.VISIBLE);
            btnDismiss.setOnClickListener(v -> deleteNotification(notifId));
        }
        else if ("not_selected".equals(notifType)) {

            btnAccept.setVisibility(View.GONE);
            btnDecline.setVisibility(View.GONE);
            btnDismiss.setVisibility(View.VISIBLE);
            btnDismiss.setOnClickListener(v -> deleteNotification(notifId));

        }
        else if ("custom_message".equals(notifType)) {

            btnAccept.setVisibility(View.GONE);
            btnDecline.setVisibility(View.GONE);
            btnDismiss.setVisibility(View.VISIBLE);
            btnDismiss.setOnClickListener(v -> deleteNotification(notifId));
        }
        else if ("winner_selected".equals(notifType)){

            btnDismiss.setVisibility(View.GONE);

            btnAccept.setVisibility(View.VISIBLE);
            btnDecline.setVisibility(View.VISIBLE);

            btnAccept.setOnClickListener(v -> acceptEvent(eventId, notifId));
            btnDecline.setOnClickListener(v -> declineEvent(eventId, notifId));
        }
        else {

            btnAccept.setVisibility(View.GONE);
            btnDecline.setVisibility(View.GONE);
            btnDismiss.setVisibility(View.VISIBLE);

            btnDismiss.setOnClickListener(v -> deleteNotification(notifId));
        }

        alertsContainer.addView(card);
    }


    /**
     * Marks the user as having accepted their selected event spot.
     * Updates Firestore and removes the corresponding notification.
     */
    private void acceptEvent(String eventId, String notifId) {

        db.collection("events").document(eventId)
                .update(
                        "acceptedEntrants", FieldValue.arrayUnion(userEmail)
                )
                .addOnSuccessListener(v -> {

                    deleteNotification(notifId);
                    Toast.makeText(this,
                            "You've accepted your spot! Tap Sign Up on the event.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Failed to accept spot. Please try again.",
                        Toast.LENGTH_SHORT
                ).show());
    }

    /**
     * Marks the user as having declined the event spot. Removes them from the
     * selected list, logs the decline, notifies the organizer, and deletes the notification.
     */
    private void declineEvent(String eventId, String notifId) {

        db.collection("events").document(eventId)
                .update(
                        "cancelledEntrants", FieldValue.arrayUnion(userEmail),
                        "selectedEntrants", FieldValue.arrayRemove(userEmail)
                )
                .addOnSuccessListener(v -> {

                    deleteNotification(notifId);


                    Toast.makeText(
                            this,
                            "You’ve declined your spot.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Failed to decline spot.",
                        Toast.LENGTH_SHORT
                ).show());
    }


    /**
     * Removes a notification from Firestore permanently.
     */
    private void deleteNotification(String notifId) {
        db.collection("notifications")
                .document(notifId)
                .delete();
    }

    /**
     * Stops the Firestore real-time listener to prevent memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifListener != null) notifListener.remove();
    }

    /**
     * Formats a timestamp into a human-readable relative time string
     * (e.g., "5 minutes ago", "Yesterday").
     */
    private String getRelativeTime(long millis) {
        return android.text.format.DateUtils.getRelativeTimeSpanString(
                millis,
                System.currentTimeMillis(),
                android.text.format.DateUtils.MINUTE_IN_MILLIS
        ).toString();
    }

}
