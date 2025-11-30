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


    private void attemptSignup() {
        String name = signupName.getText().toString().trim();
        String email = signupEmail.getText().toString().trim();
        String phone = signupPhone.getText().toString().trim();
        String password = signupPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            toast("Please fill in all fields");
            return;
        }

        if (!phone.matches("\\d{10}")) {
            toast("Phone number must be exactly 10 digits");
            return;
        }

        createAuthAccount(name, email, phone, password);
    }


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


    private void handleAuthFailure(Exception e) {

        if (e instanceof FirebaseAuthUserCollisionException) {
            toast("An account with this email already exists.");
        } else if (e != null) {
            toast("Error: " + e.getMessage());
        } else {
            toast("Sign up failed.");
        }
    }


    private void writeUserToFirestore(String uid, String name, String email, String phone,
                                      String password) {

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("password", password);

        // Always entrant on signup
        user.put("role", "entrant");

        // Organizer mode allowed by default
        user.put("organizer_allowed", true);

        // Required defaults
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


    private void navigateAfterSignup(String name, String email, String phone) {

        Intent intent = new Intent(this, EntrantNavigationActivity.class);

        intent.putExtra("userName", name);
        intent.putExtra("userEmail", email);
        intent.putExtra("userPhone", phone);
        intent.putExtra("userRole", "entrant");

        startActivity(intent);
        finish();
    }


    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
