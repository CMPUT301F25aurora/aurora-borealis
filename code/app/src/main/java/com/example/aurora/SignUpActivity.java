/**
 * SignUpActivity.java
 *
 * Creates a new user account.
 * - Validates fields
 * - Creates Firebase Auth user
 * - Saves user document in "users" collection
 * - Logs registration in "logs"
 * - Routes:
 *      organizer → OrganizerActivity
 *      entrant   → EventsActivity (NOT EntrantNavigationActivity)
 */

/*
 * References for this screen:
 *
 * 1) source: Firebase docs — "Authenticate with Firebase using Password-Based Accounts on Android"
 *    https://firebase.google.com/docs/auth/android/password-auth
 *    Used for createUserWithEmailAndPassword and handling sign-up callbacks.
 *
 * 2) author: Stack Overflow user — "How to include username when storing email and password using Firebase"
 *    https://stackoverflow.com/questions/32151178/how-do-you-include-a-username-when-storing-email-and-password-using-firebase-ba
 *    Used for storing extra profile fields in Firestore right after auth sign-up.
 *
 * 3) source: Android Developers — "Understand the Activity lifecycle"
 *    https://developer.android.com/guide/components/activities/activity-lifecycle
 *    Used for managing navigation after a successful sign up and finishing this Activity.
 */


package com.example.aurora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText signupName, signupEmail, signupPhone, signupPassword;
    private RadioGroup radioGroupRole;
    private Button signupButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupName     = findViewById(R.id.Name);
        signupEmail    = findViewById(R.id.Email);
        signupPhone    = findViewById(R.id.Phone);
        signupPassword = findViewById(R.id.Password);
        radioGroupRole = findViewById(R.id.Role);
        signupButton   = findViewById(R.id.SignUpButton);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        signupButton.setOnClickListener(v -> saveUser());

        findViewById(R.id.backToLoginButton)
                .setOnClickListener(v ->
                        startActivity(new Intent(this, LoginActivity.class)));
    }

    private void saveUser() {
        String name     = signupName.getText().toString().trim();
        String email    = signupEmail.getText().toString().trim();
        String phone    = signupPhone.getText().toString().trim();
        String password = signupPassword.getText().toString().trim();
        int selectedId  = radioGroupRole.getCheckedRadioButtonId();

        if (name.isEmpty() || email.isEmpty()
                || password.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this,
                    "Please fill in all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.length() != 10 || !phone.matches("\\d{10}")) {
            Toast.makeText(this,
                    "Phone number must be exactly 10 digits",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String role = (selectedId == R.id.Organizer) ? "organizer" : "entrant";

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser == null) {
                            Toast.makeText(this,
                                    "Could not create account.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String uid = firebaseUser.getUid();

                        Map<String, Object> user = new HashMap<>();
                        user.put("name",  name);
                        user.put("email", email);
                        user.put("phone", phone);
                        user.put("role",  role);
                        user.put("password", password);

                        db.collection("users").document(uid)
                                .set(user)
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(this,
                                            "Account created!",
                                            Toast.LENGTH_SHORT).show();

                                    // simple log
                                    Map<String, Object> log = new HashMap<>();
                                    log.put("type", "user_registered");
                                    log.put("message", "User registered: " + email);
                                    log.put("timestamp", FieldValue.serverTimestamp());
                                    log.put("userId", uid);
                                    log.put("userEmail", email);
                                    log.put("userRole", role);
                                    db.collection("logs").add(log);

                                    getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                                            .edit()
                                            .putString("user_email", email)
                                            .putString("user_name", name)
                                            .putString("user_role", role)
                                            .putString("user_doc_id", uid)
                                            .apply();

                                    Intent intent;
                                    if (role.equals("organizer")) {
                                        intent = new Intent(this, OrganizerActivity.class);
                                    } else {
                                        // ✅ entrants -> EventsActivity
                                        intent = new Intent(this, EventsActivity.class);
                                    }

                                    intent.putExtra("userName",  name);
                                    intent.putExtra("userEmail", email);
                                    intent.putExtra("userPhone", phone);
                                    intent.putExtra("userRole",  role);

                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Error: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show());
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(this,
                                    "An account with this email already exists.",
                                    Toast.LENGTH_SHORT).show();
                        } else if (e != null) {
                            Toast.makeText(this,
                                    "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this,
                                    "Sign up failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
