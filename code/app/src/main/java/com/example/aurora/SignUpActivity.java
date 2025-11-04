package com.example.aurora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText signupName, signupEmail, signupPhone, signupPassword;
    private RadioGroup radioGroupRole;
    private Button signupButton;
    private FirebaseFirestore db;

    private FirebaseAuth mAuth; // NEW

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupName = findViewById(R.id.Name);
        signupEmail = findViewById(R.id.Email);
        signupPhone = findViewById(R.id.Phone);
        signupPassword = findViewById(R.id.Password);
        radioGroupRole = findViewById(R.id.Role);
        signupButton = findViewById(R.id.SignUpButton);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance(); // NEW

        signupButton.setOnClickListener(v -> saveUser());

        // Optional: back to login
        findViewById(R.id.backToLoginButton).setOnClickListener(v ->
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class)));
    }

    private void saveUser() {
        String name = signupName.getText().toString().trim();
        String email = signupEmail.getText().toString().trim();
        String phone = signupPhone.getText().toString().trim();
        String password = signupPassword.getText().toString().trim();
        int selectedId = radioGroupRole.getCheckedRadioButtonId();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.length() != 10 || !phone.matches("\\d{10}")) {
            Toast.makeText(this, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = (selectedId == R.id.Organizer) ? "organizer" : "entrant";

        // NEW: Use Firebase Authentication to ensure unique accounts
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // NEW: Account created successfully, now store user info in Firestore
                        FirebaseUser firebaseUser = mAuth.getCurrentUser(); // NEW

                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("email", email);
                        user.put("phone", phone);
                        user.put("role", role);
                        user.put("password", password);

                        String uid = firebaseUser.getUid();
                        db.collection("users").document(uid).set(user)
                                .addOnSuccessListener(docRef -> {
                                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                                    Intent intent;
                                    if (role.equals("organizer")) {
                                        intent = new Intent(this, OrganizerActivity.class);
                                    } else {
                                        intent = new Intent(this, EntrantNavigationActivity.class);
                                    }

                                    intent.putExtra("userName", name);
                                    intent.putExtra("userEmail", email);
                                    intent.putExtra("userPhone", phone);
                                    intent.putExtra("userRole", role);

                                    // ✅ NEW: store for later access (used by OrganizerProfileActivity)
                                    getSharedPreferences("AuroraPrefs", MODE_PRIVATE)
                                            .edit()
                                            .putString("userName", name)
                                            .putString("userEmail", email)
                                            .putString("userPhone", phone)
                                            .putString("userRole", role)
                                            .apply();

                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                });
    }
}
