package com.example.aurora.activities;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
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
    private ImageButton btnBack;
    private TextView tvEventTitle;
    private TextView tvEventSubtitle;
    private TextView tvWaitingCount;
    private TextView tvSelectedCount;
    private TextView tvCancelledCount;
    private TextView tvTotalSpots;
    private Button btnNotify;
    private TextView tabWaiting;
    private TextView tabSelected;
    private TextView tabCancelled;
    private TextView tabFinal;
    private RecyclerView recyclerEntrants;
    private EntrantsAdapter entrantsAdapter;
    private String eventId;
    private ImageView imgEventPoster;
    private Button btnUpdatePoster;
    private Uri newPosterUri = null;
    private ActivityResultLauncher<Intent> posterPickerLauncher;
    private StorageReference posterStorageRef;
    private List<String> waitingEmails = new ArrayList<>();
    private List<String> selectedEmails = new ArrayList<>();
    private List<String> cancelledEmails = new ArrayList<>();
    private List<String> finalEmails = new ArrayList<>();
    private long maxSpots = 0L;
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

    /** Connects XML views to Java fields and sets back button listener. */
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
        tabWaiting = findViewById(R.id.tabWaiting);
        tabSelected = findViewById(R.id.tabSelected);
        tabCancelled = findViewById(R.id.tabCancelled);
        tabFinal = findViewById(R.id.tabFinal);
        recyclerEntrants = findViewById(R.id.recyclerEntrants);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }
    }

    /**
     * Sets up the RecyclerView and its listeners:
     *  selection listener for notify button behavior
     *  delete listener for removing entrants
     */
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

    /**
     * Configures logic for the Notify button based on current tab
     * (Waiting / Selected / Cancelled).
     */
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


    /** Sends a message to all waiting entrants. */
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


    /** Sends message only to the selected waiting entrants. */
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


    /** Sends a message to all selected entrants. */
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
                            "Notified all selected entrants.",
                            Toast.LENGTH_SHORT).show();
                });
    }


    /** Sends message only to selected entrants in the Selected tab. */
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


    /** Sends a message to all cancelled entrants. */
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


    /** Sends to selected entrants in Cancelled tab. */
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


    /**
     * Updates the Notify button text dynamically depending on:
     *  current tab
     *  number of selected entrants
     */
    private void updateNotifyButtonMode() {

        List<EntrantsAdapter.EntrantItem> selected = entrantsAdapter.getSelectedEntrants();

        if (selected.isEmpty()) {
            if (currentTab == Tab.WAITING) {
                btnNotify.setText("Notify All");
            } else if (currentTab == Tab.SELECTED) {
                btnNotify.setText("Notify All Selected");
            } else if (currentTab == Tab.CANCELLED) {
                btnNotify.setText("Notify All Cancelled");
            }

        } else {

            if (currentTab == Tab.WAITING) {
                btnNotify.setText("Notify Selected (" + selected.size() + ")");
            } else if (currentTab == Tab.SELECTED) {
                btnNotify.setText("Notify Selected (" + selected.size() + ")");
            } else if (currentTab == Tab.CANCELLED) {
                btnNotify.setText("Notify Selected (" + selected.size() + ")");
            }
        }
    }

    /** Sets up click listeners for all tabs. */
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

    /** Loads the event document then binds UI and lists. */
    private void loadEventAndLists() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::bindEventData)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Reads event data (title, lists, poster, stats) and updates the UI.
     * Also loads entrant lists.
     */
    private void bindEventData(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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

        String posterUrl = doc.getString("posterUrl");
        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(this)
                    .load(posterUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(imgEventPoster);
        }

        waitingEmails = (List<String>) doc.get("waitingList");
        if (waitingEmails == null) waitingEmails = new ArrayList<>();

        selectedEmails = (List<String>) doc.get("selectedEntrants");
        if (selectedEmails == null) selectedEmails = new ArrayList<>();

        cancelledEmails = (List<String>) doc.get("cancelledEntrants");
        if (cancelledEmails == null) cancelledEmails = new ArrayList<>();

        finalEmails = (List<String>) doc.get("finalEntrants");
        if (finalEmails == null) finalEmails = new ArrayList<>();

        Long max = doc.getLong("maxSpots");
        if (max == null) max = 0L;
        maxSpots = max;

        tvWaitingCount.setText(String.valueOf(waitingEmails.size()));
        tvSelectedCount.setText(String.valueOf(selectedEmails.size()));
        tvCancelledCount.setText(String.valueOf(cancelledEmails.size()));
        tvTotalSpots.setText(String.valueOf(maxSpots));

        setActiveTab(Tab.WAITING);
    }

    /**
     * Switches to the selected tab and loads its entrant list.
     */
    private void setActiveTab(Tab tab) {
        currentTab = tab;
        resetTabStyles();

        btnNotify.setVisibility(View.VISIBLE);

        switch (tab) {
            case WAITING:
                // Highlight with Light Blue (#29B6F6)
                highlightTab(tabWaiting, Color.parseColor("#29B6F6"));
                loadEntrantsForEmails(waitingEmails, "Waiting");
                btnNotify.setText("Notify All");
                break;

            case SELECTED:
                // Highlight with Green (#66BB6A)
                highlightTab(tabSelected, Color.parseColor("#66BB6A"));
                loadEntrantsForEmails(selectedEmails, "Selected");
                btnNotify.setText("Notify All Selected");
                break;

            case CANCELLED:
                // Highlight with Red (#EF5350)
                highlightTab(tabCancelled, Color.parseColor("#EF5350"));
                loadEntrantsForEmails(cancelledEmails, "Cancelled");
                btnNotify.setText("Notify All Cancelled");
                break;

            case FINAL:
                // Highlight with Orange (#fe7f2d)
                highlightTab(tabFinal, Color.parseColor("#fe7f2d"));
                loadEntrantsForEmails(finalEmails, "Final");
                btnNotify.setVisibility(View.GONE);
                break;
        }

        updateNotifyButtonMode();
    }

    /** Resets all tab styles to inactive mode. */
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

    /** Highlights the selected tab visually. */
    private void highlightTab(TextView tab, int textColor) {
        tab.setTextColor(textColor);
        tab.setBackgroundResource(R.drawable.bg_input_sharp);
        tab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        tab.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    /**
     * Loads entrant data for a list of emails.
     * Looks up name from users collection and adds to the RecyclerView.
     */
    private void loadEntrantsForEmails(List<String> emails, String statusLabel) {
        entrantsAdapter.clearItems();

        if (emails == null || emails.isEmpty()) {
            Toast.makeText(this, "No entrants in this list yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String email : emails) {
            if (email == null || email.isEmpty()) continue;

            db.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snap -> {
                        String name = email;

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

    /** Sets up ActivityResultLauncher for poster image picking. */
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

    /** Opens gallery to pick a new event poster image. */
    private void openPosterPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        posterPickerLauncher.launch(
                Intent.createChooser(intent, "Select New Poster")
        );
    }

    /**
     * Uploads new poster image to Firebase Storage
     * and updates event document with new URL.
     */
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

    /** Callback interface for custom notification messages. */
    private interface CustomMessageCallback {
        void onMessage(String msg);
    }

    /**
     * Shows dialog for typing a custom message.
     * Returns message through callback when "Send" pressed.
     */
    private void showCustomMessageDialog(CustomMessageCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_send_message, null);

        EditText input = view.findViewById(R.id.dialogInput);
        TextView btnCancel = view.findViewById(R.id.dialogCancel);
        TextView btnSend = view.findViewById(R.id.dialogSend);

        AlertDialog dialog = builder.setView(view).create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSend.setOnClickListener(v -> {
            String msg = input.getText().toString().trim();
            if (msg.isEmpty()) {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            callback.onMessage(msg);
            dialog.dismiss();
        });

        dialog.show();
    }


    /**
     * Exports the FINAL entrants list as a CSV file.
     */
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

    /**
     * Fetches name/phone for each email in final list
     * and builds CSV string.
     */
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

    /**
     * Saves built CSV content into Downloads folder.
     */
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