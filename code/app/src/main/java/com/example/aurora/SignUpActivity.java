package com.example.aurora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

// activity for user signup screen
public class SignUpActivity extends AppCompatActivity {

    private EditText signupName;
    private EditText signupEmail;
    private EditText signupPassword;
    private RadioGroup radioGroupRole;
    private Button signupButton;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupName = findViewById(R.id.Name);
        signupEmail = findViewById(R.id.Email);
        radioGroupRole = findViewById(R.id.Role);
        signupButton = findViewById(R.id.SignUpButton);
        signupPassword = findViewById(R.id.Password);
        db = FirebaseFirestore.getInstance();

        signupButton.setOnClickListener(v -> saveUser());
    }

    // gets info user enters and stores it in firestore under a new user
    private void saveUser() {
        String name = signupName.getText().toString().trim();
        String email = signupEmail.getText().toString().trim();
        String password = signupPassword.getText().toString().trim();
        int selectedId = radioGroupRole.getCheckedRadioButtonId();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
         // user role
        String role = (selectedId == R.id.Organizer) ? "organizer" : "entrant";

        //save info in firestore
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("password", password);
        user.put("role", role);

        db.collection("users").add(user)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                    if (role.equals("organizer")) {
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
