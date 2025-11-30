package com.example.aurora.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private TextView createAccountButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);

        String lastMode = sp.getString("user_last_mode", null);
        String storedRole = sp.getString("user_role", null);
        String docId = sp.getString("user_doc_id", null);

        if (storedRole != null
                && lastMode != null
                && docId != null
                && !storedRole.trim().isEmpty()) {

            if (storedRole.equalsIgnoreCase("admin")) {
                startActivity(new Intent(this, AdminActivity.class));
                finish();
                return;
            }

            if (lastMode.equals("organizer")) {
                startActivity(new Intent(this, OrganizerActivity.class));
                finish();
                return;
            }

            startActivity(new Intent(this, EntrantNavigationActivity.class));
            finish();
            return;
        }

        // No auto-login → show login screen
        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);
        createAccountButton = findViewById(R.id.createAccountButton);

        db = FirebaseFirestore.getInstance();

        setupPasswordToggle();

        loginButton.setOnClickListener(v -> loginUser());

        createAccountButton.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class))
        );
    }

    private void setupPasswordToggle() {
        ImageView toggle = findViewById(R.id.passwordToggle);
        final boolean[] visible = {false};

        toggle.setOnClickListener(v -> {
            if (visible[0]) {
                loginPassword.setTransformationMethod(new PasswordTransformationMethod());
                toggle.setImageResource(R.drawable.ic_eye_closed);
            } else {
                loginPassword.setTransformationMethod(null);
                toggle.setImageResource(R.drawable.ic_eye_open);
            }
            visible[0] = !visible[0];
            loginPassword.setSelection(loginPassword.getText().length());
        });
    }

    private void loginUser() {
        String input = loginEmail.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (input.isEmpty() || password.isEmpty()) {
            toast("Please enter both fields.");
            return;
        }

        // 1) Try login by email
        db.collection("users")
                .whereEqualTo("email", input)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener(emailQuery -> {
                    if (!emailQuery.isEmpty()) {
                        handleLogin(emailQuery.getDocuments().get(0));
                    } else {
                        // 2) Try login by phone
                        db.collection("users")
                                .whereEqualTo("phone", input)
                                .whereEqualTo("password", password)
                                .get()
                                .addOnSuccessListener(phoneQuery -> {
                                    if (!phoneQuery.isEmpty()) {
                                        handleLogin(phoneQuery.getDocuments().get(0));
                                    } else {
                                        toast("Invalid login credentials.");
                                    }
                                });
                    }
                });
    }

    private void handleLogin(DocumentSnapshot doc) {

        String name = doc.getString("name");
        String email = doc.getString("email");
        String phone = doc.getString("phone");
        String role  = doc.getString("role");

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);

        sp.edit()
                .putString("user_name",  name)
                .putString("user_email", email)
                .putString("user_phone", phone)
                .putString("user_role",  role)
                .putString("user_doc_id", doc.getId())

                // ALWAYS set last mode
                .putString("user_last_mode",
                        role.equals("organizer") ? "organizer" : "entrant"
                )

                .apply();

        toast("Welcome " + name);

        Intent intent;

        if (role.equals("admin")) {
            intent = new Intent(this, AdminActivity.class);
        } else if (role.equals("organizer")) {
            intent = new Intent(this, OrganizerActivity.class);
        } else {
            intent = new Intent(this, EntrantNavigationActivity.class);
        }

        startActivity(intent);
        finish();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
