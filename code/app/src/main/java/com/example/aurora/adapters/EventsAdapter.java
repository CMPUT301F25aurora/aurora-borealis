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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aurora.R;
import com.example.aurora.activities.EventDetailsActivity;
import com.example.aurora.map.JoinLocation;
import com.example.aurora.models.Event;
import com.example.aurora.utils.LocationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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

    // ============================================================
    // USER CURRENT STATUS
    // ============================================================
    // USER CURRENT STATUS
// ============================================================
    private String getUserStatus(Event e) {

        if (e.getFinalEntrants() != null && e.getFinalEntrants().contains(userKey)) {
            return "final";
        }
        if (e.getAcceptedEntrants() != null && e.getAcceptedEntrants().contains(userKey)) {
            return "accepted";
        }
        if (e.getSelectedEntrants() != null && e.getSelectedEntrants().contains(userKey)) {
            return "selected";
        }
        if (e.getCancelledEntrants() != null && e.getCancelledEntrants().contains(userKey)) {
            return "cancelled";
        }
        if (e.getWaitingList() != null && e.getWaitingList().contains(userKey)) {
            return "waiting";
        }

        return "none";
    }


    // ============================================================
    // UPDATE BUTTON UI
    // ============================================================
    private void updateJoinButton(Button btn, String status) {

        switch (status) {

            case "waiting":
                btn.setText("Leave Waiting List");
                btn.setEnabled(true);
                break;

            case "selected":
                btn.setText("Selected – Pending");
                btn.setEnabled(false);
                break;

            case "cancelled":
                btn.setText("Declined Invitation");
                btn.setEnabled(false);
                break;

            case "final":
                btn.setText("Selected!");
                btn.setEnabled(false);
                break;

            case "accepted":
                btn.setText("Sign Up");
                btn.setEnabled(true);
                break;

            default: // none
                btn.setText("Join List");
                btn.setEnabled(true);
                break;
        }
    }


    private void joinWaitingList(Event e, Button button) {

        String eventId = e.getEventId();

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {

                    Boolean geoRequired = doc.getBoolean("geoRequired");
                    if (geoRequired == null) geoRequired = false;

                    List<String> list = (List<String>) doc.get("waitingList");
                    if (list == null) list = new ArrayList<>();

                    Long maxSpots = doc.getLong("maxSpots");
                    if (maxSpots != null && list.size() >= maxSpots) {
                        Toast.makeText(context, "Waiting list full", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (list.contains(userKey)) {
                        Toast.makeText(context, "Already joined", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ============================================================
                    // CASE A: geoRequired = FALSE
                    // ============================================================
                    if (!geoRequired) {

                        boolean hasPermission = LocationUtils.isLocationPermissionGranted(context);
                        boolean gpsOn = LocationUtils.isGpsEnabled(context);

                        // ---- SUBCASE A1: GPS OFF OR NO PERMISSION ----
                        // join list WITHOUT storing a location
                        if (!hasPermission || !gpsOn) {

                            db.collection("events")
                                    .document(eventId)
                                    .update("waitingList", FieldValue.arrayUnion(userKey))
                                    .addOnSuccessListener(v -> {
                                        Toast.makeText(context, "Joined waiting list", Toast.LENGTH_SHORT).show();
                                        updateJoinButton(button, "waiting");
                                    });

                            return;
                        }

                        // ---- SUBCASE A2: GPS ON + permission granted ----
                        // join normally AND store location
                        LocationUtils.getUserLocation(context, (lat, lng) -> {

                            if (Double.isNaN(lat) || Double.isNaN(lng)) {
                                // fallback: join without location
                                db.collection("events")
                                        .document(eventId)
                                        .update("waitingList", FieldValue.arrayUnion(userKey))
                                        .addOnSuccessListener(v -> {
                                            Toast.makeText(context, "Joined waiting list", Toast.LENGTH_SHORT).show();
                                            updateJoinButton(button, "waiting");
                                        });
                                return;
                            }

                            // Save location
                            db.collection("events")
                                    .document(eventId)
                                    .collection("waitingLocations")
                                    .add(new JoinLocation(userKey, lat, lng))
                                    .addOnSuccessListener(s -> {

                                        db.collection("events")
                                                .document(eventId)
                                                .update("waitingList", FieldValue.arrayUnion(userKey))
                                                .addOnSuccessListener(x -> {
                                                    Toast.makeText(context, "Joined waiting list", Toast.LENGTH_SHORT).show();
                                                    updateJoinButton(button, "waiting");
                                                });
                                    });
                        });

                        return;
                    }

                    // ============================================================
                    // CASE B: geoRequired = TRUE
                    // ============================================================

                    boolean hasPermission = LocationUtils.isLocationPermissionGranted(context);
                    boolean gpsOn = LocationUtils.isGpsEnabled(context);

                    // ---- SUBCASE B1: missing permission or GPS ----
                    if (!hasPermission) {
                        Toast.makeText(context, "This event requires location to join.", Toast.LENGTH_LONG).show();
                        LocationUtils.requestLocationPermission(context);
                        return;
                    }

                    if (!gpsOn) {
                        Toast.makeText(context,
                                "Please enable GPS to join this event.",
                                Toast.LENGTH_LONG).show();

                        context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        return;
                    }

                    // ---- SUBCASE B2: everything ON → fetch location ----
                    LocationUtils.getUserLocation(context, (lat, lng) -> {

                        if (Double.isNaN(lat) || Double.isNaN(lng)) {
                            Toast.makeText(context, "Unable to fetch location. Ensure GPS is ON.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Save location
                        db.collection("events")
                                .document(eventId)
                                .collection("waitingLocations")
                                .add(new JoinLocation(userKey, lat, lng))
                                .addOnSuccessListener(s -> {

                                    db.collection("events")
                                            .document(eventId)
                                            .update("waitingList", FieldValue.arrayUnion(userKey))
                                            .addOnSuccessListener(x -> {
                                                Toast.makeText(context, "Joined waiting list", Toast.LENGTH_SHORT).show();
                                                updateJoinButton(button, "waiting");
                                            });
                                });
                    });
                });
    }



    // ============================================================
    // LEAVE WAITING LIST
    // ============================================================
    private void leaveWaitingList(Event e, Button button) {

        String eventId = e.getEventId();

        db.collection("events")
                .document(eventId)
                .update("waitingList", FieldValue.arrayRemove(userKey))
                .addOnSuccessListener(unused -> {

                    // REMOVE LOCATION ENTRY
                    db.collection("events")
                            .document(eventId)
                            .collection("waitingLocations")
                            .whereEqualTo("userKey", userKey)
                            .get()
                            .addOnSuccessListener(snap -> {

                                for (var d : snap.getDocuments()) {
                                    d.getReference().delete();
                                }

                                Toast.makeText(context, "Left waiting list", Toast.LENGTH_SHORT).show();
                                updateJoinButton(button, "none");
                            });
                });
    }

    // SIGN UP FROM PREVIEW (accepted -> final)
    private void signUpFromPreview(Event e, Button button) {

        String eventId = e.getEventId();

        db.collection("events")
                .document(eventId)
                .update(
                        "finalEntrants", FieldValue.arrayUnion(userKey),
                        "acceptedEntrants", FieldValue.arrayRemove(userKey)
                )
                .addOnSuccessListener(unused -> {
                    Toast.makeText(context, "You are signed up!", Toast.LENGTH_SHORT).show();
                    updateJoinButton(button, "final");
                })
                .addOnFailureListener(err -> {
                    Toast.makeText(context, "Failed to sign up", Toast.LENGTH_SHORT).show();
                });
    }


    // ============================================================
    // BIND VIEW HOLDER
    // ============================================================
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {

        Event e = events.get(position);

        holder.eventTitle.setText(e.getTitle());
        holder.eventDate.setText(e.getDate());
        holder.eventLocation.setText(e.getLocation());

        // LOAD POSTER
        if (e.getPosterUrl() != null && !e.getPosterUrl().isEmpty()) {
            Glide.with(context)
                    .load(e.getPosterUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.eventImage);
        } else {
            holder.eventImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // LIVE STATUS UPDATES
        db.collection("events")
                .document(e.getEventId())
                .addSnapshotListener((doc, error) -> {

                    if (doc == null || !doc.exists()) return;

                    e.setWaitingList((List<String>) doc.get("waitingList"));
                    e.setSelectedEntrants((List<String>) doc.get("selectedEntrants"));
                    e.setCancelledEntrants((List<String>) doc.get("cancelledEntrants"));
                    e.setAcceptedEntrants((List<String>) doc.get("acceptedEntrants"));
                    e.setFinalEntrants((List<String>) doc.get("finalEntrants"));

                    updateJoinButton(holder.btnJoin, getUserStatus(e));
                });

        // BUTTON ACTION
        // BUTTON ACTION
        // BUTTON ACTION
        holder.btnJoin.setOnClickListener(v -> {

            String status = getUserStatus(e);

            if (status.equals("waiting")) {
                leaveWaitingList(e, holder.btnJoin);
            } else if (status.equals("none")) {
                joinWaitingList(e, holder.btnJoin);
            } else if (status.equals("accepted")) {
                signUpFromPreview(e, holder.btnJoin);
            }
        });


        // DETAILS BUTTON
        holder.btnViewDetails.setOnClickListener(v -> {
            Intent i = new Intent(context, EventDetailsActivity.class);
            i.putExtra("eventId", e.getEventId());
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    // ============================================================
    // VIEW HOLDER
    // ============================================================
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
