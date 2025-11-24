package com.example.aurora;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity for entrants in the Aurora app.
 *
 * <p>Connects to Firestore to load a list of available events
 * that users can browse and register for. Future versions will
 * include event details, registration, and history views.</p>
 */

public class EntrantActivity extends AppCompatActivity {

    private List<Event> eventList = new ArrayList<>();
    private FirebaseFirestore db;
}
