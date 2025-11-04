package com.example.aurora;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    private EditText eventName, eventDescription, eventStart, eventEnd,
            registrationStart, registrationEnd, maxCapacity;
    private Button choosePosterButton, createEventButton;
    private ImageView imagePreview;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        eventName = findViewById(R.id.eventName);
        eventDescription = findViewById(R.id.eventDescription);
        eventStart = findViewById(R.id.eventStartDate);
        eventEnd = findViewById(R.id.eventEndDate);
        registrationStart = findViewById(R.id.registrationStart);
        registrationEnd = findViewById(R.id.registrationEnd);
        maxCapacity = findViewById(R.id.maxCapacity);
        choosePosterButton = findViewById(R.id.choosePosterButton);
        imagePreview = findViewById(R.id.imagePreview);
        createEventButton = findViewById(R.id.createEventButton);

        createEventButton.setOnClickListener(v -> uploadEvent());
    }

    private void uploadEvent() {
        String name = eventName.getText().toString().trim();
        String description = eventDescription.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty()) {
            Toast.makeText(this,
                    "Please enter event name and description",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        saveEventData();
    }

    private void saveEventData() {
        String name = eventName.getText().toString().trim();
        String description = eventDescription.getText().toString().trim();
        String start = eventStart.getText().toString().trim();
        String end = eventEnd.getText().toString().trim();
        String regStart = registrationStart.getText().toString().trim();
        String regEnd = registrationEnd.getText().toString().trim();
        String capacityStr = maxCapacity.getText().toString().trim();

        long maxSpots = 0L;
        try {
            if (!capacityStr.isEmpty()) {
                maxSpots = Long.parseLong(capacityStr);
            }
        } catch (NumberFormatException ignored) {}

        Map<String, Object> event = new HashMap<>();

        // your original schema
        event.put("name", name);
        event.put("description", description);
        event.put("startDate", start);
        event.put("endDate", end);
        event.put("registrationStart", regStart);
        event.put("registrationEnd", regEnd);
        event.put("capacity", capacityStr);

        // extra fields your friend's dashboard expects
        event.put("title", name);          // display title
        event.put("date", start);          // simple date string
        event.put("maxSpots", maxSpots);   // numeric

        // optional placeholders so his emoji/location logic doesn't crash
        event.put("location", "");
        event.put("category", "");

        db.collection("events")
                .add(event)
                .addOnSuccessListener(ref ->
                        Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}
