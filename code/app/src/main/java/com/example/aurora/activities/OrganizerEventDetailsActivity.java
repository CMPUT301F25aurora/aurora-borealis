/*
 * References for this screen:
 *
 * 1) source: Firebase docs — "Get data with Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/query-data/get-data
 *    Used for loading a single event document and its fields for organizer view.
 *
 * 2) source: Firebase docs — "Add data to Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/manage-data/add-data
 *    Used for updating event fields such as description or dates from this screen.
 *
 * 3) author: Stack Overflow user — "How to generate a QR Code for an Android application?"
 *    https://stackoverflow.com/questions/8800919/how-to-generate-a-qr-code-for-an-android-application
 *    Used for the QRCodeWriter pattern if the organizer screen shows its own QR code.
 *
 * 4) source: Android Developers — "Dialogs"
 *    https://developer.android.com/develop/ui/views/components/dialogs
 *    Used for confirmation dialogs when organizer performs actions like delete or close event.
 */


package com.example.aurora.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.R;
import com.example.aurora.notifications.FirestoreNotificationHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * OrganizerEventDetailsActivity.java
 *
 * Displays detailed information for a specific event created by the organizer.
 * - Loads event data from Firestore and shows title, date, category, location, and capacity.
 * - Displays the registration window and current waiting list of entrants.
 * - Ensures only the event creator (organizer) can access their event details.
 * - Provides a simple back button for navigation.
 */
public class OrganizerEventDetailsActivity extends AppCompatActivity {
    private FirebaseFirestore db;

    private ImageButton backButton;
    private TextView titleView, dateView, locationView, categoryView, capacityView, regWindowView;
    private LinearLayout waitingListContainer;
    private String eventId;
    private String myEmail;
    private Button notifyWaitingBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_event_details);

        db = FirebaseFirestore.getInstance();
        myEmail = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_email", null);

        eventId = getIntent().getStringExtra("eventId");
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, "No event ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        backButton.setOnClickListener(v -> onBackPressed());
        notifyWaitingBtn.setOnClickListener(v -> notifyWaitingList());
        loadEvent();
    }

    /**
     * Connects XML layout views to their Java variables.
     */
    private void bindViews() {
        backButton = findViewById(R.id.btnBackEventDetails);
        titleView = findViewById(R.id.detailTitle);
        dateView = findViewById(R.id.detailDate);
        locationView = findViewById(R.id.detailLocation);
        categoryView = findViewById(R.id.detailCategory);
        capacityView = findViewById(R.id.detailCapacity);
        regWindowView = findViewById(R.id.detailRegWindow);
        waitingListContainer = findViewById(R.id.waitingListContainer);
        notifyWaitingBtn = findViewById(R.id.btnNotifyWaiting);
    }

    /**
     * Loads the event document from Firestore and passes it to populateUi().
     */
    private void loadEvent() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(this::populateUi)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Fills the screen with event details:
     *   title, date, category, location
     *   capacity
     *   registration window
     *   waiting list entries
     *
     * Also checks that the logged-in organizer is the creator of this event.
     */
    private void populateUi(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String creatorEmail = doc.getString("organizerEmail");
        if (myEmail == null || !myEmail.equalsIgnoreCase(creatorEmail)) {
            Toast.makeText(this, "You are not allowed to view this event.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String title = doc.getString("title");
        if (title == null) title = doc.getString("name");
        if (title == null) title = "Untitled Event";

        String date = doc.getString("date");
        if (date == null) date = doc.getString("startDate");
        if (date == null) date = "Date not set";

        String location = doc.getString("location");
        if (location == null) location = "Location not set";

        String category = doc.getString("category");
        if (category == null) category = "General";

        Long maxSpots = doc.getLong("maxSpots");
        if (maxSpots == null) {
            Object capObj = doc.get("capacity");
            if (capObj instanceof Number) {
                maxSpots = ((Number) capObj).longValue();
            } else if (capObj instanceof String) {
                try {
                    maxSpots = Long.parseLong((String) capObj);
                } catch (NumberFormatException e) {
                    maxSpots = 0L;
                }
            } else {
                maxSpots = 0L;
            }
        }

        String regStart = doc.getString("registrationStart");
        String regEnd = doc.getString("registrationEnd");
        String regWindow = (regStart == null && regEnd == null)
                ? "Not set"
                : (regStart + " → " + regEnd);

        titleView.setText(title);
        dateView.setText(date);
        locationView.setText(location);
        categoryView.setText(category);
        capacityView.setText(String.valueOf(maxSpots));
        regWindowView.setText(regWindow);

        waitingListContainer.removeAllViews();

        List<String> waitingList = (List<String>) doc.get("waitingList");
        if (waitingList == null || waitingList.isEmpty()) {
            TextView tv = buildWaitingRow("No one on the waiting list yet.");
            waitingListContainer.addView(tv);
        } else {
            for (String idOrEmail : waitingList) {
                TextView tv = buildWaitingRow(idOrEmail);
                waitingListContainer.addView(tv);
            }
        }
    }

    /**
     * Builds a simple TextView row used to display one waiting-list entry.
     */
    private TextView buildWaitingRow(String text) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tv.setText("• " + text);
        tv.setTextSize(14f);
        tv.setPadding(8, 4, 8, 4);
        return tv;
    }

    /**
     * Sends a notification to everyone in the event's waiting list.
     * Uses FirestoreNotificationHelper to send one message per entrant.
     */
    private void notifyWaitingList() {

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {

                    List<String> waitingList = (List<String>) doc.get("waitingList");

                    if (waitingList == null || waitingList.isEmpty()) {
                        Toast.makeText(this, "Waiting list is empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String eventName = doc.getString("title");
                    if (eventName == null) eventName = "Event";

                    for (String userIdOrEmail : waitingList) {
                        FirestoreNotificationHelper.sendWaitingListNotification(
                                db,
                                userIdOrEmail,
                                eventName,
                                eventId,
                                myEmail
                        );
                    }

                    Toast.makeText(this, "Waiting list notified!", Toast.LENGTH_SHORT).show();

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to notify waiting list.", Toast.LENGTH_SHORT).show()
                );
    }
}
