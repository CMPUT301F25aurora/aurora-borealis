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

        // Top buttons
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

        // Bottom navigation
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
                        Toast.makeText(this,
                                "Error loading events: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void addEventCard(DocumentSnapshot doc) {
        View eventView = LayoutInflater.from(this)
                .inflate(R.layout.item_event_card, eventListContainer, false);

        TextView title = eventView.findViewById(R.id.eventTitle);
        TextView date = eventView.findViewById(R.id.eventDate);
        TextView stats = eventView.findViewById(R.id.eventStats);
        TextView status = eventView.findViewById(R.id.eventStatus);

        // Support both your old and new field names
        String titleText = doc.getString("title");
        if (titleText == null) titleText = doc.getString("name");

        String dateText = doc.getString("date");
        if (dateText == null) dateText = doc.getString("startDate");

        Long maxSpots = doc.getLong("maxSpots");
        if (maxSpots == null) {
            String capStr = doc.getString("capacity");
            try {
                maxSpots = (capStr == null || capStr.isEmpty())
                        ? 0L
                        : Long.parseLong(capStr);
            } catch (NumberFormatException e) {
                maxSpots = 0L;
            }
        }

        String location = doc.getString("location");
        String category = doc.getString("category");

        if (titleText == null) titleText = "Untitled Event";
        if (dateText == null) dateText = "Date not set";
        if (maxSpots == null) maxSpots = 0L;
        if (location == null) location = "";
        if (category == null) category = "";

        title.setText(titleText);
        date.setText(dateText);
        stats.setText("Max spots: " + maxSpots);

        // Emoji based on category
        String emoji = "📍";
        String lowerCategory = category.toLowerCase();

        if (lowerCategory.equals("arts")) {
            emoji = "🎨";
        } else if (lowerCategory.equals("sport") || lowerCategory.equals("sports")) {
            emoji = "⚽";
        } else if (lowerCategory.equals("music")) {
            emoji = "🎵";
        } else if (lowerCategory.equals("technology")) {
            emoji = "💻";
        } else if (lowerCategory.equals("education")) {
            emoji = "📚";
        }

        String statusText = emoji + " " + capitalize(category);
        if (!location.isEmpty()) statusText += " • " + location;

        status.setText(statusText);

        eventListContainer.addView(eventView);
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
