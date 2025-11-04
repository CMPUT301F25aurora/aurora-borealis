package com.example.aurora;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
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
            Toast.makeText(this, "Please enter event name and description", Toast.LENGTH_SHORT).show();
            return;
        }

        saveEventData();
    }

    private void saveEventData() {
        String name = eventName.getText().toString().trim();

        Map<String, Object> event = new HashMap<>();
        event.put("title", name); // use "title" to match your other events
        event.put("description", eventDescription.getText().toString());
        event.put("startDate", eventStart.getText().toString());
        event.put("endDate", eventEnd.getText().toString());
        event.put("registrationStart", registrationStart.getText().toString());
        event.put("registrationEnd", registrationEnd.getText().toString());
        event.put("capacity", maxCapacity.getText().toString());
        // optional starter waitingList array
        event.put("waitingList", new java.util.ArrayList<String>());

        db.collection("events")
                .add(event)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show();

                    // 🔹 LOG: event created
                    Map<String, Object> log = new HashMap<>();
                    log.put("type", "event_created");
                    log.put("message", "Event created: " + name);
                    log.put("timestamp", FieldValue.serverTimestamp());
                    log.put("eventId", ref.getId());
                    log.put("eventTitle", name);

                    db.collection("logs").add(log);

                    finish(); // go back if you want
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
