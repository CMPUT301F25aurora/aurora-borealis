package com.example.aurora;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class EventDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String eventId;
    private String uid;

    private ImageView banner;
    private TextView title, subtitle, timeView, about, regWindow, joinedBadge, stats, location;
    private Button btnJoinLeave;
    private Button btnScanQr;

    private final List<String> currentWaitingList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Lottery criteria button
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
        btnScanQr = findViewById(R.id.btnScanQr);

        loadEvent();

        btnJoinLeave.setOnClickListener(v -> toggleWaitlist());

        // Criteria button (again, just to be safe)
        Button btnCriteria = findViewById(R.id.btnCriteria);
        if (btnCriteria != null) {
            btnCriteria.setOnClickListener(v -> showCriteriaDialog());
        }

        // Scan QR button on details screen
        if (btnScanQr != null) {
            btnScanQr.setOnClickListener(v -> startQrScan());
        }
    }

    // ---------------------------------------------------
    // Lottery criteria dialog
    // ---------------------------------------------------

    private void showCriteriaDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_criteria, null, false);

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

    // ---------------------------------------------------
    // Load event data
    // ---------------------------------------------------

    private void loadEvent() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(this::bindEvent)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show());
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

    // ---------------------------------------------------
    // Join / leave waiting list
    // ---------------------------------------------------

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

    // ---------------------------------------------------
    // QR scanning from Event Details
    // ---------------------------------------------------

    private void startQrScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan Aurora event QR");
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                handleScannedText(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleScannedText(String text) {
        try {
            // Treat the scanned text as a deep link and reuse DeepLinkUtil logic
            Intent tmp = new Intent(Intent.ACTION_VIEW, Uri.parse(text));
            String scannedId = DeepLinkUtil.extractEventIdFromIntent(tmp);

            if (scannedId == null || scannedId.isEmpty()) {
                Toast.makeText(this, "Invalid Aurora event QR", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update this screen to show the new event
            this.eventId = scannedId;
            loadEvent();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to handle QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
