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

        // -----------------------------
        // SESSION VALIDATION
        // -----------------------------
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

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {

        // -----------------------------
        // STEP 1: Delete all events by organizer
        // -----------------------------
        db.collection("events")
                .whereEqualTo("organizerEmail", userEmail)
                .get()
                .addOnSuccessListener(eventQuery -> {

                    for (DocumentSnapshot eventDoc : eventQuery) {
                        db.collection("events")
                                .document(eventDoc.getId())
                                .delete();
                    }

                    // -----------------------------
                    // STEP 2: Delete Firestore user document
                    // -----------------------------
                    db.collection("users").document(userDocId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {

                                // -----------------------------
                                // STEP 3: Delete Firebase Auth user
                                // -----------------------------
                                FirebaseUser user = auth.getCurrentUser();
                                if (user != null) {
                                    user.delete();
                                }

                                // -----------------------------
                                // STEP 4: Clear SharedPreferences
                                // -----------------------------
                                getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                                        .edit().clear().apply();

                                Toast.makeText(this, "Account deleted.", Toast.LENGTH_SHORT).show();

                                // -----------------------------
                                // STEP 5: Back to login
                                // -----------------------------
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
