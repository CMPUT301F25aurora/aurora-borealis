package com.example.aurora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

//class for login screen on app startup
public class LoginActivity extends AppCompatActivity {

    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private Button createAccountButton;
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
        String email = loginEmail.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener(query -> {
                    //return invalid if no user info matches ones entered in firestore
                    if (query.isEmpty()) {
                        Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                        return;
                    }

                        // look into matching user and get role
                    QueryDocumentSnapshot doc = (QueryDocumentSnapshot) query.getDocuments().get(0);
                    String role = doc.getString("role");

                            //start correct activity based on role
                            if ("organizer".equals(role)) {
                                startActivity(new Intent(this, OrganizerActivity.class));
                            } else {
                                startActivity(new Intent(this, EntrantActivity.class));
                            }
                            finish();

                    })

                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
