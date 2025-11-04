package com.example.aurora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText signupName, signupEmail, signupPhone, signupPassword;
    private RadioGroup radioGroupRole;
    private Button signupButton;
    private FirebaseFirestore db;

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

        signupButton.setOnClickListener(v -> saveUser());

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

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("password", password);   // needed so Login can query
        user.put("role", role);

        db.collection("users").add(user)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();

                    Intent intent;
                    if ("organizer".equals(role)) {
                        intent = new Intent(this, OrganizerActivity.class);
                    } else {
                        intent = new Intent(this, EntrantNavigationActivity.class);
                    }

                    intent.putExtra("userName", name);
                    intent.putExtra("userEmail", email);
                    intent.putExtra("userPhone", phone);
                    intent.putExtra("userRole", role);

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
