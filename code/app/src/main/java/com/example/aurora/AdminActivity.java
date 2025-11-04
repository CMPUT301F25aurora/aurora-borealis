package com.example.aurora;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    private TextView countEvents, countUsers, countImages, countLogs;
    private TextView sectionTitle;
    private LinearLayout tabEvents, tabProfiles, tabImages, tabLogs;
    private LinearLayout listContainer;
    private MaterialButton buttonSearch;

    private FirebaseFirestore db;

    // Cached lists so we can filter client-side with the search dialog
    private List<DocumentSnapshot> eventDocs = new ArrayList<>();
    private List<DocumentSnapshot> profileDocs = new ArrayList<>();
    private List<DocumentSnapshot> logDocs = new ArrayList<>();

    private enum Mode { EVENTS, PROFILES, IMAGES, LOGS }
    private Mode currentMode = Mode.EVENTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();

        countEvents = findViewById(R.id.textEventCount);
        countUsers  = findViewById(R.id.textUserCount);
        countImages = findViewById(R.id.textImageCount);
        countLogs   = findViewById(R.id.textLogCount);

        sectionTitle   = findViewById(R.id.textSectionTitle);
        listContainer  = findViewById(R.id.adminListContainer);

        tabEvents   = findViewById(R.id.tabEvents);
        tabProfiles = findViewById(R.id.tabProfiles);
        tabImages   = findViewById(R.id.tabImages);
        tabLogs     = findViewById(R.id.tabLogs);

        buttonSearch = findViewById(R.id.buttonSearch);

        tabEvents.setOnClickListener(v -> switchMode(Mode.EVENTS));
        tabProfiles.setOnClickListener(v -> switchMode(Mode.PROFILES));
        tabImages.setOnClickListener(v -> switchMode(Mode.IMAGES));
        tabLogs.setOnClickListener(v -> switchMode(Mode.LOGS));

        buttonSearch.setOnClickListener(v -> showSearchDialog());

        // Initial load
        switchMode(Mode.EVENTS);
        refreshCounts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // When we return to this screen, refresh counts and the current list
        refreshCounts();
        switchMode(currentMode);
    }

    // ---------------------------------------------------------------------
    // MODE / TABS
    // ---------------------------------------------------------------------

    private void switchMode(Mode mode) {
        currentMode = mode;
        updateTabHighlight();

        switch (mode) {
            case EVENTS:
                sectionTitle.setText("Browse Events");
                loadEvents();
                break;
            case PROFILES:
                sectionTitle.setText("Browse Profiles");
                loadProfiles();
                break;
            case IMAGES:
                sectionTitle.setText("Browse Images");
                loadImagesPlaceholder();
                break;
            case LOGS:
                sectionTitle.setText("Activity Logs");
                loadLogs();
                break;
        }
    }

    private void updateTabHighlight() {
        float on = 1f;
        float off = 0.4f;

        tabEvents.setAlpha(currentMode == Mode.EVENTS ? on : off);
        tabProfiles.setAlpha(currentMode == Mode.PROFILES ? on : off);
        tabImages.setAlpha(currentMode == Mode.IMAGES ? on : off);
        tabLogs.setAlpha(currentMode == Mode.LOGS ? on : off);
    }

    private void refreshCounts() {
        db.collection("events").get()
                .addOnSuccessListener(snap ->
                        countEvents.setText(String.valueOf(snap.size())));

        db.collection("users").get()
                .addOnSuccessListener(snap ->
                        countUsers.setText(String.valueOf(snap.size())));

        // If there's no "images" collection yet, this will just show 0
        db.collection("images").get()
                .addOnSuccessListener(snap ->
                        countImages.setText(String.valueOf(snap.size())));

        db.collection("logs").get()
                .addOnSuccessListener(snap ->
                        countLogs.setText(String.valueOf(snap.size())));
    }

    // ---------------------------------------------------------------------
    // EVENTS TAB
    // ---------------------------------------------------------------------

    private void loadEvents() {
        db.collection("events")
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventDocs = new ArrayList<>(querySnapshot.getDocuments());
                    renderEventList(eventDocs);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading events: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void renderEventList(List<DocumentSnapshot> docs) {
        listContainer.removeAllViews();

        if (docs == null || docs.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No events found.");
            tv.setPadding(8, 16, 8, 16);
            listContainer.addView(tv);
            return;
        }

        for (DocumentSnapshot doc : docs) {
            addEventCard(doc);
        }
    }

    private void addEventCard(DocumentSnapshot doc) {
        View card = getLayoutInflater()
                .inflate(R.layout.item_admin_event, listContainer, false);

        TextView titleView     = card.findViewById(R.id.adminEventTitle);
        TextView statusView    = card.findViewById(R.id.adminEventStatus);
        TextView organizerView = card.findViewById(R.id.adminEventOrganizer);
        TextView dateView      = card.findViewById(R.id.adminEventDate);
        TextView entrantsView  = card.findViewById(R.id.adminEventEntrants);
        Button   removeButton  = card.findViewById(R.id.adminEventRemoveButton);

        String title = nz(doc.getString("title"));
        if (title.isEmpty()) title = nz(doc.getString("name"));

        String date = nz(doc.getString("date"));
        if (date.isEmpty()) date = nz(doc.getString("dateDisplay"));

        String organizer = nz(doc.getString("organizerName"));
        if (organizer.isEmpty()) organizer = nz(doc.getString("location"));

        // entrants from waitingList array if present
        List<String> waiting = (List<String>) doc.get("waitingList");
        int entrants = waiting == null ? 0 : waiting.size();

        titleView.setText(title);
        dateView.setText(date);
        organizerView.setText(organizer.isEmpty() ? "Unknown" : organizer);
        entrantsView.setText(String.valueOf(entrants));
        statusView.setText("Active");

        String eventId = doc.getId();
        String finalTitle = title;

        removeButton.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Remove Event")
                        .setMessage("Are you sure you want to remove \"" + finalTitle + "\"?")
                        .setPositiveButton("Remove", (dialog, which) ->
                                deleteEvent(eventId, finalTitle))
                        .setNegativeButton("Cancel", null)
                        .show());

        listContainer.addView(card);
    }

    private void deleteEvent(String eventId, String title) {
        db.collection("events").document(eventId)
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Event removed", Toast.LENGTH_SHORT).show();
                    ActivityLogger.logEventRemoved(title);
                    refreshCounts();
                    loadEvents();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to remove event: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ---------------------------------------------------------------------
    // PROFILES TAB
    // ---------------------------------------------------------------------

    private void loadProfiles() {
        db.collection("users")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    profileDocs = new ArrayList<>(snapshot.getDocuments());
                    renderProfileList(profileDocs);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading profiles: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void renderProfileList(List<DocumentSnapshot> docs) {
        listContainer.removeAllViews();

        if (docs == null || docs.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No profiles found.");
            tv.setPadding(8, 16, 8, 16);
            listContainer.addView(tv);
            return;
        }

        for (DocumentSnapshot doc : docs) {
            addProfileCard(doc);
        }
    }

    private void addProfileCard(DocumentSnapshot doc) {
        View card = getLayoutInflater()
                .inflate(R.layout.item_admin_profile, listContainer, false);

        TextView nameView  = card.findViewById(R.id.adminProfileName);
        TextView emailView = card.findViewById(R.id.adminProfileEmail);
        TextView roleView  = card.findViewById(R.id.adminProfileRole);
        Button   removeBtn = card.findViewById(R.id.adminProfileRemoveButton);

        String name  = nz(doc.getString("name"));
        String email = nz(doc.getString("email"));
        String role  = nz(doc.getString("role"));

        nameView.setText(name.isEmpty() ? "Unnamed user" : name);
        emailView.setText(email);
        roleView.setText(role.isEmpty() ? "Entrant" : capitalize(role));

        removeBtn.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Remove Profile")
                        .setMessage("Remove user \"" + email + "\"?")
                        .setPositiveButton("Remove", (dialog, which) ->
                                deleteProfile(doc.getId(), email))
                        .setNegativeButton("Cancel", null)
                        .show());

        listContainer.addView(card);
    }

    private void deleteProfile(String docId, String email) {
        db.collection("users").document(docId)
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Profile removed", Toast.LENGTH_SHORT).show();
                    ActivityLogger.logProfileRemoved(email);
                    refreshCounts();
                    loadProfiles();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to remove profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ---------------------------------------------------------------------
    // IMAGES TAB (placeholder)
    // ---------------------------------------------------------------------

    private void loadImagesPlaceholder() {
        listContainer.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("Image moderation coming soon.");
        tv.setTextSize(14f);
        tv.setTextColor(0xFF555555);
        tv.setPadding(8, 16, 8, 16);

        listContainer.addView(tv);
    }

    // ---------------------------------------------------------------------
    // LOGS TAB
    // ---------------------------------------------------------------------

    private void loadLogs() {
        db.collection("logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    countLogs.setText(String.valueOf(snapshot.size()));
                    logDocs = new ArrayList<>(snapshot.getDocuments());
                    renderLogList(logDocs);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading logs: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void renderLogList(List<DocumentSnapshot> docs) {
        listContainer.removeAllViews();

        if (docs == null || docs.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No activity yet.");
            tv.setPadding(8, 16, 8, 16);
            listContainer.addView(tv);
            return;
        }

        for (DocumentSnapshot doc : docs) {
            addLogCard(doc);
        }
    }

    private void addLogCard(DocumentSnapshot doc) {
        View card = getLayoutInflater()
                .inflate(R.layout.item_admin_log, listContainer, false);

        TextView titleView    = card.findViewById(R.id.adminLogTitle);
        TextView subtitleView = card.findViewById(R.id.adminLogSubtitle);
        TextView timeView     = card.findViewById(R.id.adminLogTime);

        String title   = nz(doc.getString("title"));
        String message = nz(doc.getString("message"));
        Timestamp ts   = doc.getTimestamp("timestamp");

        titleView.setText(title.isEmpty() ? "Log" : title);
        subtitleView.setText(message);

        if (ts != null) {
            timeView.setText(formatRelativeTime(ts.toDate()));
        } else {
            timeView.setText("");
        }

        listContainer.addView(card);
    }

    // ---------------------------------------------------------------------
    // SEARCH
    // ---------------------------------------------------------------------

    private void showSearchDialog() {
        if (currentMode == Mode.IMAGES) {
            Toast.makeText(this,
                    "Search is not available for images yet.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(true);

        switch (currentMode) {
            case EVENTS:
                input.setHint("Search events (title, location, category)...");
                break;
            case PROFILES:
                input.setHint("Search profiles (name, email, role)...");
                break;
            case LOGS:
                input.setHint("Search logs...");
                break;
        }

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);

        new AlertDialog.Builder(this)
                .setTitle("Search")
                .setView(input)
                .setPositiveButton("Search", (dialog, which) -> {
                    String q = input.getText().toString();
                    applySearch(q);
                })
                .setNegativeButton("Clear", (dialog, which) -> applySearch(""))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void applySearch(String query) {
        if (query == null) query = "";
        query = query.trim().toLowerCase();

        switch (currentMode) {
            case EVENTS: {
                if (query.isEmpty()) {
                    renderEventList(eventDocs);
                    return;
                }
                List<DocumentSnapshot> filtered = new ArrayList<>();
                for (DocumentSnapshot doc : eventDocs) {
                    String title = nz(doc.getString("title"));
                    if (title.isEmpty()) title = nz(doc.getString("name"));
                    String location = nz(doc.getString("location"));
                    String category = nz(doc.getString("category"));

                    if (title.toLowerCase().contains(query) ||
                            location.toLowerCase().contains(query) ||
                            category.toLowerCase().contains(query)) {
                        filtered.add(doc);
                    }
                }
                renderEventList(filtered);
                break;
            }
            case PROFILES: {
                if (query.isEmpty()) {
                    renderProfileList(profileDocs);
                    return;
                }
                List<DocumentSnapshot> filtered = new ArrayList<>();
                for (DocumentSnapshot doc : profileDocs) {
                    String name = nz(doc.getString("name"));
                    String email = nz(doc.getString("email"));
                    String role = nz(doc.getString("role"));

                    if (name.toLowerCase().contains(query) ||
                            email.toLowerCase().contains(query) ||
                            role.toLowerCase().contains(query)) {
                        filtered.add(doc);
                    }
                }
                renderProfileList(filtered);
                break;
            }
            case LOGS: {
                if (query.isEmpty()) {
                    renderLogList(logDocs);
                    return;
                }
                List<DocumentSnapshot> filtered = new ArrayList<>();
                for (DocumentSnapshot doc : logDocs) {
                    String title = nz(doc.getString("title"));
                    String message = nz(doc.getString("message"));
                    if (title.toLowerCase().contains(query) ||
                            message.toLowerCase().contains(query)) {
                        filtered.add(doc);
                    }
                }
                renderLogList(filtered);
                break;
            }
            case IMAGES:
                // we already early-return in showSearchDialog()
                break;
        }
    }

    // ---------------------------------------------------------------------
    // UTIL
    // ---------------------------------------------------------------------

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private String formatRelativeTime(Date date) {
        long diff = System.currentTimeMillis() - date.getTime();
        long minutes = diff / (60 * 1000);
        if (minutes < 60) {
            return minutes + " min ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " hours ago";
        }
        long days = hours / 24;
        return days + " days ago";
    }
}
