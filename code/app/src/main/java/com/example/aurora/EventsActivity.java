package com.example.aurora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.*;
import java.util.*;

public class EventsActivity extends AppCompatActivity {

    private EditText searchEvents;
    private Button logoutButton;
    private RecyclerView recyclerEvents;
    private EventsAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        searchEvents = findViewById(R.id.searchEvents);
        logoutButton = findViewById(R.id.logoutButton);
        recyclerEvents = findViewById(R.id.recyclerEvents);

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

        loadEvents();
    }

    private void loadEvents() {
        db.collection("events")
                .get()
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
