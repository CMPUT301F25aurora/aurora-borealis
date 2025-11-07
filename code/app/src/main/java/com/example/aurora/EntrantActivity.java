/**
 * EntrantActivity.java
 *
 * This activity represents the main screen for entrants in the Aurora app.
 * It connects to Firestore to retrieve a list of available events that users can view
 * and register for. The events will be displayed in a RecyclerView using a linear layout.
 *
 * Future implementations will handle displaying event details, registration actions,
 * and viewing the entrant’s event history.
 */


package com.example.aurora;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;


import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EntrantActivity extends AppCompatActivity {


    private List<Event> eventList = new ArrayList<>();
    private FirebaseFirestore db;
}

