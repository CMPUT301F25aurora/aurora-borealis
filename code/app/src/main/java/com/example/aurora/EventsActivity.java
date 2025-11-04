package com.example.aurora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class EventsActivity extends AppCompatActivity {

    private EditText searchEvents;
    private Button logoutButton;
    private RecyclerView recyclerEvents;
    private EventsAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Button btnAll, btnMusic, btnSports, btnEducation, btnArts, btnTechnology;
    private Button navEvents, navProfile, navAlerts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        searchEvents = findViewById(R.id.searchEvents);
        logoutButton = findViewById(R.id.logoutButton);
        recyclerEvents = findViewById(R.id.recyclerEvents);

        btnAll = findViewById(R.id.btnAll);
        btnMusic = findViewById(R.id.btnMusic);
        btnSports = findViewById(R.id.btnSports);
        btnEducation = findViewById(R.id.btnEducation);
        btnArts = findViewById(R.id.btnArts);
        btnTechnology = findViewById(R.id.btnTechnology);

        navEvents = findViewById(R.id.navEvents);
        navProfile = findViewById(R.id.navProfile);
        navAlerts = findViewById(R.id.navAlerts);

        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventsAdapter(this, eventList);
        recyclerEvents.setAdapter(adapter);

        logoutButton.setOnClickListener(v -> {
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnAll.setOnClickListener(v -> loadEvents(null));
        btnMusic.setOnClickListener(v -> loadEvents("Music"));
        btnSports.setOnClickListener(v -> loadEvents("Sports"));
        btnEducation.setOnClickListener(v -> loadEvents("Education"));
        btnArts.setOnClickListener(v -> loadEvents("Arts"));
        btnTechnology.setOnClickListener(v -> loadEvents("Technology"));

        navEvents.setOnClickListener(v -> {});
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        navAlerts.setOnClickListener(v -> startActivity(new Intent(this, AlertsActivity.class)));

        loadEvents();
    }

    private void loadEvents() {
        loadEvents(null);
    }

    private void loadEvents(String category) {
        Query q = db.collection("events");
        if (category != null) q = q.whereEqualTo("category", category);
        q.get()
                .addOnSuccessListener(query ->{
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        eventList.add(event);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show());
    }
}
