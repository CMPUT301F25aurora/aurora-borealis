/*
 * References for this screen:
 *
 * 1) source: Android Developers — "The activity lifecycle"
 *    https://developer.android.com/guide/components/activities/activity-lifecycle
 *    Used for handling onCreate / onResume when building the organizer home screen.
 *
 *
 * 2) author: Stack Overflow user — "How to start new activity on button click"
 *    https://stackoverflow.com/questions/4186021/how-to-start-new-activity-on-button-click
 *    Used for launching other organizer screens from buttons or cards.
 */


package com.example.aurora;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
/**
 * OrganizerActivity.java
 *
 * Main dashboard for organizers in the Aurora app.
 * - Displays all events from Firestore, marking which ones belong to the current organizer.
 * - Lets organizers create new events or manage existing ones.
 * - Shows event info such as title, date, capacity, category, and location.
 * - Generates and displays QR codes for event deep links.
 * - Provides navigation to profile and notifications pages, and supports logout.
 */

public class OrganizerActivity extends AppCompatActivity {

    private Button myEventsButton, createEventButton;
    private Button btnLogout;
    private ImageButton btnBack;
    private LinearLayout eventListContainer;

    private TextView bottomHome, bottomProfile, bottomAlerts;

    private FirebaseFirestore db;
    private String organizerEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

        db = FirebaseFirestore.getInstance();
        organizerEmail = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_email", null);

        bindViews();
        setupTopBar();
        setupTabs();
        setupBottomNav();

        loadEventsFromFirebase();
    }

    private void bindViews() {
        myEventsButton = findViewById(R.id.myEventsButton);
        createEventButton = findViewById(R.id.createEventButton);
        eventListContainer = findViewById(R.id.eventListContainer);

        btnLogout = findViewById(R.id.btnLogoutOrganizer);
        btnBack = findViewById(R.id.btnBackOrganizer);

        bottomHome = findViewById(R.id.bottomHome);
        bottomProfile = findViewById(R.id.bottomProfile);
        bottomAlerts = findViewById(R.id.bottomAlerts);
    }

    private void setupTopBar() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logoutUser());
        }
    }

    private void logoutUser() {

        FirebaseAuth.getInstance().signOut();

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
        sp.edit().clear().apply();

        Intent intent = new Intent(OrganizerActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void setupTabs() {
        myEventsButton.setOnClickListener(v -> {
            Toast.makeText(this, "Events", Toast.LENGTH_SHORT).show();
        });

        createEventButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerActivity.this, CreateEventActivity.class);
            startActivity(intent);
        });
    }

    private void setupBottomNav() {
        bottomHome.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerActivity.this, OrganizerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        bottomProfile.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerActivity.this, OrganizerProfileActivity.class);
            startActivity(intent);
        });

        bottomAlerts.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerActivity.this, OrganizerNotificationsActivity.class);
            startActivity(intent);
        });
    }

    private void loadEventsFromFirebase() {
        eventListContainer.removeAllViews();

        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No events found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        addEventCard(doc);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error loading events: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void addEventCard(DocumentSnapshot doc) {
        View eventView = LayoutInflater.from(this)
                .inflate(R.layout.item_event_card, eventListContainer, false);

        TextView title = eventView.findViewById(R.id.eventTitle);
        TextView date = eventView.findViewById(R.id.eventDate);
        TextView stats = eventView.findViewById(R.id.eventStats);
        TextView status = eventView.findViewById(R.id.eventStatus);
        Button btnShowQR = eventView.findViewById(R.id.btnShowQR);
        //Button btnManage = eventView.findViewById(R.id.btnManageEvent);

        String eventId = doc.getId();

        // Title
        String titleText = doc.getString("title");
        if (titleText == null) titleText = doc.getString("name");
        if (titleText == null) titleText = "Untitled Event";

        // Date
        String dateText = doc.getString("date");
        if (dateText == null) dateText = doc.getString("startDate");
        if (dateText == null) dateText = "Date not set";

        // Max spots
        Long maxSpots = doc.getLong("maxSpots");
        if (maxSpots == null) {
            Object capObj = doc.get("capacity");
            if (capObj instanceof Number) {
                maxSpots = ((Number) capObj).longValue();
            } else if (capObj instanceof String) {
                try {
                    maxSpots = Long.parseLong((String) capObj);
                } catch (NumberFormatException e) {
                    maxSpots = 0L;
                }
            } else {
                maxSpots = 0L;
            }
        }

        String location = doc.getString("location");
        if (location == null) location = "";
        String category = doc.getString("category");
        if (category == null) category = "";

        String deepLink = doc.getString("deepLink");
        String creatorEmail = doc.getString("organizerEmail");

        title.setText(titleText);
        date.setText(dateText);
        stats.setText("Max spots: " + maxSpots);


        String emoji = "📍";
        String lowerCategory = category.toLowerCase();

        if (lowerCategory.equals("arts")) {
            emoji = "🎨";
        } else if (lowerCategory.equals("sport") || lowerCategory.equals("sports")) {
            emoji = "⚽";
        } else if (lowerCategory.equals("music")) {
            emoji = "🎵";
        } else if (lowerCategory.equals("technology") || lowerCategory.equals("tech")) {
            emoji = "💻";
        } else if (lowerCategory.equals("education")) {
            emoji = "📚";
        }

        String statusText = emoji + " " + capitalize(category);
        if (!location.isEmpty()) statusText += " • " + location;

        // Mark which ones are mine
        boolean isMine = organizerEmail != null && organizerEmail.equalsIgnoreCase(creatorEmail);
        if (isMine) {
            statusText += " • My Event";
        }

        status.setText(statusText);

        // QR button
        btnShowQR.setOnClickListener(v -> {
            if (deepLink == null || deepLink.isEmpty()) {
                Toast.makeText(this,
                        "No QR link saved for this event yet",
                        Toast.LENGTH_SHORT).show();
            } else {
                showQrPopup(deepLink);
            }
        });



        eventListContainer.addView(eventView);
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private void showQrPopup(String deepLink) {
        try {
            int size = 800;
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(deepLink, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ImageView qrView = new ImageView(this);
            qrView.setImageBitmap(bitmap);
            qrView.setPadding(40, 40, 40, 40);

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("🎟️ Event QR Code")
                    .setView(qrView)
                    .setPositiveButton("Close", (d, w) -> d.dismiss())
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Failed to generate QR code",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
