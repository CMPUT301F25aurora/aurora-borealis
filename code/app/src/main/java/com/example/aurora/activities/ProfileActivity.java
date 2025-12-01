/*
 * References for this screen:
 *
 * 1) source: Firebase docs — "Get data with Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/query-data/get-data
 *    Used for loading the current user's profile data.
 *
 * 2) source: Firebase docs — "Add data to Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/manage-data/add-data
 *    Used for saving edits to the user's profile fields.
 *
 * 3) author: Stack Overflow user — "How to get data from Firestore"
 *    https://stackoverflow.com/questions/72769031/how-to-retrieve-data-from-firestore
 *    Used as a reminder of the collection / document structure when reading profile info.
 */

package com.example.aurora.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Entrant profile screen.
 * Shows profile data loaded from Firestore (by email).
 * Allows editing/saving name/email/phone.
 * Has a back button.
 * Allows the entrant to delete their own account (with confirmation).
 * Has a logout button that fully clears session and returns to LoginActivity.
 */
public class ProfileActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private DocumentReference userRef;
    private ImageView avatar;
    private ImageView backButton;
    private TextView roleBadge, headerName, joinedCount, winsCount, editToggle;
    private EditText fullName, email, phone;
    private Button btnSave, btnEventHistory, btnNotifSettings, btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_profile);

        db = FirebaseFirestore.getInstance();

        backButton       = findViewById(R.id.backButtonProfile);
        roleBadge        = findViewById(R.id.roleBadge);
        headerName       = findViewById(R.id.headerName);
        joinedCount      = findViewById(R.id.joinedCount);
        winsCount        = findViewById(R.id.winsCount);
        editToggle       = findViewById(R.id.editToggle);
        fullName         = findViewById(R.id.inputFullName);
        email            = findViewById(R.id.inputEmail);
        phone            = findViewById(R.id.inputPhone);
        btnSave          = findViewById(R.id.btnSave);
        btnEventHistory  = findViewById(R.id.btnEventHistory);
        btnNotifSettings = findViewById(R.id.btnNotifSettings);
        btnDelete        = findViewById(R.id.btnDeleteAccount);

        backButton.setOnClickListener(v -> onBackPressed());

        setEditing(false);
        resolveAndLoad();

        editToggle.setOnClickListener(v -> setEditing(true));

        btnSave.setOnClickListener(v -> saveProfile());

        btnEventHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantEventHistoryActivity.class);
            startActivity(intent);
        });

        btnNotifSettings.setOnClickListener(v -> showNotifSettingsDialog());

        btnDelete.setOnClickListener(v -> showDeleteDialog());
    }

    /**
     * Resolve current user by email stored in SharedPreferences and load their profile doc.
     * If user does not exist anymore in Firestore → force logout.
     */
    private void resolveAndLoad() {
        String savedEmail = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_email", null);

        if (!TextUtils.isEmpty(savedEmail)) {
            queryByEmail(savedEmail);
        } else {
            Toast.makeText(this, "No logged-in user found", Toast.LENGTH_SHORT).show();
            performLogout();
        }
    }

    /**
     * Queries Firestore for a user document matching the given email.
     * If found, stores its reference and loads profile data.
     * If missing, clears session and returns user to login.
     */
    private void queryByEmail(String em) {
        Query q = db.collection("users")
                .whereEqualTo("email", em)
                .limit(1);

        q.get().addOnSuccessListener(snap -> {
            if (!snap.isEmpty()) {
                userRef = snap.getDocuments().get(0).getReference();
                loadProfile();
            } else {
                Toast.makeText(this, "User no longer exists. Please log in again.", Toast.LENGTH_SHORT).show();

                getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply();

                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Loads profile fields such as name, email, phone, role,
     * joinedCount, winsCount, and notification settings.
     * Updates the UI accordingly.
     */
    private void loadProfile() {
        userRef.get().addOnSuccessListener(doc -> {

            String n    = doc.getString("name");
            String e    = doc.getString("email");
            String p    = doc.getString("phone");
            String role = doc.getString("role");

            fullName.setText(n == null ? "" : n);
            email.setText(e == null ? "" : e);
            phone.setText(p == null ? "" : p);

            headerName.setText(TextUtils.isEmpty(n) ? "Entrant" : n);
            roleBadge.setText(TextUtils.isEmpty(role) ? "Entrant" : role);
            setAvatarInitials(fullName.getText().toString());

            Boolean notifs = doc.getBoolean("entrant_notifications_enabled");
            if (notifs != null) {
                getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("entrant_notifications_enabled", notifs)
                        .apply();
            }


            String myEmail = email.getText().toString().trim();
            if (!TextUtils.isEmpty(myEmail)) {
                calculateStats(myEmail);
            }

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
        );
    }
    /**
     * Helper to count events where the user is joined or has won.
     */
    private void calculateStats(String email) {
        db.collection("events").get().addOnSuccessListener(snap -> {
            int joined = 0;
            int wins = 0;

            for (DocumentSnapshot event : snap) {
                List<String> waiting = (List<String>) event.get("waitingList");
                List<String> selected = (List<String>) event.get("selectedEntrants");
                List<String> finalEntrants = (List<String>) event.get("finalEntrants");
                List<String> accepted = (List<String>) event.get("acceptedEntrants");


                if (waiting != null && waiting.contains(email)) {
                    joined++;
                }


                if ((selected != null && selected.contains(email)) ||
                        (accepted != null && accepted.contains(email)) ||
                        (finalEntrants != null && finalEntrants.contains(email))) {
                    wins++;
                }
            }

            joinedCount.setText(String.valueOf(joined));
            winsCount.setText(String.valueOf(wins));
        });
    }

    /**
     * Saves edited name, email, and phone to Firestore.
     * Validates phone number, updates SharedPreferences,
     * and switches UI back to view mode.
     */
    private void saveProfile() {
        String n = fullName.getText().toString().trim();
        String e = email.getText().toString().trim();
        String pRaw = phone.getText().toString();

        String pTrimmed = pRaw.trim();
        String pDigits = pTrimmed.replaceAll("\\D", "");

        if (!pTrimmed.isEmpty() && pDigits.length() != 10) {
            Toast.makeText(this, "Phone number must be 10 digits or left blank", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> upd = new HashMap<>();
        upd.put("name", n);
        upd.put("email", e);
        upd.put("phone", pRaw);

        userRef.set(upd, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    headerName.setText(TextUtils.isEmpty(n) ? "Entrant" : n);
                    setAvatarInitials(n);
                    setEditing(false);

                    getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("user_email", e)
                            .putString("user_name", n)
                            .apply();

                    Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e1 ->
                        Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show());
    }

    /**
     * Enables or disables edit mode for profile fields
     * and shows/hides the Save button.
     */
    private void setEditing(boolean editing) {
        fullName.setEnabled(editing);
        email.setEnabled(editing);
        phone.setEnabled(editing);
        btnSave.setVisibility(editing ? View.VISIBLE : View.GONE);
        editToggle.setVisibility(editing ? View.GONE : View.VISIBLE);
    }

    /**
     * Creates initials from the user's name and stores them
     * for use in the avatar display.
     */
    private void setAvatarInitials(String name) {
        String initials = "U";
        if (!TextUtils.isEmpty(name)) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1) {
                initials = parts[0].substring(0, 1).toUpperCase();
            } else {
                initials = (parts[0].substring(0, 1)
                        + parts[parts.length - 1].substring(0, 1)).toUpperCase();
            }
        }
        headerName.setTag(initials);
    }

    /**
     * Shows a dialog to enable or disable entrant notifications.
     * Saves the chosen value to both SharedPreferences and Firestore.
     */
    private void showNotifSettingsDialog() {

        boolean enabled = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getBoolean("entrant_notifications_enabled", true);

        new AlertDialog.Builder(this)
                .setTitle("Notifications")
                .setMessage(enabled
                        ? "Notifications are currently ENABLED. Do you want to disable them?"
                        : "Notifications are currently DISABLED. Do you want to enable them?")
                .setPositiveButton(enabled ? "Disable" : "Enable", (dialog, which) -> {

                    boolean newValue = !enabled;

                    getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean("entrant_notifications_enabled", newValue)
                            .apply();

                    if (userRef != null) {
                        userRef.update("entrant_notifications_enabled", newValue);
                    }

                    Toast.makeText(this,
                            newValue ? "Notifications enabled" : "Notifications disabled",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Displays a confirmation dialog before deleting the account.
     */
    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This will delete your profile and sign you out of Aurora. " +
                        "This action cannot be undone.\n\nAre you sure?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Fully deletes an entrant account.
     * Steps:
     *  Removes entrant email from all event lists.
     *  Deletes the entrant's Firestore user document.
     *  Signs out and clears session data.
     *  Returns user to LoginActivity.
     */
    private void deleteAccount() {
        if (userRef == null) {
            Toast.makeText(this, "No user profile found to delete.", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        final String emailValue = email.getText().toString().trim();

        db.collection("events")
                .get()
                .addOnSuccessListener(allEventsQuery -> {

                    for (DocumentSnapshot eventDoc : allEventsQuery.getDocuments()) {
                        boolean updated = false;
                        Map<String, Object> updates = new HashMap<>();

                        List<String> waitingList = (List<String>) eventDoc.get("waitingList");
                        if (waitingList != null && waitingList.contains(emailValue)) {
                            waitingList.remove(emailValue);
                            updates.put("waitingList", waitingList);
                            updated = true;
                        }

                        List<String> selected = (List<String>) eventDoc.get("selectedEntrants");
                        if (selected != null && selected.contains(emailValue)) {
                            selected.remove(emailValue);
                            updates.put("selectedEntrants", selected);
                            updated = true;
                        }

                        List<String> cancelled = (List<String>) eventDoc.get("cancelledEntrants");
                        if (cancelled != null && cancelled.contains(emailValue)) {
                            cancelled.remove(emailValue);
                            updates.put("cancelledEntrants", cancelled);
                            updated = true;
                        }

                        List<String> finalEntrants = (List<String>) eventDoc.get("finalEntrants");
                        if (finalEntrants != null && finalEntrants.contains(emailValue)) {
                            finalEntrants.remove(emailValue);
                            updates.put("finalEntrants", finalEntrants);
                            updated = true;
                        }

                        if (updated) {
                            db.collection("events")
                                    .document(eventDoc.getId())
                                    .update(updates);
                        }
                    }

                    userRef.delete()
                            .addOnSuccessListener(v -> {
                                FirebaseAuth.getInstance().signOut();
                                getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                                        .edit()
                                        .clear()
                                        .apply();

                                Toast.makeText(this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Failed to delete account: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error cleaning up entrant data: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    /**
     * Logs the user out by clearing authentication and session data,
     * then navigates back to LoginActivity.
     */
    private void performLogout() {
        FirebaseAuth.getInstance().signOut();

        getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}
