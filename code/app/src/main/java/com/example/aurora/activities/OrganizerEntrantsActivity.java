package com.example.aurora.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aurora.R;
import com.example.aurora.adapters.EntrantsAdapter;
import com.example.aurora.notifications.FirestoreNotificationHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AlertDialog;
import android.widget.EditText;

import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * OrganizerEntrantsActivity
 * Lottery-style management screen for a single event.
 * - Shows stats: waiting, selected, cancelled, total spots.
 * - Tabs: Waiting / Selected / Cancelled / Final.
 * - List of entrants with checkbox + status badge.
 * are assumed to store EMAIL strings.
 */

public class OrganizerEntrantsActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    // Header
    private ImageButton btnBack;
    private TextView tvEventTitle;
    private TextView tvEventSubtitle;

    // Stat cards
    private TextView tvWaitingCount;
    private TextView tvSelectedCount;
    private TextView tvCancelledCount;
    private TextView tvTotalSpots;

    // Buttons

    private Button btnNotify;
    private Button btnReplace;
    // Tabs
    private TextView tabWaiting;
    private TextView tabSelected;
    private TextView tabCancelled;
    private TextView tabFinal;

    // List
    private RecyclerView recyclerEntrants;
    private EntrantsAdapter entrantsAdapter;

    private String eventId;
    private ImageView imgEventPoster;
    private Button btnUpdatePoster;

    private Uri newPosterUri = null;
    private ActivityResultLauncher<Intent> posterPickerLauncher;
    private StorageReference posterStorageRef;


    // Email lists from the event doc
    private List<String> waitingEmails = new ArrayList<>();
    private List<String> selectedEmails = new ArrayList<>();
    private List<String> cancelledEmails = new ArrayList<>();
    private List<String> finalEmails = new ArrayList<>();
    private long maxSpots = 0L;

    // Current tab
    private enum Tab { WAITING, SELECTED, CANCELLED, FINAL }
    private Tab currentTab = Tab.WAITING;
    private String organizerEmail;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_entrants);

        db = FirebaseFirestore.getInstance();
        organizerEmail = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_email", null);

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 🔥 Initialize storage
        posterStorageRef = FirebaseStorage.getInstance()
                .getReference()
                .child("event_posters");

        bindViews();
        setupRecycler();
        setupTabs();
        setupNotifyButtonLogic();
        setupPosterPicker();
        btnUpdatePoster.setOnClickListener(v -> openPosterPicker());
        loadEventAndLists();
        Button btnExportCsv = findViewById(R.id.btnExportCsv);
        btnExportCsv.setOnClickListener(v -> exportFinalListAsCsv());

    }

    private void bindViews() {
        imgEventPoster = findViewById(R.id.imgEventPoster);
        btnUpdatePoster = findViewById(R.id.btnUpdatePoster);

        btnBack = findViewById(R.id.btnBackEntrants);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvEventSubtitle = findViewById(R.id.tvEventSubtitle);

        tvWaitingCount = findViewById(R.id.tvWaitingCount);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        tvCancelledCount = findViewById(R.id.tvCancelledCount);
        tvTotalSpots = findViewById(R.id.tvTotalSpots);


        btnNotify = findViewById(R.id.btnNotify);
        btnReplace = findViewById(R.id.btnReplace);

        tabWaiting = findViewById(R.id.tabWaiting);
        tabSelected = findViewById(R.id.tabSelected);
        tabCancelled = findViewById(R.id.tabCancelled);
        tabFinal = findViewById(R.id.tabFinal);

        recyclerEntrants = findViewById(R.id.recyclerEntrants);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecycler() {
        entrantsAdapter = new EntrantsAdapter(this, new ArrayList<>());
        recyclerEntrants.setLayoutManager(new LinearLayoutManager(this));
        recyclerEntrants.setAdapter(entrantsAdapter);
        entrantsAdapter.setSelectionListener(() -> updateNotifyButtonMode());
        entrantsAdapter.setDeleteListener(email -> {
            db.collection("events")
                    .document(eventId)
                    .update("selectedEntrants", FieldValue.arrayRemove(email))
                    .addOnSuccessListener(v -> {
                        entrantsAdapter.removeByEmail(email);
                        selectedEmails.remove(email);
                        tvSelectedCount.setText(String.valueOf(selectedEmails.size()));
                        Toast.makeText(this, "Entrant removed", Toast.LENGTH_SHORT).show();
                    });
        });

    }
    private void setupNotifyButtonLogic() {

        btnNotify.setText("Notify All");

        btnNotify.setOnClickListener(v -> {

            List<EntrantsAdapter.EntrantItem> selected = entrantsAdapter.getSelectedEntrants();

            if (currentTab == Tab.WAITING) {
                showCustomMessageDialog(msg -> {
                    if (selected.isEmpty()) notifyAllWaiting(msg);
                    else notifySelectedEntrants_Waiting(msg, selected);
                });
            }

            else if (currentTab == Tab.SELECTED) {
                showCustomMessageDialog(msg -> {
                    if (selected.isEmpty()) notifyAllSelected(msg);
                    else notifySelectedEntrants_SelectedTab(msg, selected);
                });
            }

            else if (currentTab == Tab.CANCELLED) {
                showCustomMessageDialog(msg -> {
                    if (selected.isEmpty()) notifyAllCancelled(msg);
                    else notifySelectedEntrants_Cancelled(msg, selected);
                });
            }
        });
    }


    // WAITING — notify ALL
    private void notifyAllWaiting(String msg) {
        if (waitingEmails.isEmpty()) {
            Toast.makeText(this, "No entrants in waiting list.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    String eventName = doc.getString("title");
                    if (eventName == null) eventName = "Event";

                    for (String email : waitingEmails) {
                        FirestoreNotificationHelper.sendCustomNotification(
                                db, email, eventName, eventId, msg, organizerEmail
                        );
                    }

                    Toast.makeText(this, "Notified all waiting entrants.", Toast.LENGTH_SHORT).show();
                });
    }


    // WAITING — notify SELECTED (checkbox)
    private void notifySelectedEntrants_Waiting(String msg, List<EntrantsAdapter.EntrantItem> selected) {

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {

                    String eventName = doc.getString("title");

                    for (EntrantsAdapter.EntrantItem e : selected) {
                        FirestoreNotificationHelper.sendCustomNotification(
                                db, e.getEmail(), eventName, eventId, msg, organizerEmail
                        );
                    }

                    Toast.makeText(this,
                            "Notified " + selected.size() + " waiting entrant(s).",
                            Toast.LENGTH_SHORT).show();
                });
    }


    // SELECTED — notify ALL
    private void notifyAllSelected(String msg) {

        if (selectedEmails.isEmpty()) {
            Toast.makeText(this, "No selected entrants.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {

                    String eventName = doc.getString("title");

                    for (String email : selectedEmails) {
                        FirestoreNotificationHelper.sendCustomNotification(
                                db, email, eventName, eventId, msg, organizerEmail
                        );
                    }

                    Toast.makeText(this,
                            "Notified ALL selected entrants (" + selectedEmails.size() + ")",
                            Toast.LENGTH_SHORT).show();
                });
    }


    // SELECTED — notify SELECTED (checkbox)
    private void notifySelectedEntrants_SelectedTab(String msg, List<EntrantsAdapter.EntrantItem> selected) {

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {

                    String eventName = doc.getString("title");

                    for (EntrantsAdapter.EntrantItem e : selected) {
                        FirestoreNotificationHelper.sendCustomNotification(
                                db, e.getEmail(), eventName, eventId, msg, organizerEmail
                        );
                    }

                    Toast.makeText(this,
                            "Notified " + selected.size() + " selected entrant(s).",
                            Toast.LENGTH_SHORT).show();
                });
    }


    // CANCELLED — notify ALL
    private void notifyAllCancelled(String msg) {
        if (cancelledEmails.isEmpty()) {
            Toast.makeText(this, "No cancelled entrants.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {

                    String eventName = doc.getString("title");

                    for (String email : cancelledEmails) {
                        FirestoreNotificationHelper.sendCustomNotification(
                                db, email, eventName, eventId, msg, organizerEmail
                        );
                    }

                    Toast.makeText(this,
                            "Notified all cancelled entrants.",
                            Toast.LENGTH_SHORT).show();
                });
    }


    // CANCELLED — notify SELECTED (checkbox)
    private void notifySelectedEntrants_Cancelled(String msg, List<EntrantsAdapter.EntrantItem> selected) {

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {

                    String eventName = doc.getString("title");

                    for (EntrantsAdapter.EntrantItem e : selected) {
                        FirestoreNotificationHelper.sendCustomNotification(
                                db, e.getEmail(), eventName, eventId, msg, organizerEmail
                        );
                    }

                    Toast.makeText(this,
                            "Notified " + selected.size() + " cancelled entrant(s).",
                            Toast.LENGTH_SHORT).show();
                });
    }


    // ⭐ ADDED — Automatically update button label based on selection state
    private void updateNotifyButtonMode() {

        List<EntrantsAdapter.EntrantItem> selected = entrantsAdapter.getSelectedEntrants();

        if (selected.isEmpty()) {

            if (currentTab == Tab.WAITING) {
                btnNotify.setText("Notify All");
            } else if (currentTab == Tab.SELECTED) {
                btnNotify.setText("Notify All Selected");
            } else if (currentTab == Tab.CANCELLED) {   // ⭐ ADD THIS
                btnNotify.setText("Notify All Cancelled");
            }

        } else {

            if (currentTab == Tab.WAITING) {
                btnNotify.setText("Notify Selected (" + selected.size() + ")");
            } else if (currentTab == Tab.SELECTED) {
                btnNotify.setText("Notify Selected (" + selected.size() + ")");
            } else if (currentTab == Tab.CANCELLED) {   // ⭐ ADD THIS
                btnNotify.setText("Notify Selected (" + selected.size() + ")");
            }
        }
    }



    private void setupButtons() {

        // Only replace button stays here
        btnReplace.setOnClickListener(v -> {
            List<EntrantsAdapter.EntrantItem> selected = entrantsAdapter.getSelectedEntrants();

            if (currentTab != Tab.SELECTED) {
                Toast.makeText(this,
                        "Switch to the Selected tab to cancel entrants.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (selected.isEmpty()) {
                Toast.makeText(this,
                        "Select at least one entrant to cancel.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            cancelUnconfirmedEntrants(selected);
        });
    }


    private void cancelUnconfirmedEntrants(List<EntrantsAdapter.EntrantItem> toCancel) {
        if (selectedEmails == null) selectedEmails = new ArrayList<>();
        if (cancelledEmails == null) cancelledEmails = new ArrayList<>();

        List<String> emailsToMove = new ArrayList<>();

        for (EntrantsAdapter.EntrantItem item : toCancel) {
            String email = item.getEmail();
            if (email == null || email.isEmpty()) continue;

            // Only move if currently in selected list
            if (selectedEmails.remove(email)) {
                if (!cancelledEmails.contains(email)) {
                    cancelledEmails.add(email);
                }
                emailsToMove.add(email);
            }
        }

        if (emailsToMove.isEmpty()) {
            Toast.makeText(this,
                    "No selected entrants to cancel.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("selectedEntrants", selectedEmails);
        updates.put("cancelledEntrants", cancelledEmails);

        db.collection("events")
                .document(eventId)
                .update(updates)
                .addOnSuccessListener(v -> {
                    // Update stat cards
                    tvSelectedCount.setText(String.valueOf(selectedEmails.size()));
                    tvCancelledCount.setText(String.valueOf(cancelledEmails.size()));

                    // Show them under the Cancelled tab
                    setActiveTab(Tab.CANCELLED);

                    Toast.makeText(this,
                            "Cancelled " + emailsToMove.size() + " entrant(s) who did not sign up.",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to cancel entrants: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }


    private void setupTabs() {
        View.OnClickListener listener = v -> {
            if (v.getId() == R.id.tabWaiting) {
                setActiveTab(Tab.WAITING);
            } else if (v.getId() == R.id.tabSelected) {
                setActiveTab(Tab.SELECTED);
            } else if (v.getId() == R.id.tabCancelled) {
                setActiveTab(Tab.CANCELLED);
            } else if (v.getId() == R.id.tabFinal) {
                setActiveTab(Tab.FINAL);
            }
        };

        tabWaiting.setOnClickListener(listener);
        tabSelected.setOnClickListener(listener);
        tabCancelled.setOnClickListener(listener);
        tabFinal.setOnClickListener(listener);
    }

    private void loadEventAndLists() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::bindEventData)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void bindEventData(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Title
        String title = doc.getString("title");
        if (title == null) title = doc.getString("name");
        if (title == null) title = "Event";
        tvEventTitle.setText(title);

        // Subtitle
        String date = doc.getString("date");
        if (date == null) date = doc.getString("startDate");

        String location = doc.getString("location");

        String subtitle = "";
        if (date != null) subtitle += date;
        if (location != null) {
            if (!subtitle.isEmpty()) subtitle += " • ";
            subtitle += location;
        }
        tvEventSubtitle.setText(subtitle);

        // Poster URL 🔥🔥🔥
        String posterUrl = doc.getString("posterUrl");
        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(this)
                    .load(posterUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(imgEventPoster);
        }

        // Lists
        waitingEmails = (List<String>) doc.get("waitingList");
        if (waitingEmails == null) waitingEmails = new ArrayList<>();

        selectedEmails = (List<String>) doc.get("selectedEntrants");
        if (selectedEmails == null) selectedEmails = new ArrayList<>();

        cancelledEmails = (List<String>) doc.get("cancelledEntrants");
        if (cancelledEmails == null) cancelledEmails = new ArrayList<>();

        finalEmails = (List<String>) doc.get("finalEntrants");
        if (finalEmails == null) finalEmails = new ArrayList<>();

        // Capacity
        Long max = doc.getLong("maxSpots");
        if (max == null) max = 0L;
        maxSpots = max;

        // Cards
        tvWaitingCount.setText(String.valueOf(waitingEmails.size()));
        tvSelectedCount.setText(String.valueOf(selectedEmails.size()));
        tvCancelledCount.setText(String.valueOf(cancelledEmails.size()));
        tvTotalSpots.setText(String.valueOf(maxSpots));

        // Default tab
        setActiveTab(Tab.WAITING);
    }

    private void setActiveTab(Tab tab) {

        currentTab = tab;
        resetTabStyles();

        // Always show the notify button for Waiting, Selected, and Cancelled
        // Only hide for Final
        btnNotify.setVisibility(View.VISIBLE);

        switch (tab) {

            case WAITING:
                highlightTab(tabWaiting);
                loadEntrantsForEmails(waitingEmails, "Waiting");
                btnNotify.setText("Notify All");
                break;

            case SELECTED:
                highlightTab(tabSelected);
                loadEntrantsForEmails(selectedEmails, "Selected");
                btnNotify.setText("Notify All Selected");
                break;

            case CANCELLED:
                highlightTab(tabCancelled);
                loadEntrantsForEmails(cancelledEmails, "Cancelled");
                btnNotify.setText("Notify All Cancelled");   // ⭐ NEW
                break;

            case FINAL:
                highlightTab(tabFinal);
                loadEntrantsForEmails(finalEmails, "Final");
                btnNotify.setVisibility(View.GONE);  // FINAL should NOT notify anyone
                break;
        }

        updateNotifyButtonMode(); // always recalc label based on selection
    }

    private void resetTabStyles() {
        int normalColor = getResources().getColor(android.R.color.darker_gray);
        int normalBg = getResources().getColor(android.R.color.transparent);

        tabWaiting.setTextColor(normalColor);
        tabSelected.setTextColor(normalColor);
        tabCancelled.setTextColor(normalColor);
        tabFinal.setTextColor(normalColor);

        tabWaiting.setBackgroundColor(normalBg);
        tabSelected.setBackgroundColor(normalBg);
        tabCancelled.setBackgroundColor(normalBg);
        tabFinal.setBackgroundColor(normalBg);
    }

    private void highlightTab(TextView tab) {
        int accent = getResources().getColor(R.color.purple_500, getTheme());
        tab.setTextColor(accent);
        tab.setBackgroundResource(R.drawable.bg_tab_selected);
    }

    private void loadEntrantsForEmails(List<String> emails, String statusLabel) {
        entrantsAdapter.clearItems();

        if (emails == null || emails.isEmpty()) {
            Toast.makeText(this, "No entrants in this list yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String email : emails) {
            if (email == null || email.isEmpty()) continue;

            // For each email, lookup name from users collection
            db.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snap -> {
                        String name = email; // default fallback

                        if (!snap.isEmpty() && snap.getDocuments().get(0) != null) {
                            DocumentSnapshot u = snap.getDocuments().get(0);
                            String n = u.getString("name");
                            if (n != null && !n.isEmpty()) {
                                name = n;
                            }
                        }

                        EntrantsAdapter.EntrantItem item =
                                new EntrantsAdapter.EntrantItem(name, email, statusLabel);

                        entrantsAdapter.addItem(item);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Failed to load user for " + email,
                                    Toast.LENGTH_SHORT).show());
        }
    }
    private void setupPosterPicker() {
        posterPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        newPosterUri = result.getData().getData();
                        if (newPosterUri != null) {
                            uploadNewPoster();
                        }
                    }
                }
        );
    }

    private void openPosterPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        posterPickerLauncher.launch(
                Intent.createChooser(intent, "Select New Poster")
        );
    }

    private void uploadNewPoster() {
        if (newPosterUri == null) return;

        StorageReference ref = posterStorageRef.child(eventId + ".jpg");

        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        ref.putFile(newPosterUri)
                .continueWithTask(task -> ref.getDownloadUrl())
                .addOnSuccessListener(downloadUrl -> {
                    db.collection("events")
                            .document(eventId)
                            .update("posterUrl", downloadUrl.toString())
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Poster Updated!", Toast.LENGTH_SHORT).show();

                                Glide.with(this)
                                        .load(downloadUrl)
                                        .placeholder(R.drawable.ic_launcher_background)
                                        .into(imgEventPoster);
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private interface CustomMessageCallback {
        void onMessage(String msg);
    }

    private void showCustomMessageDialog(CustomMessageCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send Message");

        final EditText input = new EditText(this);
        input.setHint("Type your message...");
        input.setMinLines(2);
        input.setPadding(50, 40, 50, 20);
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String msg = input.getText().toString().trim();
            if (msg.isEmpty()) {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            callback.onMessage(msg);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void exportFinalListAsCsv() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> finalEntrants = (List<String>) doc.get("finalEntrants");
                    if (finalEntrants == null || finalEntrants.isEmpty()) {
                        Toast.makeText(this, "No final entrants to export", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    fetchEntrantDetails(finalEntrants);
                });
    }

    private void fetchEntrantDetails(List<String> emails) {
        StringBuilder csv = new StringBuilder("Name,Email,Phone\n");

        final int total = emails.size();
        final int[] count = {0};

        for (String email : emails) {
            db.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snap -> {
                        String name = "";
                        String phone = "";

                        if (!snap.isEmpty()) {
                            DocumentSnapshot user = snap.getDocuments().get(0);
                            name = user.getString("name");
                            phone = user.getString("phone");
                        }

                        if (name == null) name = "";
                        if (phone == null) phone = "";

                        csv.append(name).append(",")
                                .append(email).append(",")
                                .append(phone).append("\n");

                        count[0]++;
                        if (count[0] == total) {
                            saveCsvToFile(csv.toString());
                        }
                    });
        }
    }

    private void saveCsvToFile(String csvData) {
        try {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloads, "Event_" + eventId + "_FinalEntrants.csv");

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(csvData.getBytes());
            fos.close();

            Toast.makeText(this, "CSV exported to Downloads", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void removeEntrantFromSelected(String email) {

        if (!selectedEmails.contains(email)) {
            Toast.makeText(this, "Entrant not in selected list", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedEmails.remove(email);

        db.collection("events")
                .document(eventId)
                .update("selectedEntrants", selectedEmails)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Entrant removed", Toast.LENGTH_SHORT).show();
                    setActiveTab(Tab.SELECTED);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}