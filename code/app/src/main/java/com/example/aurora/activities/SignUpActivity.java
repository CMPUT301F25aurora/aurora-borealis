/*
 * source: Firebase docs — "Sign up new users".
 * url: https://firebase.google.com/docs/auth/android/password-auth#create_a_password-based_account
 * note: Used for mAuth.createUserWithEmailAndPassword() to register the user in Firebase Auth.
 *
 * source: Firebase docs — "ServerTimestamp".
 * url: https://firebase.google.com/docs/firestore/manage-data/add-data#server_timestamp
 * note: Used (FieldValue.serverTimestamp()) to record the exact time of account creation.
 *
 * source: Stack Overflow user — "Regex for phone number validation".
 * url: https://stackoverflow.com/questions/2113908/what-regular-expression-will-match-valid-international-phone-numbers
 * note: Logic adapted for the 10-digit phone number check ("\\d{10}").
 *
 * source: Android Developers — "SharedPreferences".
 * url: https://developer.android.com/training/data-storage/shared-preferences
 * note: Used to immediately store the new user's session data so they don't have to log in again.
 */


package com.example.aurora.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
/**
 * SignUpActivity
 *
 * Handles new user registration.
 * Features:
 *  Validates signup form fields.
 *  Creates a Firebase Auth account.
 *  Stores user profile data in Firestore.
 *  Saves session preferences.
 *  Navigates the user to the entrant home screen.
 */
public class SignUpActivity extends AppCompatActivity {
    private EditText signupName, signupEmail, signupPhone, signupPassword;
    private Button signupButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        signupName = findViewById(R.id.Name);
        signupEmail = findViewById(R.id.Email);
        signupPhone = findViewById(R.id.Phone);
        signupPassword = findViewById(R.id.Password);
        signupButton = findViewById(R.id.SignUpButton);

        setupPasswordToggle();
        signupButton.setOnClickListener(v -> attemptSignup());

        findViewById(R.id.backToLoginButton).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );
    }

    /**
     * Enables tap-to-show / tap-to-hide functionality
     * for the password input field.
     */
    private void setupPasswordToggle() {
        final boolean[] visible = {false};
        ImageView toggle = findViewById(R.id.passwordToggleSignup);

        toggle.setOnClickListener(v -> {
            if (visible[0]) {
                signupPassword.setTransformationMethod(new PasswordTransformationMethod());
                toggle.setImageResource(R.drawable.ic_eye_closed);
            } else {
                signupPassword.setTransformationMethod(null);
                toggle.setImageResource(R.drawable.ic_eye_open);
            }
            visible[0] = !visible[0];
            signupPassword.setSelection(signupPassword.getText().length());
        });
    }

    /**
     * Reads all signup form fields, validates them,
     * and if valid, begins account creation.
     */
    private void attemptSignup() {
        String name = signupName.getText().toString().trim();
        String email = signupEmail.getText().toString().trim().toLowerCase();
        String phone = signupPhone.getText().toString().trim();
        String password = signupPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            toast("Please fill in all Required fields");
            return;
        }

        if (password.length() < 6) {
            toast("Password must be at least 6 characters long");
            return;
        }

        if (!phone.isEmpty() && !phone.matches("\\d{10}")) {
            toast("Phone number must be 10 digits or left blank");
            return;
        }


        createAuthAccount(name, email, phone, password);
    }

    /**
     * Creates a FirebaseAuth account using the provided email and password.
     * On success, continues by writing a Firestore profile document.
     */
    private void createAuthAccount(String name, String email, String phone, String password) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        handleAuthFailure(task.getException());
                        return;
                    }
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser == null) {
                        toast("Could not create account.");
                        return;
                    }
                    writeUserToFirestore(firebaseUser.getUid(), name, email, phone, password);
                });
    }

    /**
     * Handles FirebaseAuth creation errors such as
     * duplicate emails or invalid credentials.
     */
    private void handleAuthFailure(Exception e) {

        if (e instanceof FirebaseAuthUserCollisionException) {
            toast("An account with this email already exists.");
        } else if (e != null) {
            toast("Error: " + e.getMessage());
        } else {
            toast("Sign up failed.");
        }
    }

    /**
     * Writes a new user profile document to Firestore.
     * Sets default values such as role, permissions, and counters.
     * On success, saves local session preferences and navigates to home.
     */
    private void writeUserToFirestore(String uid, String name, String email, String phone,
                                      String password) {

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("password", password);

        user.put("role", "entrant");

        user.put("organizer_allowed", true);

        user.put("entrant_notifications_enabled", true);
        user.put("joinedCount", 0);
        user.put("winsCount", 0);

        user.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(v -> {
                    toast("Account created!");
                    savePreferences(name, email, uid);
                    navigateAfterSignup(name, email, phone);
                })
                .addOnFailureListener(e ->
                        toast("Error: " + e.getMessage()));
    }

    /**
     * Stores basic user session info in SharedPreferences
     * so the user stays logged in after signup.
     */
    private void savePreferences(String name, String email, String uid) {

        getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .edit()
                .putString("user_name", name)
                .putString("user_email", email)
                .putString("user_role", "entrant")
                .putString("user_last_mode", "entrant")
                .putString("user_doc_id", uid)
                .apply();
    }

    /**
     * Redirects the user to the entrant home screen
     * after signup is fully completed.
     */
    private void navigateAfterSignup(String name, String email, String phone) {

        Intent intent = new Intent(this, EventsActivity.class);

        intent.putExtra("userName", name);
        intent.putExtra("userEmail", email);
        intent.putExtra("userPhone", phone);
        intent.putExtra("userRole", "entrant");

        startActivity(intent);
        finish();
    }

    /**
     * Helper for showing short Toast messages.
     */
    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
