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

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
/**
 * OrganizerProfileActivity
 *
 * Displays the organizer’s personal profile information.
 * Features:
 *  Shows name, email, phone, and number of active events.
 *  Allows user to delete their entire account.
 *  Loads profile data from Firestore.
 *  Handles session validation and redirects if session is invalid.
 */

public class OrganizerProfileActivity extends AppCompatActivity {
    private ImageView backButton;
    private TextView profileName, profileEmail, profilePhone, profileHeaderName, profileHeaderRole, activeEventsCount;
    private Button deleteAccountButton;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String userDocId;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_profile);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
        userDocId = sp.getString("user_doc_id", null);
        userEmail = sp.getString("user_email", null);

        if (userDocId == null || userEmail == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bindViews();

        backButton.setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerActivity.class));
            finish();
        });

        loadProfileData();

        deleteAccountButton.setOnClickListener(v -> showDeleteDialog());
    }

    /**
     * Connects all XML layout views to their corresponding Java variables.
     */
    private void bindViews() {
        backButton = findViewById(R.id.backButton);
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        profilePhone = findViewById(R.id.profilePhone);
        profileHeaderName = findViewById(R.id.profileHeaderName);
        profileHeaderRole = findViewById(R.id.profileHeaderRole);
        activeEventsCount = findViewById(R.id.activeEventsCount);
        deleteAccountButton = findViewById(R.id.deleteAccountButton);
    }

    /**
     * Loads the organizer’s profile details and active event count from Firestore.
     * Updates the UI with name, email, phone, and event statistics.
     */
    private void loadProfileData() {

        db.collection("users").document(userDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String fullName = doc.getString("name");
                    String phone = doc.getString("phone");

                    profileName.setText(fullName != null ? fullName : "N/A");
                    profileEmail.setText(userEmail);
                    profilePhone.setText(phone != null ? phone : "N/A");

                    profileHeaderName.setText(fullName != null ? fullName : "Organizer");
                    profileHeaderRole.setText("Event Organizer");

                    db.collection("events")
                            .whereEqualTo("organizerEmail", userEmail)
                            .get()
                            .addOnSuccessListener(eventsQuery -> {
                                activeEventsCount.setText(String.valueOf(eventsQuery.size()));
                            })
                            .addOnFailureListener(e -> activeEventsCount.setText("0"));

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Displays a confirmation dialog asking the organizer
     * if they really want to delete their account.
     */
    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Fully deletes the organizer account.
     *
     * Steps:
     *  1) Deletes all events created by the organizer.
     *  2) Deletes organizer's Firestore user document.
     *  3) Deletes Firebase Auth account.
     *  4) Clears stored session data.
     *  5) Redirects user back to LoginScreen.
     */
    private void deleteAccount() {

        db.collection("events")
                .whereEqualTo("organizerEmail", userEmail)
                .get()
                .addOnSuccessListener(eventQuery -> {

                    for (DocumentSnapshot eventDoc : eventQuery) {
                        db.collection("events")
                                .document(eventDoc.getId())
                                .delete();
                    }

                    db.collection("users").document(userDocId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {

                                FirebaseUser user = auth.getCurrentUser();
                                if (user != null) {
                                    user.delete();
                                }

                                getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                                        .edit().clear().apply();

                                Toast.makeText(this, "Account deleted.", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete events: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
