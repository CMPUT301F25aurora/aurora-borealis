/**
 * EventsFragment.java
 *
 * Fragment that shows a scrollable list of events for entrants.
 * - Sets up a RecyclerView with EventsAdapter.
 * - Loads events from Firestore ("events" collection) and supports optional category filtering
 *   via whereEqualTo("category", ...). Category buttons (All/Music/Sports/Education/Arts/Technology)
 *   reload the list with the selected filter.
 * - Logout returns the user to LoginActivity and clears the back stack.
 *
 * Note: searchEvents is present in the layout; wire it to query filtering if needed.
 */


package com.example.aurora;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment {

    private EditText searchEvents;
    private Button logoutButton, btnAll, btnMusic, btnSports, btnEducation, btnArts, btnTechnology;
    private RecyclerView recyclerEvents;
    private EventsAdapter adapter;
    private final List<Event> eventList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        searchEvents = v.findViewById(R.id.searchEvents);
        logoutButton = v.findViewById(R.id.logoutButton);
        recyclerEvents = v.findViewById(R.id.recyclerEvents);
        btnAll = v.findViewById(R.id.btnAll);
        btnMusic = v.findViewById(R.id.btnMusic);
        btnSports = v.findViewById(R.id.btnSports);
        btnEducation = v.findViewById(R.id.btnEducation);
        btnArts = v.findViewById(R.id.btnArts);
        btnTechnology = v.findViewById(R.id.btnTechnology);

        recyclerEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EventsAdapter(requireContext(), eventList);
        recyclerEvents.setAdapter(adapter);

        logoutButton.setOnClickListener(x -> {
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(getContext(), LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            requireActivity().finish();
        });

        btnAll.setOnClickListener(x -> loadEvents(null));
        btnMusic.setOnClickListener(x -> loadEvents("Music"));
        btnSports.setOnClickListener(x -> loadEvents("Sports"));
        btnEducation.setOnClickListener(x -> loadEvents("Education"));
        btnArts.setOnClickListener(x -> loadEvents("Arts"));
        btnTechnology.setOnClickListener(x -> loadEvents("Technology"));
        loadEvents(null);
    }

    private void loadEvents(String category) {
        Query q = db.collection("events");
        if (category != null) q = q.whereEqualTo("category", category);
        q.get()
                .addOnSuccessListener(query -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        Event e = doc.toObject(Event.class);
                        e.setEventId(doc.getId());
                        eventList.add(e);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show());
    }
}
