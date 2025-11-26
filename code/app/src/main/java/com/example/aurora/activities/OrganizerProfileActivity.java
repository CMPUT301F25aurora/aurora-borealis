/*
 * References for this screen:
 *
 * 1) source: Firebase docs — "Get data with Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/query-data/get-data
 *    Used for loading the organizer's profile document from Firestore.
 *
 * 2) source: Firebase docs — "Add data to Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/manage-data/add-data
 *    Used for saving changed profile fields such as name or phone.
 *
 * 3) author: Stack Overflow user — "How to update a specific document in Firestore"
 *    https://stackoverflow.com/questions/46597327/firebase-firestore-how-to-update-a-document
 *    Used for the pattern of calling update(...) on a Firestore document reference.
 */

package com.example.aurora.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;


import com.example.aurora.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
/**
 * OrganizerProfileActivity.java
 *
 * Displays the organizer’s profile information in the Aurora app.
 * - Shows name, email, phone, and role details passed from the login screen.
 * - Fetches additional stats like the number of active events from Firestore.
 * - Includes navigation back to the organizer dashboard.
 * - Provides an option to permanently delete the organizer’s account.
 */


public class OrganizerProfileActivity extends AppCompatActivity {

    private ImageView backButton;
    private TextView profileName, profileEmail, profilePhone, profileHeaderName, profileHeaderRole, activeEventsCount;
    private Button deleteAccountButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_profile);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize UI
        backButton = findViewById(R.id.backButton);
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        profilePhone = findViewById(R.id.profilePhone);
        profileHeaderName = findViewById(R.id.profileHeaderName);
        profileHeaderRole = findViewById(R.id.profileHeaderRole);
        activeEventsCount = findViewById(R.id.activeEventsCount);
        deleteAccountButton = findViewById(R.id.deleteAccountButton);

        // Back Button
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerProfileActivity.this, UnifiedNavigationActivity.class);
            startActivity(intent);
            finish();
        });

        // Fetch Profile Data
        loadProfileData();

        // Delete Account
        deleteAccountButton.setOnClickListener(v -> showDeleteDialog());
    }

    private void loadProfileData() {

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
        String docId = sp.getString("user_doc_id", null);
        String email = sp.getString("user_email", null);

        if (docId == null || email == null) {
            Toast.makeText(this, "No saved user session found.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String fullName = doc.getString("name");
                    String phone = doc.getString("phone");

                    profileName.setText(fullName != null ? fullName : "N/A");
                    profileEmail.setText(email);
                    profilePhone.setText(phone != null ? phone : "N/A");

                    profileHeaderName.setText(fullName != null ? fullName : "Event Organizer");
                    profileHeaderRole.setText("Event Organizer");

                    // 🔥 Count events by organizer's email (adjust field name to match your schema)
                    db.collection("events")
                            .whereEqualTo("organizerEmail", email)
                            .get()
                            .addOnSuccessListener(eventsQuery -> {
                                int count = eventsQuery.size();
                                activeEventsCount.setText(String.valueOf(count));
                            })
                            .addOnFailureListener(e -> {
                                activeEventsCount.setText("0");
                            });

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteAccount() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get email passed from intent (or from prefs)
        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
        String email = sp.getString("user_email", null);

        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "No user email found in session.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "No user email found.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1️⃣ Delete ALL events created by this organizer
        db.collection("events")
                .whereEqualTo("organizerEmail", email)
                .get()
                .addOnSuccessListener(eventQuery -> {
                    for (DocumentSnapshot eventDoc : eventQuery.getDocuments()) {
                        db.collection("events").document(eventDoc.getId()).delete();
                    }

                    // 2️⃣ Now delete the user account document
                    db.collection("users")
                            .whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    String docId = querySnapshot.getDocuments().get(0).getId();

                                    db.collection("users").document(docId)
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();

                                                // 3️⃣ Redirect back to login
                                                Intent intent = new Intent(this, LoginActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Failed to delete Firestore data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                } else {
                                    Toast.makeText(this, "No matching user found in Firestore.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error accessing Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete organizer events: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}