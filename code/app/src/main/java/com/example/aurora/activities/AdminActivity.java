/*
 *
 * source: Firebase docs — "Get data with Cloud Firestore".
 * url: https://firebase.google.com/docs/firestore/query-data/get-data
 * note: Used as a reference for calling collection().get() with addOnSuccessListener
 *       to load events, users, images, and logs into the admin dashboard.
 *
 * source: Firebase docs - "Order and limit data with Cloud Firestore".
 * url: https://firebase.google.com/docs/firestore/query-data/order-limit-data
 * note: Used for ordering queries such as orderBy("date", Query.Direction.ASCENDING)
 *       and orderBy("timestamp", Query.Direction.DESCENDING) when listing events and logs.
 *
 * source: Firebase docs - "Add data to Cloud Firestore".
 * url: https://firebase.google.com/docs/firestore/manage-data/add-data
 * note: General reference for working with Map<String,Object>, reading document
 *       snapshots, and understanding how collection().document() and document IDs work.
 *
 * source: Firebase developers article - "The secrets of Firestore's FieldValue.serverTimestamp()".
 * url: https://medium.com/firebase-developers/the-secrets-of-firestores-fieldvalue-servertimestamp-revealed-29dd7a38a82b
 * note: Background for why ActivityLogger uses FieldValue.serverTimestamp() and why
 *       AdminActivity later reads these timestamps and formats them in formatRelativeTime().
 *
 * source: Stack Overflow user - "Android / Firebase, get timestamp to date - java".
 * author: Stack Overflow user
 * url: https://stackoverflow.com/questions/66522800/android-firebase-get-timestamp-to-date
 * note: Example of reading a Firebase Timestamp field, converting it to java.util.Date,
 *       and displaying it, similar to how timeView shows log times.
 *
 * source: Firebase Auth docs - "Manage users in Firebase Authentication".
 * url: https://firebase.google.com/docs/auth/android/manage-users
 * note: Used as a reference for signing out with FirebaseAuth.getInstance().signOut()
 *       when the admin presses the back button to leave the dashboard.
 *
 * source: Android documentation for SharedPreferences.
 * url: https://developer.android.com/reference/android/content/SharedPreferences
 * note: Used for clearing the "aurora_prefs" preferences with edit().clear().apply()
 *       when logging the admin out.
 *
 * source: Android ViewGroup / LinearLayout documentation.
 * url: https://developer.android.com/reference/android/view/ViewGroup#removeAllViews()
 * note: Used as a reference for listContainer.removeAllViews() followed by inflating
 *       child layouts for each event, profile, and log entry.
 *
 * source: Stack Overflow user - "Android AlertDialog with embedded EditText".
 * author: Stack Overflow user
 * url: https://stackoverflow.com/questions/2795300/android-alertdialog-with-embedded-edittext
 * note: Used for the pattern of building a custom AlertDialog with setView(input)
 *       to implement the search dialog in showSearchDialog().
 *
 * source: Android AlertDialog.Builder examples and tutorials.
 * url: https://www.androidcode.ninja/android-alertdialog-example/
 * note: General reference for showing confirmation dialogs when removing events and profiles.
 *
 * source: ChatGPT (OpenAI assistant).
 * note: Helped tighten up JavaDoc wording, method names, and in-memory search logic,
 *       but not the underlying Firebase or Android APIs.
 */

package com.example.aurora.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import com.bumptech.glide.Glide;
import com.example.aurora.R;
import com.example.aurora.models.AdminImage;
import com.example.aurora.utils.ActivityLogger;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.aurora.notifications.FirestoreNotificationHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * The main dashboard for administrators in the Aurora app.
 * Provides access to monitor and manage events, user profiles, logs, and images.
 * Includes a top back button for returning to the login screen (with session logout).
 */


