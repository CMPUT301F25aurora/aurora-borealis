/**
 * This activity displays the organizer’s profile information.
 * Shows name, email, phone, and total active events.
 * Fetches organizer data from Firestore using their email.
 * Allows the organizer to delete their account.
 * On delete, removes the user's Firestore document and redirects to LoginActivity.
 * Includes a back button to return to OrganizerActivity.
 */


package com.example.aurora;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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


        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        backButton = findViewById(R.id.backButton);
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        profilePhone = findViewById(R.id.profilePhone);
        profileHeaderName = findViewById(R.id.profileHeaderName);
        profileHeaderRole = findViewById(R.id.profileHeaderRole);
        activeEventsCount = findViewById(R.id.activeEventsCount);
        deleteAccountButton = findViewById(R.id.deleteAccountButton);

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerProfileActivity.this, OrganizerActivity.class);
            startActivity(intent);
            finish();
        });
        loadProfileData();
        deleteAccountButton.setOnClickListener(v -> showDeleteDialog());
    }
    private void loadProfileData() {

        String fullName = getIntent().getStringExtra("fullName");
        String email = getIntent().getStringExtra("email");
        String phone = getIntent().getStringExtra("phone");

        profileName.setText(fullName != null ? fullName : "N/A");
        profileEmail.setText(email != null ? email : "N/A");
        profilePhone.setText(phone != null ? phone : "N/A");
        profileHeaderName.setText(fullName != null ? fullName : "Event Organizer");
        profileHeaderRole.setText("Event Organizer");

        db.collection("organizers")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        DocumentSnapshot document = query.getDocuments().get(0);
                        Long activeEvents = document.getLong("activeEvents");
                        activeEventsCount.setText(activeEvents != null ? String.valueOf(activeEvents) : "0");
                    }
                });
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

        String email = getIntent().getStringExtra("email");
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "No user email found.", Toast.LENGTH_SHORT).show();
            return;
        }

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
    }


}
