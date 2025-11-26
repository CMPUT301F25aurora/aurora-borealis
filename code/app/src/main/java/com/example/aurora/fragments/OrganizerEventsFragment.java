package com.example.aurora.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aurora.R;
import com.example.aurora.activities.CreateEventActivity;
import com.example.aurora.adapters.OrganizerEventsAdapter;
import com.example.aurora.models.Event;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrganizerEventsFragment extends Fragment {

    private RecyclerView recycler;
    private OrganizerEventsAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private FirebaseFirestore db;
    private String organizerEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_organizer_events, container, false);

        db = FirebaseFirestore.getInstance();

        organizerEmail = requireActivity()
                .getSharedPreferences("aurora_prefs", requireContext().MODE_PRIVATE)
                .getString("user_email", null);

        recycler = v.findViewById(R.id.recyclerOrganizerEvents);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OrganizerEventsAdapter(requireContext(), eventList);
        recycler.setAdapter(adapter);

        loadEvents();

        v.findViewById(R.id.createEventButton)
                .setOnClickListener(x -> startActivity(new Intent(getContext(), CreateEventActivity.class)));

        return v;
    }

    private void loadEvents() {
        db.collection("events")
                .whereEqualTo("organizerEmail", organizerEmail)
                .get()
                .addOnSuccessListener(query -> {
                    eventList.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        Event e = doc.toObject(Event.class);
                        e.setEventId(doc.getId());
                        eventList.add(e);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load", Toast.LENGTH_SHORT).show());
    }
}
