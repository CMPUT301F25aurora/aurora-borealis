/**
 * EventDetailsActivity.java
 *
 * Displays full details for a single event and lets the entrant join/leave the waiting list.
 * - Fetches the event document from Firestore (collection: "events") using the provided eventId.
 * - Binds title, date/time, description, location, registration window, and banner UI.
 * - Formats times in Mountain Time (America/Edmonton) and shows a "criteria" dialog on demand.
 * - Uses the device ANDROID_ID as a simple uid; updates the "waitingList" field via arrayUnion/arrayRemove.
 * - Updates UI state (button label, joined badge, stats) after join/leave actions.
 */


package com.example.aurora;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.view.LayoutInflater;
import android.view.View;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;


public class EventDetailsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private String eventId;
    private String uid;
    private ImageView banner;
    private TextView title, subtitle, timeView, about, regWindow, joinedBadge, stats, location;
    private Button btnJoinLeave;
    private final List<String> currentWaitingList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        View criteriaBtn = findViewById(R.id.btnCriteria);
        if (criteriaBtn != null) {
            criteriaBtn.setOnClickListener(v -> showCriteriaDialog());
        }

        // Try to get event ID from intent or deep link
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            eventId = DeepLinkUtil.extractEventIdFromIntent(getIntent());
        }

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        uid = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        db = FirebaseFirestore.getInstance();

        banner = findViewById(R.id.imgBanner);
        title = findViewById(R.id.txtTitle);
        subtitle = findViewById(R.id.txtSubtitle);
        timeView = findViewById(R.id.txtTime);
        about = findViewById(R.id.txtAbout);
        regWindow = findViewById(R.id.txtRegWindow);
        joinedBadge = findViewById(R.id.txtJoinedBadge);
        stats = findViewById(R.id.txtStats);
        location = findViewById(R.id.txtLocation);
        btnJoinLeave = findViewById(R.id.btnJoinLeave);

        loadEvent();
        btnJoinLeave.setOnClickListener(v -> toggleWaitlist());
        // QR Code button
        Button btnShowQR = findViewById(R.id.btnShowQR);
        if (btnShowQR != null) {
            btnShowQR.setOnClickListener(v -> fetchAndShowQr());
        }

    }

    private void showCriteriaDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_criteria, null, false);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        View close = dialogView.findViewById(R.id.btnGotIt);
        if (close != null) close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadEvent() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(this::bindEvent);
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private void bindEvent(DocumentSnapshot d) {
        if (d == null || !d.exists()) return;

        String titleStr = nz(d.getString("title"));
        if (titleStr.isEmpty()) titleStr = nz(d.getString("name"));

        String dateStr = nz(d.getString("date"));
        if (dateStr.isEmpty()) dateStr = nz(d.getString("dateDisplay"));

        title.setText(titleStr);
        subtitle.setText(dateStr);

        String locStr = nz(d.getString("location"));
        if (locStr.isEmpty()) {
            String ln = nz(d.getString("locationName"));
            String la = nz(d.getString("locationAddress"));
            locStr = (ln + (la.isEmpty() ? "" : ", " + la)).trim();
        }
        location.setText(locStr);

        String aboutStr = nz(d.getString("description"));
        if (aboutStr.isEmpty()) aboutStr = nz(d.getString("notes"));
        about.setText(aboutStr);

        Timestamp startTs = d.getTimestamp("startAt");
        Timestamp endTs = d.getTimestamp("endAt");
        if (startTs != null && endTs != null) {
            SimpleDateFormat tfmt = new SimpleDateFormat("h:mm a 'MST'", Locale.CANADA);
            tfmt.setTimeZone(TimeZone.getTimeZone("America/Edmonton")); // Mountain Time
            String s = tfmt.format(startTs.toDate());
            String e = tfmt.format(endTs.toDate());
            timeView.setText(s + " – " + e);
            timeView.setVisibility(TextView.VISIBLE);
        } else {
            timeView.setText("");
            timeView.setVisibility(TextView.GONE);
        }

        Timestamp regOpen = d.getTimestamp("registrationOpensAt");
        Timestamp regClose = d.getTimestamp("registrationClosesAt");
        if (regOpen != null || regClose != null) {
            SimpleDateFormat dfmt = new SimpleDateFormat("MMM d, yyyy h:mm a 'MST'", Locale.CANADA);
            dfmt.setTimeZone(TimeZone.getTimeZone("America/Edmonton"));
            String openS = regOpen == null ? "" : dfmt.format(regOpen.toDate());
            String closeS = regClose == null ? "" : dfmt.format(regClose.toDate());
            regWindow.setText(("Registration: " + openS +
                    (closeS.isEmpty() ? "" : " — " + closeS)).trim());
        } else {
            regWindow.setText("");
        }

        List<String> wl = (List<String>) d.get("waitingList");
        currentWaitingList.clear();
        if (wl != null) currentWaitingList.addAll(wl);
        stats.setText("Waiting List: " + currentWaitingList.size());

        boolean joined = currentWaitingList.contains(uid);
        joinedBadge.setText(joined ? "You're on the waiting list" : "");
        btnJoinLeave.setText(joined ? "Leave Waiting List" : "Join Waiting List");
    }

    private void toggleWaitlist() {
        boolean joined = currentWaitingList.contains(uid);
        if (joined) {
            db.collection("events").document(eventId)
                    .update("waitingList", FieldValue.arrayRemove(uid))
                    .addOnSuccessListener(v -> {
                        currentWaitingList.remove(uid);
                        btnJoinLeave.setText("Join Waiting List");
                        joinedBadge.setText("");
                        stats.setText("Waiting List: " + currentWaitingList.size());
                    });
        } else {
            db.collection("events").document(eventId)
                    .update("waitingList", FieldValue.arrayUnion(uid))
                    .addOnSuccessListener(v -> {
                        currentWaitingList.add(uid);
                        btnJoinLeave.setText("Leave Waiting List");
                        joinedBadge.setText("You're on the waiting list");
                        stats.setText("Waiting List: " + currentWaitingList.size());
                    });
        }
    }
    private void fetchAndShowQr() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String deepLink = doc.getString("deepLink");
                        showQrPopup(deepLink);
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error fetching QR: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showQrPopup(String deepLink) {
        if (deepLink == null || deepLink.isEmpty()) {
            Toast.makeText(this, "No QR code available for this event", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            com.google.zxing.qrcode.QRCodeWriter writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(
                    deepLink,
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    800, 800
            );

            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(800, 800, android.graphics.Bitmap.Config.RGB_565);
            for (int x = 0; x < 800; x++) {
                for (int y = 0; y < 800; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y)
                            ? android.graphics.Color.BLACK
                            : android.graphics.Color.WHITE);
                }
            }

            android.widget.ImageView qrView = new android.widget.ImageView(this);
            qrView.setImageBitmap(bitmap);
            qrView.setPadding(40, 40, 40, 40);

            new androidx.appcompat.app.AlertDialog.Builder(this)
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
