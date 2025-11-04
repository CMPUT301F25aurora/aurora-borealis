/**
 * CreateEventActivity.java
 *
 * This activity allows organizers to create new events in the Aurora app.
 * It provides input fields for event details such as name, description, dates,
 * registration period, and maximum capacity. Users can also upload a poster image.
 *
 * When the "Create Event" button is clicked, the entered event information
 * is validated and then uploaded to the Firestore database under the "events" collection.
 * Successful uploads show a confirmation message, while errors display a toast with details.
 */


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

    private EditText eventName, eventDescription, eventStart, eventEnd, registrationStart, registrationEnd, maxCapacity, maxEntrantsEditText;
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
        maxEntrantsEditText = findViewById(R.id.maxEntrantsEditText);


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
        Map<String, Object> event = new HashMap<>();
        event.put("name", eventName.getText().toString());
        event.put("description", eventDescription.getText().toString());
        event.put("startDate", eventStart.getText().toString());
        event.put("endDate", eventEnd.getText().toString());
        event.put("registrationStart", registrationStart.getText().toString());
        event.put("registrationEnd", registrationEnd.getText().toString());
        event.put("capacity", maxCapacity.getText().toString());

        String limitText = maxEntrantsEditText.getText().toString().trim();
        Long maxEntrants = null;
        if (!limitText.isEmpty()) {
            try {
                maxEntrants = Long.parseLong(limitText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number for maximum entrants", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        event.put("maxEntrants", maxEntrants);

        db.collection("events")
                .add(event)
                .addOnSuccessListener(ref -> Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
