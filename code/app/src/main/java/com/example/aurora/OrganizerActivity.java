/**
 * OrganizerActivity.java
 *
 * Main screen for organizers in the Aurora app.
 * - Displays all events retrieved from Firestore ("events" collection) in a vertical list of cards.
 * - Each card shows event title, date, category, location, and max spots.
 * - Provides quick navigation:
 *      - "Create Event" → opens CreateEventActivity for new event creation.
 *      - "Profile" → opens OrganizerProfileActivity.
 *      - "Alerts" → opens OrganizerNotificationsActivity.
 *      - "Home" → reloads OrganizerActivity.
 * - Uses dynamic layout inflation (item_event_card.xml) to build event cards at runtime.
 * - Displays user feedback via Toast messages for empty results or Firestore errors.
 */


package com.example.aurora;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class OrganizerActivity extends AppCompatActivity {

    private Button myEventsButton, createEventButton;
    private LinearLayout eventListContainer;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

        myEventsButton = findViewById(R.id.myEventsButton);
        createEventButton = findViewById(R.id.createEventButton);
        eventListContainer = findViewById(R.id.eventListContainer);

        db = FirebaseFirestore.getInstance();

        myEventsButton.setOnClickListener(v ->
                Toast.makeText(this, "My Events clicked", Toast.LENGTH_SHORT).show());

        createEventButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerActivity.this, CreateEventActivity.class);
            startActivity(intent);
        });

        TextView bottomHome = findViewById(R.id.bottomHome);
        TextView bottomProfile = findViewById(R.id.bottomProfile);
        TextView bottomAlerts = findViewById(R.id.bottomAlerts);

        bottomHome.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerActivity.this, OrganizerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        bottomProfile.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerActivity.this, OrganizerProfileActivity.class);
            startActivity(intent);
        });

        bottomAlerts.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerActivity.this, OrganizerNotificationsActivity.class);
            startActivity(intent);
        });

        loadEventsFromFirebase();
    }

    private void loadEventsFromFirebase() {
        eventListContainer.removeAllViews();
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No events found.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        addEventCard(doc);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addEventCard(DocumentSnapshot doc) {
        View eventView = LayoutInflater.from(this)
                .inflate(R.layout.item_event_card, eventListContainer, false);

        TextView title = eventView.findViewById(R.id.eventTitle);
        TextView date = eventView.findViewById(R.id.eventDate);
        TextView stats = eventView.findViewById(R.id.eventStats);
        TextView status = eventView.findViewById(R.id.eventStatus);

        String titleText = doc.getString("title");
        String dateText = doc.getString("date");
        Long maxSpots = doc.getLong("maxSpots");
        String location = doc.getString("location");
        String category = doc.getString("category");

        if (titleText == null) titleText = "Untitled Event";
        if (dateText == null) dateText = "Date not set";
        if (maxSpots == null) maxSpots = 0L;
        if (category == null) category = "";
        if (location == null) location = "";

        title.setText(titleText);
        date.setText(dateText);
        stats.setText("Max spots: " + maxSpots);

        String statusText = "";
        if (!category.isEmpty()) statusText += "🎨 " + category;
        if (!location.isEmpty()) {
            if (!statusText.isEmpty()) statusText += " • ";
            statusText += location;
        }
        if (statusText.isEmpty()) statusText = "📍 Location not set";

        status.setText(statusText);

        eventListContainer.addView(eventView);
    }
}