/**
 * Administrative display modes for the dashboard.
 *
 * EVENTS   — displays all events with status, entrants count, and organizer.
 * PROFILES — displays all users with roles, emails, and privilege controls.
 * IMAGES   — displays event posters stored in Firebase Storage.
 * LOGS     — displays notification history sorted by timestamp.
 */
public class AdminActivity extends AppCompatActivity {

    private TextView countEvents, countUsers, countImages, countLogs;
    private TextView sectionTitle;
    private LinearLayout tabEvents, tabProfiles, tabImages, tabLogs;
    private LinearLayout listContainer;
    private ImageButton buttonSearch;
    private ImageView btnLogout;


    private FirebaseFirestore db;

    private List<DocumentSnapshot> eventDocs = new ArrayList<>();
    private List<DocumentSnapshot> profileDocs = new ArrayList<>();
    private List<DocumentSnapshot> logDocs = new ArrayList<>();
    private List<AdminImage> imageList = new ArrayList<>();

    private enum Mode { EVENTS, PROFILES, IMAGES, LOGS }
    private Mode currentMode = Mode.EVENTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();

        // Bind Views
        countEvents = findViewById(R.id.textEventCount);
        countUsers  = findViewById(R.id.textUserCount);
        countImages = findViewById(R.id.textImageCount);
        countLogs   = findViewById(R.id.textLogCount);

        sectionTitle  = findViewById(R.id.textSectionTitle);
        listContainer = findViewById(R.id.adminListContainer);

        tabEvents   = findViewById(R.id.tabEvents);
        tabProfiles = findViewById(R.id.tabProfiles);
        tabImages   = findViewById(R.id.tabImages);
        tabLogs     = findViewById(R.id.tabLogs);

