/*
 * References for this screen:
 *
 * 1) author: Stack Overflow user — "How to generate a QR Code for an Android application?"
 *    https://stackoverflow.com/questions/8800919/how-to-generate-a-qr-code-for-an-android-application
 *    Used for the QRCodeWriter + BitMatrix pattern to draw a QR code Bitmap.
 *
 * 2) source: Firebase docs — "Get data with Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/query-data/get-data
 *    Used for reading a single event document and its fields from Firestore.
 *
 * 3) source: Firebase docs — "Perform simple and compound queries in Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/query-data/queries
 *    Used for loading related data such as entrants or logs tied to the event.
 *
 * 4) source: Android Developers — "Dialogs"
 *    https://developer.android.com/develop/ui/views/components/dialogs
 *    Used for showing an AlertDialog with a custom view around the QR code or actions.
 *
 * 5) source: Android Developers — "Settings.Secure"
 *    https://developer.android.com/reference/android/provider/Settings.Secure
 *    Used when reading a device identifier like ANDROID_ID to tag actions from this screen.
 *
 * 6) source: ChatGPT (OpenAI assistant)
 *    Used only to help tidy up wording in the Javadoc and choose some helper method names.
 */


package com.example.aurora;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.List;

/**
 * Shows details for a single event.
 * As an entrant you can:
 *  - See title, time, location, description, stats.
 *  - Join / leave the waiting list.
 *  - View selection criteria (dialog).
 *  - View the event's QR code (no scanning here).
 *
 * Waiting list entries are stored as the entrant's EMAIL when possible,
 * falling back to device ID only if no email is available.
 */


public class EventDetailsActivity extends AppCompatActivity {

    private ImageView imgBanner;
    private TextView txtJoinedBadge, txtTitle, txtSubtitle, txtTime,
            txtLocation, txtAbout, txtStats, txtRegWindow;
    private Button btnJoinLeave;
    private Button btnCriteria;
    private Button btnShowQr;
    private ImageButton btnBackEvent;

    private FirebaseFirestore db;
    private String eventId;
    /** Identifier stored in waitingList (email preferred, else device id). */
    private String userId;
    private boolean isJoined = false;
    private String currentDeepLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        db = FirebaseFirestore.getInstance();
        userId = resolveCurrentUserKey();   // <- EMAIL (with fallback)

        imgBanner = findViewById(R.id.imgBanner);
        txtJoinedBadge = findViewById(R.id.txtJoinedBadge);
        txtTitle = findViewById(R.id.txtTitle);
        txtSubtitle = findViewById(R.id.txtSubtitle);
        txtTime = findViewById(R.id.txtTime);
        txtLocation = findViewById(R.id.txtLocation);
        txtAbout = findViewById(R.id.txtAbout);
        txtStats = findViewById(R.id.txtStats);
        txtRegWindow = findViewById(R.id.txtRegWindow);
        btnJoinLeave = findViewById(R.id.btnJoinLeave);
        btnCriteria = findViewById(R.id.btnCriteria);
        btnShowQr = findViewById(R.id.btnShowQr);
        btnBackEvent = findViewById(R.id.btnBackEvent);

        // Back arrow
        btnBackEvent.setOnClickListener(v -> onBackPressed());

