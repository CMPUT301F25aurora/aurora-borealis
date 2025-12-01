/**
 * OrganizerActivity
 *
 * Main screen for organizers.
 * Lets organizers:
 *  View events they created
 *  Create new events
 *  Run lotteries
 *  View maps / manage entrants / show QR codes
 *  Switch to entrant mode
 *  Delete events
 *
 * Loads all events owned by the current organizer and builds
 * cards dynamically into a vertical list.
 */

package com.example.aurora.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.R;
import com.example.aurora.map.EventMapActivity;
import com.example.aurora.models.NotificationModel;
import com.example.aurora.notifications.FirestoreNotificationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrganizerActivity extends AppCompatActivity {

    private ImageButton btnLogout;
    private LinearLayout eventListContainer;
    private LinearLayout bottomHome, bottomProfile, createEventButton;

    private FirebaseFirestore db;
    private String organizerEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
        String docId = sp.getString("user_doc_id", null);
        organizerEmail = sp.getString("user_email", null);

        if (docId == null || organizerEmail == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupTopBar();
        setupBottomNav();
        loadEventsFromFirebase();

        ExtendedFloatingActionButton fab = findViewById(R.id.roleSwitchFab);
        fab.setText("Entrant Mode");
        fab.setTranslationY(-30);

        fab.setOnClickListener(v -> {
            sp.edit().putString("user_last_mode","entrant").apply();
            startActivity(new Intent(this, EventsActivity.class));
            finish();
        });

    }

    /** Connects XML views to Java fields. */
    private void bindViews() {
        createEventButton = findViewById(R.id.createEventButton);
        eventListContainer = findViewById(R.id.eventListContainer);
        btnLogout = findViewById(R.id.btnLogoutOrganizer);
        bottomHome = findViewById(R.id.bottomHome);
        bottomProfile = findViewById(R.id.bottomProfile);
    }

    /** Sets up logout and back button behavior in the top bar. */
    private void setupTopBar() {
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logoutUser());
        }
    }

    /** Logs the organizer out and clears saved session. */
    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .edit().clear().apply();

        Intent intent = new Intent(OrganizerActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    /** Bottom navigation bar for Home, Profile, Create Event. */
    private void setupBottomNav() {
        bottomHome.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerActivity.this, OrganizerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        bottomProfile.setOnClickListener(v ->
                startActivity(new Intent(OrganizerActivity.this, OrganizerProfileActivity.class))
        );

        createEventButton.setOnClickListener(v ->
                startActivity(new Intent(OrganizerActivity.this, CreateEventActivity.class))
        );
    }


    /** Loads all events created by this organizer from Firestore. */
    private void loadEventsFromFirebase() {
        eventListContainer.removeAllViews();

        db.collection("events")
                .whereEqualTo("organizerEmail", organizerEmail)
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

    /**
     * Creates a clickable event card for one event document.
     * Card contains:
     *  title, date, stats
     *  Show QR
     *  Manage Entrants
     *  Lottery
     *  Map View
     *  Delete
     */
    private void addEventCard(DocumentSnapshot doc) {
        View eventView = LayoutInflater.from(this)
                .inflate(R.layout.item_event_card, eventListContainer, false);

        TextView title = eventView.findViewById(R.id.eventTitle);
        TextView date = eventView.findViewById(R.id.eventDate);
        TextView stats = eventView.findViewById(R.id.eventStats);

        Button btnManage = eventView.findViewById(R.id.btnManage);
        Button btnLottery = eventView.findViewById(R.id.btnLottery);
        ImageButton btnShowQR = eventView.findViewById(R.id.btnShowQR);
        ImageButton btnMap = eventView.findViewById(R.id.btnMap);
        ImageButton btnDelete = eventView.findViewById(R.id.btnDeleteEvent);


        List<String> selected = (List<String>) doc.get("selectedEntrants");
        if (selected != null && !selected.isEmpty()) {
            btnLottery.setText("Re-roll");
        } else {
            btnLottery.setText("Lottery");
        }
        String eventId = doc.getId();

        String titleText = doc.getString("title");
        if (titleText == null) titleText = doc.getString("name");
        if (titleText == null) titleText = "Untitled Event";

        String dateText = doc.getString("date");
        if (dateText == null) dateText = doc.getString("startDate");
        if (dateText == null) dateText = "Date not set";

        Long maxSpots = doc.getLong("maxSpots");
        if (maxSpots == null) maxSpots = 0L;

        String deepLink = doc.getString("deepLink");

        title.setText(titleText);
        date.setText(dateText);
        stats.setText("Max spots: " + maxSpots);

        btnMap.setOnClickListener(v -> {
            Intent i = new Intent(OrganizerActivity.this, EventMapActivity.class);
            i.putExtra("eventId", eventId);
            startActivity(i);
        });

        btnShowQR.setOnClickListener(v -> {
            if (deepLink == null || deepLink.isEmpty()) {
                Toast.makeText(this, "No QR saved", Toast.LENGTH_SHORT).show();
            } else {
                showQrPopup(deepLink);
            }
        });

        btnManage.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerActivity.this, OrganizerEntrantsActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        btnLottery.setOnClickListener(v -> runLotteryDialog(eventId));
        btnDelete.setOnClickListener(v -> confirmDelete(eventId));

        eventListContainer.addView(eventView);
    }

    /**
     * Shows "Run Lottery" input dialog where organizer enters amount to draw.
     */
    private void runLotteryDialog(String eventId) {

        View view = getLayoutInflater().inflate(R.layout.dialog_run_lottery, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText input = view.findViewById(R.id.inputNumber);
        Button btnCancel = view.findViewById(R.id.btnCancelLottery);
        Button btnRun = view.findViewById(R.id.btnRunLottery);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnRun.setOnClickListener(v -> {
            String s = input.getText().toString().trim();
            if (s.isEmpty()) {
                Toast.makeText(this, "Enter a number", Toast.LENGTH_SHORT).show();
                return;
            }

            runLottery(eventId, Integer.parseInt(s));
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Performs the actual lottery:
     *  shuffles waitingList
     *  picks N winners
     *  marks the rest as losers
     *  updates Firestore fields
     *  sends notifications
     */
    private void runLottery(String eventId, int n) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {

                    List<String> waiting = (List<String>) doc.get("waitingList");
                    if (waiting == null || waiting.isEmpty()) {
                        Toast.makeText(this, "Waiting list is empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> emailsOnly = new ArrayList<>();
                    for (String w : waiting)
                        if (w.contains("@")) emailsOnly.add(w);

                    if (emailsOnly.isEmpty()) {
                        Toast.makeText(this, "No valid email entrants.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (n > emailsOnly.size()) {
                        Toast.makeText(this, "Not enough entrants.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Collections.shuffle(emailsOnly);
                    List<String> winners = emailsOnly.subList(0, n);

                    List<String> losers = new ArrayList<>();
                    for (String email : emailsOnly)
                        if (!winners.contains(email))
                            losers.add(email);

                    db.collection("events").document(eventId)
                            .update(
                                    "selectedEntrants", winners,
                                    "losersEntrants", FieldValue.arrayUnion(losers.toArray()),
                                    "waitingList", FieldValue.arrayRemove(winners.toArray())
                            )
                            .addOnSuccessListener(x -> {

                                sendWinnerNotifications(eventId, winners);
                                sendNotSelectedNotifications(eventId, emailsOnly, winners);
                                showWinnersDialog(winners);
                                loadEventsFromFirebase();
                            });

                });
    }

    /**
     * Sends notifications to entrants who were *not selected*.
     */
    private void sendNotSelectedNotifications(String eventId, List<String> allEntrants, List<String> winners) {

        for (String email : allEntrants) {

            if (winners.contains(email)) continue;

            NotificationModel notif = new NotificationModel(
                    "not_selected",
                    "Lottery Result",
                    "Unfortunately, you were not selected for this event.",
                    eventId,
                    email,
                    System.currentTimeMillis()
            );

            FirestoreNotificationHelper.sendIfAllowed(db, email, notif);

            FirestoreNotificationHelper.logNotification(
                    db,
                    organizerEmail,
                    eventId,
                    "Lottery Result",
                    email,
                    "Unfortunately, you were not selected for this event.",
                    "not_selected"
            );
        }
    }

    /**
     * Sends notification to entrants who *won* the lottery.
     */
    private void sendWinnerNotifications(String eventId, List<String> winners) {
        for (String email : winners) {

            NotificationModel notif = new NotificationModel(
                    "winner_selected",
                    "You've Been Selected!",
                    "You won the lottery! Accept or decline your spot.",
                    eventId,
                    email,
                    System.currentTimeMillis()
            );

            FirestoreNotificationHelper.sendIfAllowed(db, email, notif);

            FirestoreNotificationHelper.logNotification(
                    db,
                    organizerEmail,
                    eventId,
                    "Winner Selected",
                    email,
                    "You won the lottery! Accept or decline your spot.",
                    "winner_selected"
            );
        }
    }

    /**
     * Pops a dialog listing all winners after the lottery finishes.
     */
    private void showWinnersDialog(List<String> winners) {

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_winners, null);

        TextView tvList = view.findViewById(R.id.dialogList);
        MaterialButton btnOk = view.findViewById(R.id.btnConfirm);

        StringBuilder sb = new StringBuilder();
        for (String w : winners) {
            sb.append("• ").append(w).append("\n");
        }
        tvList.setText(sb.toString());

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.TransparentDialog)
                .setView(view)
                .setCancelable(true)
                .create();

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Generates and displays a QR code from the event's deepLink.
     */
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
                    .setTitle("Event QR Code")
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

    /**
     * Asks organizer to confirm deleting an event.
     */
    private void confirmDelete(String eventId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Event?")
                .setMessage("Are you sure you want to delete this event? This will permanently remove all entrants, waiting lists, and lottery data.")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent(eventId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes:
     *  waitingLocations subcollection
     *  notifications for this event
     *  the event document itself
     */
    private void deleteEvent(String eventId) {

        db.collection("events")
                .document(eventId)
                .collection("waitingLocations")
                .get()
                .addOnSuccessListener(waitSnap -> {

                    for (DocumentSnapshot d : waitSnap.getDocuments()) {
                        d.getReference().delete();
                    }

                    db.collection("notifications")
                            .whereEqualTo("eventId", eventId)
                            .get()
                            .addOnSuccessListener(notifSnap -> {

                                for (DocumentSnapshot d : notifSnap.getDocuments()) {
                                    d.getReference().delete();
                                }

                                db.collection("events").document(eventId)
                                        .delete()
                                        .addOnSuccessListener(v -> {
                                            Toast.makeText(this, "Event deleted.", Toast.LENGTH_SHORT).show();
                                            loadEventsFromFirebase(); // refresh UI
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            });
                });
    }




}
