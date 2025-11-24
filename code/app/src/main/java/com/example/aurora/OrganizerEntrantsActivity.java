package com.example.aurora;

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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


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
    private Button btnDrawLottery;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_entrants);

        db = FirebaseFirestore.getInstance();

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
        setupButtons();
        setupTabs();
        setupPosterPicker();
        btnUpdatePoster.setOnClickListener(v -> openPosterPicker());
        loadEventAndLists();
        Button btnExport = findViewById(R.id.btnExportCsv);
        btnExport.setOnClickListener(v -> exportFinalCsv());

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

        btnDrawLottery = findViewById(R.id.btnDrawLottery);
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
    }

    private void setupButtons() {
        btnDrawLottery.setOnClickListener(v -> {
            // TODO: implement your real lottery logic here.
            Toast.makeText(this,
                    "Draw Lottery clicked (UI only for now)",
                    Toast.LENGTH_SHORT).show();
        });

        btnNotify.setOnClickListener(v -> {
            List<EntrantsAdapter.EntrantItem> selected = entrantsAdapter.getSelectedEntrants();
            Toast.makeText(this,
                    "Notify " + selected.size() + " entrant(s) (UI only)",
                    Toast.LENGTH_SHORT).show();
        });

        btnReplace.setOnClickListener(v -> {
            List<EntrantsAdapter.EntrantItem> selected = entrantsAdapter.getSelectedEntrants();
            Toast.makeText(this,
                    "Replace " + selected.size() + " entrant(s) (UI only)",
                    Toast.LENGTH_SHORT).show();
        });
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

        // Reset styles
        resetTabStyles();

        switch (tab) {
            case WAITING:
                highlightTab(tabWaiting);
                loadEntrantsForEmails(waitingEmails, "Waiting");
                break;
            case SELECTED:
                highlightTab(tabSelected);
                loadEntrantsForEmails(selectedEmails, "Selected");
                break;
            case CANCELLED:
                highlightTab(tabCancelled);
                loadEntrantsForEmails(cancelledEmails, "Cancelled");
                break;
            case FINAL:
                highlightTab(tabFinal);
                loadEntrantsForEmails(finalEmails, "Final");
                break;
        }
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

    private void exportFinalCsv() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    List<String> finalList = (List<String>) doc.get("finalEntrants");
                    if (finalList == null || finalList.isEmpty()) {
                        Toast.makeText(this, "No final entrants to export", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        File dir = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS
                        ), "AuroraExports");

                        if (!dir.exists()) dir.mkdirs();

                        File file = new File(dir, "final_list_" + eventId + ".csv");
                        FileWriter writer = new FileWriter(file);

                        writer.append("Email\n");
                        for (String email : finalList) {
                            writer.append(email).append("\n");
                        }

                        writer.flush();
                        writer.close();

                        Toast.makeText(this, "CSV saved: Downloads/AuroraExports/", Toast.LENGTH_LONG).show();

                    } catch (IOException e) {
                        Toast.makeText(this, "Error writing CSV", Toast.LENGTH_SHORT).show();
                    }
                });
    }


}
