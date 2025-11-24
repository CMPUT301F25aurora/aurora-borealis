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
import com.bumptech.glide.Glide;


/**
 * Adapter responsible for displaying a list of events to entrants.
 * <p>
 * Each event card shows:
 * - Event title
 * - Date
 * - Location
 * - Poster image
 * - A "View Details" button that opens EventDetailsActivity
 * - A "Join Waiting List" button that registers the current user into the event's waiting list
 * <p>
 * A stable identifier (email or device ID fallback) is stored in the event's waitingList array field.
 */
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private final Context context;
    private final List<Event> events;
    private final FirebaseFirestore db;
    /** Identifier stored in waitingList: email preferred, else device id. */
    private final String userKey;

    /**
     * Constructs the adapter for displaying event cards.
     *
     * @param context application context
     * @param events list of Event objects to display
     */
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

    /**
     * Inflates the event card layout for each RecyclerView row.
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds event data into the view holder for a specific position.
     *
     * @param holder ViewHolder containing references to UI elements
     * @param position index of the event in the list
     */
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
                    .placeholder(R.drawable.ic_launcher_background)   // pick your placeholder
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.eventImage);
        } else {
            holder.eventImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // View Details
        holder.btnViewDetails.setOnClickListener(v -> {
            Intent i = new Intent(context, EventDetailsActivity.class);
            i.putExtra("eventId", e.getEventId());
            context.startActivity(i);
        });

        // Join Waiting List...
        holder.btnJoin.setOnClickListener(v -> {
            joinWaitingList(e, holder.btnJoin);
        });
    }


    /**
     * Attempts to add the current user to the event's waiting list.
     * <p>
     * Behavior:
     * - Prevents joining twice
     * - Checks maxSpots (if exists)
     * - Updates Firestore using FieldValue.arrayUnion
     * - Updates UI button text to "Waiting List Joined" after success
     *
     * @param e      the event being joined
     * @param button the UI button to update text on successful join
     */
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

                    button.setText("Waiting List Joined");

                    db.collection("events")
                            .document(eventId)
                            .update("waitingList", FieldValue.arrayUnion(userKey))
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(context, "Joined waiting list", Toast.LENGTH_SHORT).show()
                            );
                });
    }



    /**
     * @return total number of events displayed.
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder class that stores UI references for each event card.
     * Contains the poster image, title, date, location, and action buttons.
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {

        ImageView eventImage;
        TextView eventTitle, eventDate, eventLocation;
        Button btnViewDetails, btnJoin;

        /**
         * Initializes view references for an event card.
         *
         * @param itemView the inflated row view
         */
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
