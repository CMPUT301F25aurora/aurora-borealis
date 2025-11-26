package com.example.aurora.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aurora.R;
import com.example.aurora.activities.OrganizerEntrantsActivity;
import com.example.aurora.map.EventMapActivity;
import com.example.aurora.models.Event;
import com.example.aurora.models.NotificationModel;
import com.example.aurora.notifications.FirestoreNotificationHelper;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrganizerEventsAdapter extends RecyclerView.Adapter<OrganizerEventsAdapter.EventViewHolder> {

    private final Context context;
    private final List<Event> events;
    private final FirebaseFirestore db;

    public OrganizerEventsAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder h, int pos) {
        Event e = events.get(pos);

        String eventId = e.getEventId();

        h.title.setText(e.getTitle());
        h.date.setText(e.getDate() != null ? e.getDate() : "Date not set");
        h.stats.setText("Max spots: " + (e.getMaxSpots() != null ? e.getMaxSpots() : 0));

        // Category Emoji
        String emoji = "📍";
        if (e.getCategory() != null) {
            switch (e.getCategory().toLowerCase()) {
                case "arts": emoji = "🎨"; break;
                case "sports": emoji = "⚽"; break;
                case "music": emoji = "🎵"; break;
                case "technology": emoji = "💻"; break;
                case "education": emoji = "📚"; break;
            }
        }

        String status = emoji + " " + e.getCategory();
        if (e.getLocation() != null && !e.getLocation().isEmpty()) {
            status += " • " + e.getLocation();
        }
        h.status.setText(status);

        // Poster
        if (e.getPosterUrl() != null && !e.getPosterUrl().isEmpty()) {
            Glide.with(context)
                    .load(e.getPosterUrl())
                    .into(h.eventImage);
        } else {
            h.eventImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // QR Button
        h.btnShowQR.setOnClickListener(v -> {
            if (e.getDeepLink() == null || e.getDeepLink().isEmpty()) {
                Toast.makeText(context, "No QR saved", Toast.LENGTH_SHORT).show();
            } else {
                showQrPopup(e.getDeepLink());
            }
        });

        // Manage Button
        h.btnManage.setOnClickListener(v -> {
            Intent i = new Intent(context, OrganizerEntrantsActivity.class);
            i.putExtra("eventId", eventId);
            context.startActivity(i);
        });

        // Map Button
        h.btnMap.setOnClickListener(v -> {
            Intent i = new Intent(context, EventMapActivity.class);
            i.putExtra("eventId", eventId);
            context.startActivity(i);
        });

        // Lottery Button
        h.btnLottery.setOnClickListener(v -> runLottery(eventId));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    // ---------------------
    // QR POPUP
    // ---------------------
    private void showQrPopup(String deepLink) {
        try {
            int size = 900;
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(deepLink, BarcodeFormat.QR_CODE, size, size);

            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);

            for (int x = 0; x < size; x++)
                for (int y = 0; y < size; y++)
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);

            ImageView qrView = new ImageView(context);
            qrView.setImageBitmap(bitmap);
            qrView.setPadding(40, 40, 40, 40);

            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("🎟️ Event QR Code")
                    .setView(qrView)
                    .setPositiveButton("Close", null)
                    .show();

        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(context, "Failed to generate QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void runLottery(String eventId) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {

                    List<String> waiting = (List<String>) doc.get("waitingList");
                    if (waiting == null || waiting.isEmpty()) {
                        Toast.makeText(context, "Waiting list empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> emailsOnly = new ArrayList<>();
                    for (String w : waiting) if (w.contains("@")) emailsOnly.add(w);

                    Collections.shuffle(emailsOnly);

                    int n = 1; // CHANGE IF YOU WANT ASK DIALOG
                    if (n > emailsOnly.size()) {
                        Toast.makeText(context, "Not enough entrants.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> winners = emailsOnly.subList(0, n);

                    db.collection("events").document(eventId)
                            .update(
                                    "selectedEntrants", winners,
                                    "waitingList", FieldValue.arrayRemove(winners.toArray())
                            );

                    Toast.makeText(context, "Lottery complete!", Toast.LENGTH_SHORT).show();
                });
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, stats, status;
        Button btnShowQR, btnManage, btnLottery, btnMap;
        ImageView eventImage;

        public EventViewHolder(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.eventTitle);
            date = v.findViewById(R.id.eventDate);
            stats = v.findViewById(R.id.eventStats);
            status = v.findViewById(R.id.eventStatus);

            btnShowQR = v.findViewById(R.id.btnShowQR);
            btnManage = v.findViewById(R.id.btnManage);
            btnLottery = v.findViewById(R.id.btnLottery);
            btnMap = v.findViewById(R.id.btnMap);

            eventImage = v.findViewById(R.id.eventImage);
        }
    }
}
