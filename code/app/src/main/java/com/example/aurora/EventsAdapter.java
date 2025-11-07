package com.example.aurora;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows event cards for entrants.
 *
 * - Title, date, location from Event model.
 * - "View Details" opens EventDetailsActivity with eventId.
 * - "Join Waiting List" writes the device's uid into events/{id}.waitingList.
 * - Optionally enforces a max size from events/{id}.maxSpots (if that field exists).
 */
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private final Context context;
    private final List<Event> events;
    private final FirebaseFirestore db;
    private final String uid;  // simple stable id per device

    public EventsAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
        this.db = FirebaseFirestore.getInstance();
        this.uid = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event e = events.get(position);

        String title = e.getTitle() != null ? e.getTitle() : "Untitled Event";
        String date = e.getDate() != null ? e.getDate() : "";
        String location = e.getLocation() != null ? e.getLocation() : "";

        holder.eventTitle.setText(title);
        holder.eventDate.setText(date);
        holder.eventLocation.setText(location);

        // Open details
        holder.btnViewDetails.setOnClickListener(v -> {
            Intent i = new Intent(context, EventDetailsActivity.class);
            i.putExtra("eventId", e.getEventId());
            context.startActivity(i);
        });

        // Join waiting list
        holder.btnJoin.setOnClickListener(v -> {
            String eventId = e.getEventId();
            if (eventId == null || eventId.isEmpty()) {
                Toast.makeText(context, "Missing event ID", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("events")
                    .document(eventId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            Toast.makeText(context, "Event no longer exists", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Optional cap: events/{id}.maxSpots (Long)
                        Long maxSpots = doc.getLong("maxSpots");

                        @SuppressWarnings("unchecked")
                        List<String> waitingList = (List<String>) doc.get("waitingList");
                        if (waitingList == null) waitingList = new ArrayList<>();

                        // If limit is set and list is full
                        if (maxSpots != null && waitingList.size() >= maxSpots) {
                            Toast.makeText(context, "Waiting list is full", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (waitingList.contains(uid)) {
                            Toast.makeText(context, "You're already on the waiting list", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        db.collection("events")
                                .document(eventId)
                                .update("waitingList", FieldValue.arrayUnion(uid))
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(context, "Joined waiting list", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(ex ->
                                        Toast.makeText(context, "Failed to join: " + ex.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    })
                    .addOnFailureListener(ex ->
                            Toast.makeText(context, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {

        ImageView eventImage;
        TextView eventTitle, eventDate, eventLocation;
        Button btnViewDetails, btnJoin;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.eventImage);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventDate = itemView.findViewById(R.id.eventDate);
            eventLocation = itemView.findViewById(R.id.eventLocation);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnJoin = itemView.findViewById(R.id.btnJoin);
        }
    }
}
