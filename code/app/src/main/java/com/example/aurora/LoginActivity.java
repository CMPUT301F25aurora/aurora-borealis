package com.example.aurora;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private TextView createAccountButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
        String role = sp.getString("user_role", null);
        String name = sp.getString("user_name", null);
        String email = sp.getString("user_email", null);
        String phone = sp.getString("user_phone", null);

        if (role != null && !role.isEmpty()) {
            Intent intent;
            if (role.equalsIgnoreCase("admin")) {
                intent = new Intent(this, AdminActivity.class);
            } else if (role.equalsIgnoreCase("organizer")) {
                intent = new Intent(this, OrganizerActivity.class);
            } else {
                intent = new Intent(this, EventsActivity.class);
            }

            intent.putExtra("userName", name);
            intent.putExtra("userEmail", email);
            intent.putExtra("userPhone", phone);
            intent.putExtra("userRole", role);

            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);
        createAccountButton = findViewById(R.id.createAccountButton);
        db = FirebaseFirestore.getInstance();

        createDefaultEntrantProfile();

        loginButton.setOnClickListener(v -> loginUser());
        createAccountButton.setOnClickListener(
                v -> startActivity(new Intent(this, SignUpActivity.class))
        );
    }

    private void createDefaultEntrantProfile() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        DocumentReference userRef = db.collection("users").document(deviceId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Map<String, Object> user = new HashMap<>();
                user.put("role", "entrant");
                user.put("notificationsEnabled", false);
                user.put("createdAt", System.currentTimeMillis());
                user.put("deviceId", deviceId);

                userRef.set(user)
                        .addOnSuccessListener(aVoid ->
                                Log.d("DeviceAuth", "✅ New entrant profile created for device: " + deviceId))
                        .addOnFailureListener(e ->
                                Log.e("DeviceAuth", "❌ Error creating entrant profile", e));
            } else {
                Log.d("DeviceAuth", "Existing entrant profile found for device: " + deviceId);
            }
        }).addOnFailureListener(e ->
                Log.e("DeviceAuth", "❌ Failed to check entrant profile", e));
    }

    private void loginUser() {
        String input = loginEmail.getText().toString().trim();    // email or phone
        String password = loginPassword.getText().toString().trim();

        if (input.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    "Please enter both email/phone and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) try email
        db.collection("users")
                .whereEqualTo("email", input)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        handleLogin(query.getDocuments().get(0));
                    } else {
                        // 2) fallback phone
                        db.collection("users")
                                .whereEqualTo("phone", input)
                                .whereEqualTo("password", password)
                                .get()
                                .addOnSuccessListener(phoneQuery -> {
                                    if (!phoneQuery.isEmpty()) {
                                        handleLogin(phoneQuery.getDocuments().get(0));
                                    } else {
                                        Toast.makeText(this,
                                                "Invalid email/phone or password",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(this,
                                        "Error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void handleLogin(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String email = doc.getString("email");
        String phone = doc.getString("phone");
        String role  = doc.getString("role");

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
        sp.edit()
                .putString("user_email", email == null ? "" : email)
                .putString("user_name",  name  == null ? "" : name)
                .putString("user_role",  role  == null ? "" : role)
                .putString("user_doc_id", doc.getId())
                .apply();

        Toast.makeText(this, "Welcome " + (name == null ? "" : name), Toast.LENGTH_SHORT).show();

        // Deep-link case (scanned QR before login)
        String pending = getSharedPreferences("aurora", MODE_PRIVATE)
                .getString("pending_event", null);
        if (pending != null) {
            getSharedPreferences("aurora", MODE_PRIVATE)
                    .edit()
                    .remove("pending_event")
                    .apply();

            Intent deepLinkIntent = new Intent(this, EventDetailsActivity.class);
            deepLinkIntent.putExtra("eventId", pending);
            startActivity(deepLinkIntent);
            finish();
            return;
        }

        Intent intent;
        if (role != null && role.equalsIgnoreCase("admin")) {
            intent = new Intent(this, AdminActivity.class);
        } else if (role != null && role.equalsIgnoreCase("organizer")) {
            intent = new Intent(this, OrganizerActivity.class);
        } else {
            intent = new Intent(this, EventsActivity.class);
        }

        intent.putExtra("userName",  name);
        intent.putExtra("userEmail", email);
        intent.putExtra("userPhone", phone);
        intent.putExtra("userRole",  role);

        startActivity(intent);
        finish();
    }
}
