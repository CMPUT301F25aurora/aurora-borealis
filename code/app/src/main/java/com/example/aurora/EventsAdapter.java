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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows event cards for entrants.
 *
 * - Title, date, location from Event model.
 * - "View Details" opens EventDetailsActivity with eventId.
 * - "Join Waiting List" writes a stable user key into events/{id}.waitingList.
 *   The key is:
 *       1) user email from SharedPreferences ("aurora_prefs" -> "user_email"), or
 *       2) FirebaseAuth currentUser.getEmail(), or
 *       3) ANDROID_ID (device id) as a final fallback.
 * - Optionally enforces a max size from events/{id}.maxSpots (if that field exists).
 */

/*
 * References:
 *
 * 1) Android Developers — "Create dynamic lists with RecyclerView"
 *    https://developer.android.com/develop/ui/views/layout/recyclerview
 *    Used as a reference for implementing RecyclerView.Adapter, ViewHolder, and binding data into item views.
 *
 * 2) author: Stack Overflow user — "Simple Android RecyclerView example"
 *    https://stackoverflow.com/questions/40584424/simple-android-recyclerview-example
 *    Used as a reference for inflating row layouts, holding view references, and handling item click callbacks.
 *
 * 3) Android Developers — "Settings.Secure"
 *    https://developer.android.com/reference/android/provider/Settings.Secure
 *    Used as a reference for using Settings.Secure.ANDROID_ID when joining events with a device-based identifier.
 */


public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private final Context context;
    private final List<Event> events;
    private final FirebaseFirestore db;
    /** Identifier stored in waitingList: email preferred, else device id. */
    private final String userKey;

    public EventsAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
        this.db = FirebaseFirestore.getInstance();

        // Try to resolve an email-based key first
        String email = context.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE)
                .getString("user_email", null);

        if ((email == null || email.isEmpty())
                && FirebaseAuth.getInstance().getCurrentUser() != null) {
            email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        // Fallback to device ID if no email available
        if (email == null || email.isEmpty()) {
            email = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );
        }

        this.userKey = email;
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

        // Open details screen
        holder.btnViewDetails.setOnClickListener(v -> {
            Intent i = new Intent(context, EventDetailsActivity.class);
            i.putExtra("eventId", e.getEventId());
            context.startActivity(i);
        });

        // Outside "Join Waiting List" button
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

                        if (waitingList.contains(userKey)) {
                            Toast.makeText(context, "You're already on the waiting list", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        db.collection("events")
                                .document(eventId)
                                .update("waitingList", FieldValue.arrayUnion(userKey))
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
