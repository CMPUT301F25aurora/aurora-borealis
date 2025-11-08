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


package com.example.aurora;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

        loadEvent();
    }

    private void bindViews() {
        backButton = findViewById(R.id.btnBackEventDetails);
        titleView = findViewById(R.id.detailTitle);
        dateView = findViewById(R.id.detailDate);
        locationView = findViewById(R.id.detailLocation);
        categoryView = findViewById(R.id.detailCategory);
        capacityView = findViewById(R.id.detailCapacity);
        regWindowView = findViewById(R.id.detailRegWindow);
        waitingListContainer = findViewById(R.id.waitingListContainer);
    }

    private void loadEvent() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(this::populateUi)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateUi(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Double-check ownership: only creator can view details
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

        // Capacity
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

        // Registration window
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

        // Waiting list
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
}