        // Get event ID from intent extra or deep link
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            eventId = DeepLinkUtil.extractEventIdFromIntent(getIntent());
        }

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "No event selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
        String role = sp.getString("user_role", null);
        if (role == null || role.isEmpty()) {
            // User not logged in — save event and redirect
            getSharedPreferences("aurora", MODE_PRIVATE)
                    .edit()
                    .putString("pending_event", eventId)
                    .apply();

            Intent i = new Intent(this, WelcomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return;
        }


        btnCriteria.setOnClickListener(v -> showCriteriaDialog());

        btnShowQr.setOnClickListener(v -> {
            if (currentDeepLink == null || currentDeepLink.isEmpty()) {
                Toast.makeText(this, "No QR link saved for this event", Toast.LENGTH_SHORT).show();
            } else {
                showQrPopup(currentDeepLink);
            }
        });

        loadEventDetails();
    }

    /**
     * Prefer to use the entrant's EMAIL as the waitingList key.
     * Fallbacks:
     *  - SharedPreferences "user_email" (if your login stored it)
     *  - FirebaseAuth currentUser email
     *  - ANDROID_ID (old behaviour)
     */
    private String resolveCurrentUserKey() {
        // 1) SharedPreferences (if you store email there)
        String email = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_email", null);

        // 2) FirebaseAuth email
        if ((email == null || email.isEmpty())
                && FirebaseAuth.getInstance().getCurrentUser() != null) {
            email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        // 3) Fallback to device ID to preserve old behaviour
        if (email == null || email.isEmpty()) {
            email = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );
        }
        return email;
    }

    // Loading + binding event
    private void loadEventDetails() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::bindEvent)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindEvent(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            // (Comment out finish() when doing certain UI tests if needed)
            finish();
            return;
        }

        String title = doc.getString("title");
        if (title == null) title = doc.getString("name");

        String description = doc.getString("description");
        String date = doc.getString("date");
        if (date == null) date = doc.getString("startDate");

        String location = doc.getString("location");
        String regStart = doc.getString("registrationStart");
        String regEnd = doc.getString("registrationEnd");

        Long capacity = null;
        Long maxSpots = doc.getLong("maxSpots");
        if (maxSpots != null) capacity = maxSpots;

        if (capacity == null) {
            String capStr = doc.getString("capacity");
            if (capStr != null && !capStr.isEmpty()) {
                try {
                    capacity = Long.parseLong(capStr);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (capacity == null) capacity = 0L;

        List<String> waiting = (List<String>) doc.get("waitingList");
        int joinedCount = waiting == null ? 0 : waiting.size();
        isJoined = waiting != null && waiting.contains(userId);

        currentDeepLink = doc.getString("deepLink");

        txtTitle.setText(title == null ? "Event" : title);
        txtSubtitle.setText(location == null ? "" : location);
        txtTime.setText(date == null ? "" : date);
        txtLocation.setText(location == null ? "" : location);
        txtAbout.setText(description == null ? "" : description);
        txtStats.setText("Spots: " + capacity + " • Joined: " + joinedCount);

        if (regStart != null || regEnd != null) {
            String rs = regStart == null ? "?" : regStart;
            String re = regEnd == null ? "?" : regEnd;
            txtRegWindow.setText("Registration: " + rs + " – " + re);
        } else {
            txtRegWindow.setText("");
        }

        updateJoinedUi();

        btnJoinLeave.setOnClickListener(v -> toggleJoin());
    }

    private void updateJoinedUi() {
        if (isJoined) {
            txtJoinedBadge.setText("✅ You are on the waiting list");
            btnJoinLeave.setText("Leave Waiting List");
        } else {
            txtJoinedBadge.setText("");
            btnJoinLeave.setText("Join Waiting List");
        }
    }


    // Join / leave waiting list  (stores EMAIL or fallback key)
    private void toggleJoin() {
        if (eventId == null) return;

        if (!isJoined) {
            db.collection("events")
                    .document(eventId)
                    .update("waitingList", FieldValue.arrayUnion(userId))
                    .addOnSuccessListener(v -> {
                        isJoined = true;
                        updateJoinedUi();
                        Toast.makeText(this, "Joined waiting list", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            db.collection("events")
                    .document(eventId)
                    .update("waitingList", FieldValue.arrayRemove(userId))
                    .addOnSuccessListener(v -> {
                        isJoined = false;
                        updateJoinedUi();
                        Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to leave: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void showCriteriaDialog() {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_criteria, null, false);

        Button btnGotIt = view.findViewById(R.id.btnGotIt);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        btnGotIt.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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

            new AlertDialog.Builder(this)
                    .setTitle("🎟️ Event QR Code")
                    .setView(qrView)
                    .setPositiveButton("Close", (d, w) -> d.dismiss())
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }
}
