/**
 * EventsAdapter.java
 *
 * RecyclerView adapter that binds a list of Event objects to the event list UI.
 * - Displays each event’s title, date, location, and image placeholder.
 * - Handles “View Details” clicks to open EventDetailsActivity for the selected event.
 * - Handles “Join” clicks to add the current user (by device ANDROID_ID) to the event’s waiting list in Firestore.
 *
 * The adapter ensures smooth scrolling and efficient view reuse using the ViewHolder pattern.
 * It interacts directly with the "events" collection in Firestore to update waiting list data.
 */


package com.example.aurora;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private Context context;
    private List<Event> events;
    private FirebaseFirestore db;
    private String uid;
    private boolean isOrganizer;

    public EventsAdapter(Context context, List<Event> events, boolean isOrganizer) {
        this.context = context;
        this.events = events;
        this.db = FirebaseFirestore.getInstance();
        this.uid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        this.isOrganizer = isOrganizer; // ✅ now properly received
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use whichever layout your app currently uses
        int layoutId = isOrganizer ? R.layout.item_event_card : R.layout.item_event;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event e = events.get(position);

        // Defensive check (avoid NullPointerException)
        if (holder.eventTitle == null || holder.eventDate == null || holder.eventLocation == null) {
            Toast.makeText(context, "⚠️ Layout mismatch: missing TextView IDs in item_event_card.xml", Toast.LENGTH_SHORT).show();
            return;
        }

        holder.eventTitle.setText(e.getTitle() != null ? e.getTitle() : "Untitled Event");
        holder.eventDate.setText(e.getDate() != null ? e.getDate() : "Date not set");
        holder.eventLocation.setText(e.getLocation() != null ? e.getLocation() : "Location not set");

        // View Details button
        holder.btnViewDetails.setOnClickListener(v -> {
            Intent i = new Intent(context, EventDetailsActivity.class);
            i.putExtra("eventId", e.getEventId());
            context.startActivity(i);
        });

        // Join button (Entrant)
        if (holder.btnJoin != null) {
            holder.btnJoin.setOnClickListener(v ->
                    db.collection("events").document(e.getEventId())
                            .update("waitingList", FieldValue.arrayUnion(uid))
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(context, "Joined waiting list", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(err ->
                                    Toast.makeText(context, "Error: " + err.getMessage(), Toast.LENGTH_SHORT).show())
            );
        }

        // QR button (Organizer only, ignore if missing)
        if (holder.btnShowQR != null) {
            holder.btnShowQR.setOnClickListener(v -> {
                String deepLink = e.getDeepLink();
                if (deepLink == null || deepLink.isEmpty()) {
                    Toast.makeText(context, "⚠️ No QR code for this event", Toast.LENGTH_SHORT).show();
                } else {
                    showQrPopup(deepLink); // ✅ new method call
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    // QR popup generator (safe for both organizer and entrant)
    private void showQrPopup(String deepLink) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(deepLink, BarcodeFormat.QR_CODE, 800, 800);
            Bitmap bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.RGB_565);
            for (int x = 0; x < 800; x++) {
                for (int y = 0; y < 800; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ImageView qrView = new ImageView(context);
            qrView.setImageBitmap(bitmap);
            qrView.setPadding(40, 40, 40, 40);

            new AlertDialog.Builder(context)
                    .setTitle("🎟️ Event QR Code")
                    .setView(qrView)
                    .setPositiveButton("Close", (d, w) -> d.dismiss())
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    // ViewHolder
    public static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView eventImage;
        TextView eventTitle, eventDate, eventLocation;
        Button btnViewDetails, btnJoin, btnShowQR;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.eventImage);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventDate = itemView.findViewById(R.id.eventDate);
            eventLocation = itemView.findViewById(R.id.eventLocation);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnJoin = itemView.findViewById(R.id.btnJoin);
            btnShowQR = itemView.findViewById(R.id.btnShowQR); // optional, won't crash if missing
        }
    }
}
