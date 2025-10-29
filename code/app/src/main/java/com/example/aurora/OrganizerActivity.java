package com.example.aurora;

import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;


import java.util.HashMap;
import java.util.Map;

public class OrganizerActivity extends AppCompatActivity {


    private EditText eventName, eventDescription, eventStart, eventEnd,
            registrationStart, registrationEnd, maxCapacity;
    private Button choosePosterButton, createEventButton;
    private ImageView imagePreview;
    private ListView listMyEvents;






    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

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


      //upload event
        createEventButton.setOnClickListener(v -> uploadEvent());
    }

   


// upload event if info is filled out
    private void uploadEvent() {
        String name = eventName.getText().toString().trim();
        String description = eventDescription.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please enter event name and description", Toast.LENGTH_SHORT).show();
            return;
        }

        // save the event info to Firestore
        saveEventData();



    }
    // save event details
    private void saveEventData() {

        //create hashmap to store info about event
        Map<String, Object> event = new HashMap<>();
        event.put("name", eventName.getText().toString());
        event.put("description", eventDescription.getText().toString());
        event.put("startDate", eventStart.getText().toString());
        event.put("endDate", eventEnd.getText().toString());
        event.put("registrationStart", registrationStart.getText().toString());
        event.put("registrationEnd", registrationEnd.getText().toString());
        event.put("capacity", maxCapacity.getText().toString());

    // add event to firestore

        db.collection("events")
                .add(event)
                .addOnSuccessListener(ref -> Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
