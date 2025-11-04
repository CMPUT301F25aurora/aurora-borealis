package com.example.aurora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

//class for login screen on app startup
public class LoginActivity extends AppCompatActivity {

    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private TextView createAccountButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);
        createAccountButton = findViewById(R.id.createAccountButton);

        db = FirebaseFirestore.getInstance();

        loginButton.setOnClickListener(v -> loginUser());
        createAccountButton.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));
    }
   // get user info entered and see if it matches info for a user in firestore db
    private void loginUser() {
        String input = loginEmail.getText().toString().trim(); //can be email or phone
        String password = loginPassword.getText().toString().trim();

        if (input.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email/phone and password", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("email", input)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        handleLogin(query.getDocuments().get(0));
                    } else {
                        // If not found, try phone-based login
                        db.collection("users")
                                .whereEqualTo("phone", input)
                                .whereEqualTo("password", password)
                                .get()
                                .addOnSuccessListener(phoneQuery -> {
                                    if (!phoneQuery.isEmpty()) {
                                        handleLogin(phoneQuery.getDocuments().get(0));
                                    } else {
                                        Toast.makeText(this, "Invalid email/phone or password", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void handleLogin(DocumentSnapshot doc) {
        // Extract all Firestore fields
        String name = doc.getString("name");
        String email = doc.getString("email");
        String phone = doc.getString("phone");
        String role = doc.getString("role");

        Toast.makeText(this, "Welcome " + name, Toast.LENGTH_SHORT).show();

        // Decide which screen to go to
        Intent intent;
        if ("organizer".equalsIgnoreCase(role)) {
            intent = new Intent(this, OrganizerActivity.class);
        } else {
            intent = new Intent(this, EventsActivity.class);
        }

        // Pass user info to the next activity
        intent.putExtra("userName", name);
        intent.putExtra("userEmail", email);
        intent.putExtra("userPhone", phone);
        intent.putExtra("userRole", role);

        // Save user info for later access
        getSharedPreferences("AuroraPrefs", MODE_PRIVATE)
                .edit()
                .putString("userName", name)
                .putString("userEmail", email)
                .putString("userPhone", phone)
                .putString("userRole", role)
                .apply();

        startActivity(intent);
        finish();
    }




}


