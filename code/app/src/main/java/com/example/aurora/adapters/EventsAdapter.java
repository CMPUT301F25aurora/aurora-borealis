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

package com.example.aurora.adapters;

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

import com.example.aurora.map.JoinLocation;
import com.example.aurora.utils.LocationUtils;
import com.example.aurora.R;
import com.example.aurora.activities.EventDetailsActivity;
import com.example.aurora.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import com.bumptech.glide.Glide;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private final Context context;
    private final List<Event> events;
    private final FirebaseFirestore db;
    private final String userKey;

    public EventsAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
        this.db = FirebaseFirestore.getInstance();

        String email = context.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE)
                .getString("user_email", null);

        if ((email == null || email.isEmpty())
                && FirebaseAuth.getInstance().getCurrentUser() != null) {
            email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

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

        if (e.getWaitingList().contains(userKey)) {
            holder.btnJoin.setText("Waiting List Joined");
        }

        String posterUrl = e.getPosterUrl();
        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(context)
                    .load(posterUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.eventImage);
        } else {
            holder.eventImage.setImageResource(R.drawable.ic_launcher_background);
        }

        holder.btnViewDetails.setOnClickListener(v -> {
            Intent i = new Intent(context, EventDetailsActivity.class);
            i.putExtra("eventId", e.getEventId());
            context.startActivity(i);
        });

        holder.btnJoin.setOnClickListener(v -> {
            joinWaitingList(e, holder.btnJoin);
        });
    }

    private void joinWaitingList(Event e, Button button) {

        String eventId = e.getEventId();
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(context, "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {

                    Boolean geoRequired = doc.getBoolean("geoRequired");
                    if (geoRequired == null) geoRequired = false;

                    // ===========================
                    // LOCATION PERMISSION CHECK
                    // ===========================
                    if (geoRequired) {
                        if (!LocationUtils.isLocationPermissionGranted(context)) {
                            Toast.makeText(context, "This event requires location to join.", Toast.LENGTH_LONG).show();
                            LocationUtils.requestLocationPermission(context);
                            return;
                        }

                        // ===========================
                        // GPS MUST BE ENABLED
                        // ===========================
                        if (!LocationUtils.isGpsEnabled(context)) {
                            Toast.makeText(context,
                                    "Please enable GPS to join this event.",
                                    Toast.LENGTH_LONG).show();

                            context.startActivity(
                                    new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            );
                            return;
                        }
                    }

                    if (!doc.exists()) {
                        Toast.makeText(context, "Event no longer exists", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Long maxSpots = doc.getLong("maxSpots");
                    List<String> list = (List<String>) doc.get("waitingList");
                    if (list == null) list = new ArrayList<>();

                    if (maxSpots != null && list.size() >= maxSpots) {
                        Toast.makeText(context, "Waiting list full", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (list.contains(userKey)) {
                        Toast.makeText(context, "Already joined", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ===========================
                    // FETCH LOCATION (BLOCK IF NAN)
                    // ===========================
                    LocationUtils.getUserLocation(context, (lat, lng) -> {

                        // BLOCK if no real location
                        if (Double.isNaN(lat) || Double.isNaN(lng)) {
                            Toast.makeText(context,
                                    "Unable to get your location. Please ensure GPS is ON.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // SAVE LOCATION THEN JOIN
                        db.collection("events")
                                .document(eventId)
                                .collection("waitingLocations")
                                .add(new JoinLocation(userKey, lat, lng))
                                .addOnSuccessListener(s -> {

                                    db.collection("events")
                                            .document(eventId)
                                            .update("waitingList", FieldValue.arrayUnion(userKey))
                                            .addOnSuccessListener(unused -> {
                                                button.setText("Waiting List Joined");
                                                Toast.makeText(context, "Joined waiting list", Toast.LENGTH_SHORT).show();
                                            });
                                });
                    });
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