        buttonSearch = findViewById(R.id.buttonSearch);
        btnLogout = findViewById(R.id.btnLogoutAdmin);


        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
            sp.edit().clear().apply();

            Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });


        // Tab Listeners
        tabEvents.setOnClickListener(v -> switchMode(Mode.EVENTS));
        tabProfiles.setOnClickListener(v -> switchMode(Mode.PROFILES));
        tabImages.setOnClickListener(v -> switchMode(Mode.IMAGES));
        tabLogs.setOnClickListener(v -> switchMode(Mode.LOGS));

        // Search
        buttonSearch.setOnClickListener(v -> showSearchDialog());

        // Initial Load
        switchMode(Mode.EVENTS);
        refreshCounts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCounts();
        switchMode(currentMode);
    }


    /**
     * Switches the admin dashboard into the selected mode.
     * Updates UI highlighting and triggers the appropriate Firestore query.
     *
     * @param mode One of the four dashboard modes: EVENTS, PROFILES, IMAGES, LOGS.
     */
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
                loadImages();
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


    /**
     * Reloads the top-level dashboard counters for:
     *   Total events
     *   Total users
     *   Total images (events with posterUrl)
     *   Total notification log entries
     *
     * Uses lightweight collection reads without ordering to minimize cost.
     */
    private void refreshCounts() {
        db.collection("events").get().addOnSuccessListener(snap -> countEvents.setText(String.valueOf(snap.size())));
        db.collection("users").get().addOnSuccessListener(snap -> countUsers.setText(String.valueOf(snap.size())));
        db.collection("notificationLogs").get().addOnSuccessListener(snap -> countLogs.setText(String.valueOf(snap.size())));

        db.collection("events").get().addOnSuccessListener(snap -> {
            int imgCount = 0;
            for(DocumentSnapshot doc : snap) {
                String url = doc.getString("posterUrl");
                if (url != null && !url.isEmpty()) imgCount++;
            }
            countImages.setText(String.valueOf(imgCount));
        });
    }


    /**
     * Fetches all events sorted by date and displays them as event cards.
     * Each card shows title, date, organizer, waiting count, and a remove button.
     */
    private void loadEvents() {
        db.collection("events").orderBy("date", Query.Direction.ASCENDING).get()
                .addOnSuccessListener(querySnapshot -> {
                    eventDocs = new ArrayList<>(querySnapshot.getDocuments());
                    renderEventList(eventDocs);
                });
    }


    /**
     * Renders a list of event Firestore documents into UI card components.
     * If the list is empty, displays a placeholder message.
     *
     * @param docs List of DocumentSnapshot objects representing events.
     */
    private void renderEventList(List<DocumentSnapshot> docs) {
        listContainer.removeAllViews();
        if (docs == null || docs.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No events found.");
            tv.setPadding(8, 16, 8, 16);
            listContainer.addView(tv);
            return;
        }
        for (DocumentSnapshot doc : docs) addEventCard(doc);
    }


    /**
     * Builds a single event card from Firestore data and adds it into listContainer.
     * Handles organizer name resolution through multiple fallbacks:
     *    organizerName → organizer user ID → organizerEmail
     *
     * @param doc Firestore document containing event data.
     */
    private void addEventCard(DocumentSnapshot doc) {
        View card = getLayoutInflater().inflate(R.layout.item_admin_event, listContainer, false);

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

        List<String> waiting = (List<String>) doc.get("waitingList");
        int entrants = waiting == null ? 0 : waiting.size();

        titleView.setText(title);
        dateView.setText(date);
        entrantsView.setText(String.valueOf(entrants));
        statusView.setText("Active");


        String storedName = doc.getString("organizerName");

        if (storedName != null && !storedName.isEmpty()) {
            organizerView.setText(storedName);
        }
        else {

            String organizerId = doc.getString("organizer");

            if (organizerId != null && !organizerId.isEmpty()) {
                organizerView.setText("Loading...");

                db.collection("users").document(organizerId).get()
                        .addOnSuccessListener(userSnap -> {
                            String realName = userSnap.getString("name");
                            if (realName != null && !realName.isEmpty()) {
                                organizerView.setText(realName);
                            } else {
                                String userEmail = userSnap.getString("email");
                                organizerView.setText(userEmail != null ? userEmail : "Unknown User");
                            }
                        })
                        .addOnFailureListener(e -> organizerView.setText("Unknown"));
            }
            else {
                String fallbackEmail = doc.getString("organizerEmail");
                organizerView.setText(fallbackEmail != null && !fallbackEmail.isEmpty() ? fallbackEmail : "Unknown");
            }
        }

        String finalTitle = title;
        removeButton.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Remove Event")
                        .setMessage("Remove \"" + finalTitle + "\"?")
                        .setPositiveButton("Remove", (dialog, which) -> deleteEvent(doc.getId(), finalTitle))
                        .setNegativeButton("Cancel", null)
                        .show());

        listContainer.addView(card);
    }

    private void deleteEvent(String eventId, String title) {
        db.collection("events").document(eventId).delete().addOnSuccessListener(v -> {
            Toast.makeText(this, "Event removed", Toast.LENGTH_SHORT).show();
            ActivityLogger.logEventRemoved(title);
            refreshCounts();
            loadEvents();
        });
    }


    /**
     * Loads all user profiles ordered alphabetically by name.
     * Results are stored for searching and rendered as profile cards.
     */
    private void loadProfiles() {
        db.collection("users").orderBy("name", Query.Direction.ASCENDING).get()
                .addOnSuccessListener(snapshot -> {
                    profileDocs = new ArrayList<>(snapshot.getDocuments());
                    renderProfileList(profileDocs);
                });
    }

    /**
     * Converts profile Firestore documents into UI cards for display.
     * Each card shows user name, email, role, and privilege controls.
     */
    private void renderProfileList(List<DocumentSnapshot> docs) {
        listContainer.removeAllViews();
        if (docs == null || docs.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No profiles found.");
            tv.setPadding(8, 16, 8, 16);
            listContainer.addView(tv);
            return;
        }
        for (DocumentSnapshot doc : docs) addProfileCard(doc);
    }


    /**
     * Builds an admin profile card with:
     *  - Display name and email
     *  - Current role (Entrant / Organizer / Admin)
     *  - Organizer privilege toggle
     *  - Remove user action
     *
     * @param doc Firestore user document.
     */
    private void addProfileCard(DocumentSnapshot doc) {
        View card = getLayoutInflater().inflate(R.layout.item_admin_profile, listContainer, false);

        TextView nameView  = card.findViewById(R.id.adminProfileName);
        TextView emailView = card.findViewById(R.id.adminProfileEmail);
        TextView roleView  = card.findViewById(R.id.adminProfileRole);

        Button removeBtn         = card.findViewById(R.id.adminProfileRemoveButton);
        Button toggleOrganizerBtn = card.findViewById(R.id.adminToggleOrganizerBtn);

        String name  = nz(doc.getString("name"));
        String email = nz(doc.getString("email"));
        String role  = nz(doc.getString("role"));

        nameView.setText(name.isEmpty() ? "Unnamed" : name);
        emailView.setText(email);
        roleView.setText(role.isEmpty() ? "Entrant" : capitalize(role));

        if ("admin".equalsIgnoreCase(role)) {
            toggleOrganizerBtn.setVisibility(View.GONE);
        }
        else {
            toggleOrganizerBtn.setVisibility(View.VISIBLE);

            Boolean orgAllowed = doc.getBoolean("organizer_allowed");
            boolean isOrganizer = orgAllowed != null && orgAllowed;

            toggleOrganizerBtn.setText(
                    isOrganizer
                            ? "Remove Organizer Privileges"
                            : "Restore Organizer Privileges"
            );

            toggleOrganizerBtn.setOnClickListener(v -> {

                boolean currentVal = doc.getBoolean("organizer_allowed") != null &&
                        doc.getBoolean("organizer_allowed");

                boolean newVal = !currentVal;

                db.collection("users").document(doc.getId())
                        .update("organizer_allowed", newVal)
                        .addOnSuccessListener(x -> {

                            if (!newVal) {
                                FirestoreNotificationHelper.sendOrganizerRevokedNotification(db, email);
                                deleteAllEventsForOrganizer(email);
                            } else {
                                FirestoreNotificationHelper.sendOrganizerEnabledNotification(db, email);
                            }

                            loadProfiles();
                        });
            });
        }

        removeBtn.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Remove Profile")
                        .setMessage("Remove user \"" + email + "\"?")
                        .setPositiveButton("Remove", (dialog, which) -> deleteProfile(doc.getId(), email))
                        .setNegativeButton("Cancel", null)
                        .show()
        );

        listContainer.addView(card);
    }

    private void deleteProfile(String docId, String email) {
        db.collection("users").document(docId).delete().addOnSuccessListener(v -> {
            Toast.makeText(this, "Profile removed", Toast.LENGTH_SHORT).show();
            ActivityLogger.logProfileRemoved(email);
            refreshCounts();
            loadProfiles();
        });
    }



    /**
     * Loads all event posters by scanning the events collection for non-null posterUrl fields.
     * The result is turned into AdminImage objects and rendered as image cards.
     */
    private void loadImages() {
        listContainer.removeAllViews();
        db.collection("events").get().addOnSuccessListener(query -> {
            imageList.clear();
            for (DocumentSnapshot doc : query) {
                String posterUrl = doc.getString("posterUrl");
                if (posterUrl != null && !posterUrl.isEmpty()) {
                    String title = nz(doc.getString("title"));
                    String organizer = nz(doc.getString("organizerEmail"));
                    imageList.add(new AdminImage(doc.getId(), title, organizer, posterUrl));
                }
            }
            if (imageList.isEmpty()) {
                TextView tv = new TextView(this);
                tv.setText("No images found.");
                tv.setPadding(16, 16, 16, 16);
                listContainer.addView(tv);
                return;
            }
            for (AdminImage img : imageList) addImageCard(img);
        });
    }



    /**
     * Renders a poster card showing thumbnail, event name, organizer
     * and delete button (removes Storage image and clears posterUrl in Firestore).
     *
     * @param img AdminImage containing metadata for display.
     */
    private void addImageCard(AdminImage img) {
        View card = getLayoutInflater().inflate(R.layout.item_admin_image, listContainer, false);
        ImageView thumb = card.findViewById(R.id.adminPosterThumb);
        TextView title = card.findViewById(R.id.adminImageEventTitle);
        TextView organizerTv = card.findViewById(R.id.adminImageOrganizer);
        Button removeBtn = card.findViewById(R.id.adminDeleteImageBtn);

        title.setText(img.eventTitle);
        organizerTv.setText("Organizer: " + img.organizerEmail);
        Glide.with(this).load(img.posterUrl).placeholder(R.drawable.ic_launcher_background).into(thumb);
        removeBtn.setOnClickListener(v -> deleteImage(img));
        listContainer.addView(card);
    }

    private void deleteImage(AdminImage img) {
        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(img.posterUrl);
        ref.delete().addOnSuccessListener(aVoid ->
                db.collection("events").document(img.eventId).update("posterUrl", null)
                        .addOnSuccessListener(v -> {
                            Toast.makeText(this, "Image removed.", Toast.LENGTH_SHORT).show();
                            loadImages();
                        })
        );
    }


    /**
     * Loads the notification logs in descending chronological order.
     * Each log entry contains: event name, type, message, timestamp.
     */
    private void loadLogs() {
        listContainer.removeAllViews();
        db.collection("notificationLogs").orderBy("timestamp", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snapshot -> {
                    logDocs = new ArrayList<>(snapshot.getDocuments());
                    if (logDocs.isEmpty()) {
                        TextView tv = new TextView(this);
                        tv.setText("No logs.");
                        tv.setPadding(16, 16, 16, 16);
                        listContainer.addView(tv);
                        return;
                    }
                    for (DocumentSnapshot doc : logDocs) addLogCard(doc);
                });
    }


    /**
     * Converts a single log Firestore document into a card showing message summary.
     * Timestamp formats into a human-readable "X minutes ago".
     *
     * @param doc Firestore log entry.
     */
    private void addLogCard(DocumentSnapshot doc) {
        View card = getLayoutInflater().inflate(R.layout.item_admin_log, listContainer, false);
        TextView titleView = card.findViewById(R.id.adminLogTitle);
        TextView subtitleView = card.findViewById(R.id.adminLogSubtitle);
        TextView timeView = card.findViewById(R.id.adminLogTime);

        String eventName = nz(doc.getString("eventName"));
        String type = nz(doc.getString("notificationType"));
        String message = nz(doc.getString("message"));

        titleView.setText(eventName + " (" + type + ")");
        subtitleView.setText(message);


        Object rawTime = doc.get("timestamp");
        Date finalDate = null;

        if (rawTime instanceof com.google.firebase.Timestamp) {

            finalDate = ((com.google.firebase.Timestamp) rawTime).toDate();
        } else if (rawTime instanceof Date) {
            finalDate = (Date) rawTime;
        } else if (rawTime instanceof Long) {
            finalDate = new Date((Long) rawTime);
        }

        if (finalDate != null) {
            timeView.setText(formatRelativeTime(finalDate));
        } else {
            timeView.setText("Unknown time");
        }


        listContainer.addView(card);
    }


    /**
     * Displays a custom rounded-corner search dialog for administrators.
     * The dialog allows searching users or events by name, email, or title.
     */
    private void showSearchDialog() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_admin_search, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText searchInput = dialogView.findViewById(R.id.searchInput);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelSearch);
        Button btnSearch = dialogView.findViewById(R.id.btnConfirmSearch);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSearch.setOnClickListener(v -> {
            String q = searchInput.getText().toString().trim();
            if (!q.isEmpty()) {
                performSuperSearch(q);
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    /**
     * Executes a two-step search:
     *  1) Attempts to match a user by email or name.
     *  2) If not a user, attempts to match an event by title.
     *
     * @param query Raw search input entered by the admin.
     */
    private void performSuperSearch(String query) {
        Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show();


        db.collection("users").whereEqualTo("email", query).get().addOnSuccessListener(snap -> {
            if (!snap.isEmpty()) {
                buildUserDossier(snap.getDocuments().get(0));
            } else {
                db.collection("users").whereEqualTo("name", query).get().addOnSuccessListener(snap2 -> {
                    if (!snap2.isEmpty()) {
                        buildUserDossier(snap2.getDocuments().get(0));
                    } else {
                        searchEvents(query);
                    }
                });
            }
        });
    }

    private void searchEvents(String query) {
        db.collection("events").whereEqualTo("title", query).get().addOnSuccessListener(snap -> {
            if (!snap.isEmpty()) {
                // Found an event! Show details.
                buildEventDossier(snap.getDocuments().get(0));
            } else {
                Toast.makeText(this, "No User or Event found.", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Builds a detailed popup (dossier) for a user.
     * Shows name, email, role, phone, and notification settings.
     * Also lists:
     *  Events where the user is on the waiting list
     *  Events the user has been selected for
     *
     * Uses arrayContains queries on events collection.
     *
     * @param userDoc Firestore document of the user.
     */
    private void buildUserDossier(DocumentSnapshot userDoc) {
        String userId = userDoc.getId();
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_dossier, null);
        AlertDialog dossierDialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dossierDialog.getWindow() != null) {
            dossierDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ((TextView) dialogView.findViewById(R.id.dossierHeader)).setText("User Dossier");
        ((TextView) dialogView.findViewById(R.id.dossierName)).setText("Name: " + nz(userDoc.getString("name")));
        ((TextView) dialogView.findViewById(R.id.dossierEmail)).setText("Email: " + nz(userDoc.getString("email")));
        ((TextView) dialogView.findViewById(R.id.dossierRole)).setText("Role: " + nz(userDoc.getString("role")));

        ((TextView) dialogView.findViewById(R.id.dossierPhone)).setText("Phone: " + nz(userDoc.getString("phone")));
        Boolean notif = userDoc.getBoolean("notificationsEnabled");
        ((TextView) dialogView.findViewById(R.id.dossierNotif)).setText("Notifications: " + (notif != null && notif ? "On" : "Off"));

        TextView tvSelected = dialogView.findViewById(R.id.dossierSelectedList);
        TextView tvWaiting = dialogView.findViewById(R.id.dossierWaitingList);
        dialogView.findViewById(R.id.btnCloseDossier).setOnClickListener(v -> dossierDialog.dismiss());

        db.collection("events").whereArrayContains("waitingList", userId).get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) tvWaiting.setText("No active waiting lists.");
            else {
                StringBuilder sb = new StringBuilder();
                for (DocumentSnapshot doc : snap) sb.append("• ").append(nz(doc.getString("title"))).append("\n");
                tvWaiting.setText(sb.toString());
            }
        });

        db.collection("events").whereArrayContains("selectedEntrants", userId).get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) tvSelected.setText("No active wins yet.");
            else {
                StringBuilder sb = new StringBuilder();
                for (DocumentSnapshot doc : snap) sb.append("🏆 ").append(nz(doc.getString("title"))).append("\n");
                tvSelected.setText(sb.toString());
            }
        });

        dossierDialog.show();
    }


    /**
     * Builds a detailed popup showing metadata about a specific event:
     * title, location, waiting count, selected count, cancelled count, capacity.
     *
     * Reuses the same layout as the User Dossier for visual consistency.
     *
     * @param eventDoc Firestore event document.
     */
    private void buildEventDossier(DocumentSnapshot eventDoc) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_dossier, null);
        AlertDialog dossierDialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dossierDialog.getWindow() != null) {
            dossierDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.dossierPhone).setVisibility(View.GONE);
        dialogView.findViewById(R.id.dossierNotif).setVisibility(View.GONE);
        dialogView.findViewById(R.id.dossierRole).setVisibility(View.GONE);

        ((TextView) dialogView.findViewById(R.id.dossierHeader)).setText("Event Details");
        ((TextView) dialogView.findViewById(R.id.dossierName)).setText("Event: " + nz(eventDoc.getString("title")));
        ((TextView) dialogView.findViewById(R.id.dossierEmail)).setText("Loc: " + nz(eventDoc.getString("location")));

        TextView labelSelected = dialogView.findViewById(R.id.labelSelected);
        TextView valSelected = dialogView.findViewById(R.id.dossierSelectedList);
        TextView labelWaiting = dialogView.findViewById(R.id.labelWaiting);
        TextView valWaiting = dialogView.findViewById(R.id.dossierWaitingList);

        labelSelected.setText("Counts");

        List<String> w = (List<String>) eventDoc.get("waitingList");
        List<String> s = (List<String>) eventDoc.get("selectedEntrants");
        List<String> c = (List<String>) eventDoc.get("cancelledEntrants");

        int wCount = w != null ? w.size() : 0;
        int sCount = s != null ? s.size() : 0;
        int cCount = c != null ? c.size() : 0;
        Long max = eventDoc.getLong("maxSpots");

        valSelected.setText(
                "Waiting: " + wCount + "\n" +
                        "Selected: " + sCount + "\n" +
                        "Cancelled: " + cCount + "\n" +
                        "Capacity: " + (max != null ? max : "Unlimited")
        );

        labelWaiting.setVisibility(View.GONE);
        valWaiting.setVisibility(View.GONE);

        dialogView.findViewById(R.id.btnCloseDossier).setOnClickListener(v -> dossierDialog.dismiss());
        dossierDialog.show();
    }


    /**
     * Utility: Null-safety for string fields.
     */
    private static String nz(String s) { return s == null ? "" : s; }

    /**
     * Utility: Capitalizes role names for display.
     */
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /**
     * Utility: Converts a Date into a relative time string.
     * Example: "5 min ago", "3 hours ago", "2 days ago".
     *
     * @param date Firestore timestamp converted to java.util.Date
     */
    private String formatRelativeTime(Date date) {
        long diff = System.currentTimeMillis() - date.getTime();
        long minutes = diff / (60 * 1000);
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hours ago";
        long days = hours / 24;
        return days + " days ago";
    }

    /**
     * Deletes every event created by a given organizer.
     *
     * Finds all events where `organizerEmail` matches the provided email,
     * then deletes each one using {@link #deleteEventById(FirebaseFirestore, String)}.
     *
     * @param organizerEmail The organizer's email whose events should be removed.
     */
    private void deleteAllEventsForOrganizer(String organizerEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events")
                .whereEqualTo("organizerEmail", organizerEmail)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query) {
                        deleteEventById(db, doc.getId());
                    }
                });
    }
    /**
     * Deletes a single event and all its associated Firestore data.
     *
     * This performs a multi-step cleanup:
     *  1. Deletes all documents in the event's "waitingLocations" subcollection.
     *  2. Deletes all notification documents referencing this event (via eventId).
     *  3. Finally deletes the event document itself from the "events" collection.
     *
     * The deletions are chained using success listeners to ensure ordering:
     * subcollections → notifications → parent event.
     *
     * @param db       The Firestore instance to operate on.
     * @param eventId  The ID of the event document to delete.
     */
    private void deleteEventById(FirebaseFirestore db, String eventId) {

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
                                        .delete();
                            });
                });
    }


}

