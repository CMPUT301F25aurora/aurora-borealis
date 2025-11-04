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

    private final Context context;
    private final List<Event> events;
    private final FirebaseFirestore db;
    private final String uid;

    public EventsAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
        this.db = FirebaseFirestore.getInstance();
        this.uid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event e = events.get(position);

        holder.eventTitle.setText(e.getTitle());
        holder.eventDate.setText(e.getDate());
        holder.eventLocation.setText(e.getLocation());

        // View Details button
        holder.btnViewDetails.setOnClickListener(v -> {
            Intent i = new Intent(context, EventDetailsActivity.class);
            i.putExtra("eventId", e.getEventId());
            context.startActivity(i);
        });

        // Join button
        holder.btnJoin.setOnClickListener(v ->
                db.collection("events").document(e.getEventId())
                        .update("waitingList", FieldValue.arrayUnion(uid))
                        .addOnSuccessListener(unused ->
                                Toast.makeText(context, "Joined waiting list", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(err ->
                                Toast.makeText(context, "Error: " + err.getMessage(), Toast.LENGTH_SHORT).show())
        );

        // QR button
        holder.btnShowQR.setOnClickListener(v -> {
            String deepLink = e.getDeepLink();
            if (deepLink == null || deepLink.isEmpty()) {
                Toast.makeText(context, "⚠️ No deepLink found for this event", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Generating QR for: " + deepLink, Toast.LENGTH_SHORT).show();
                showQrPopup(deepLink);
            }
        });
    }

    private void showQrPopup(String deepLink) {
        if (deepLink == null || deepLink.isEmpty()) {
            Toast.makeText(context, "No QR code available for this event", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Ensure we use an Activity context for the dialog
            Context activityContext = (context instanceof android.app.Activity)
                    ? context
                    : context.getApplicationContext();

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

            new AlertDialog.Builder(activityContext)
                    .setTitle("Event QR Code")
                    .setView(qrView)
                    .setPositiveButton("Close", (d, w) -> d.dismiss())
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventTitle, eventDate, eventLocation;
        Button btnViewDetails, btnJoin, btnShowQR;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventDate = itemView.findViewById(R.id.eventDate);
            eventLocation = itemView.findViewById(R.id.eventLocation);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnJoin = itemView.findViewById(R.id.btnJoin);
            btnShowQR = itemView.findViewById(R.id.btnShowQR);
        }
    }
}
