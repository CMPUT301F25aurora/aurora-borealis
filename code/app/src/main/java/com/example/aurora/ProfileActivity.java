package com.example.aurora;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private DocumentReference userRef;

    private ImageView avatar;
    private TextView roleBadge, headerName, joinedCount, winsCount, editToggle;
    private EditText fullName, email, phone;
    private Button btnSave, btnEventHistory, btnNotifSettings, btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile);

        db = FirebaseFirestore.getInstance();

        avatar = findViewById(R.id.avatarCircle);
        roleBadge = findViewById(R.id.roleBadge);
        headerName = findViewById(R.id.headerName);
        joinedCount = findViewById(R.id.joinedCount);
        winsCount = findViewById(R.id.winsCount);
        editToggle = findViewById(R.id.editToggle);
        fullName = findViewById(R.id.inputFullName);
        email = findViewById(R.id.inputEmail);
        phone = findViewById(R.id.inputPhone);
        btnSave = findViewById(R.id.btnSave);
        btnEventHistory = findViewById(R.id.btnEventHistory);
        btnNotifSettings = findViewById(R.id.btnNotifSettings);
        btnDelete = findViewById(R.id.btnDeleteAccount);

        setEditing(false);
        resolveAndLoad();

        editToggle.setOnClickListener(v -> setEditing(true));
        btnSave.setOnClickListener(v -> saveProfile());
        btnEventHistory.setOnClickListener(v -> Toast.makeText(this, "Event History coming soon", Toast.LENGTH_SHORT).show());
        btnNotifSettings.setOnClickListener(v -> Toast.makeText(this, "Notification Settings coming soon", Toast.LENGTH_SHORT).show());
        btnDelete.setOnClickListener(v -> Toast.makeText(this, "Delete Account coming soon", Toast.LENGTH_SHORT).show());
    }

    private void resolveAndLoad() {
        FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
        String authEmail = fu != null ? fu.getEmail() : null;
        if (!TextUtils.isEmpty(authEmail)) {
            queryByEmail(authEmail);
        } else {
            String savedEmail = getSharedPreferences("aurora_prefs", MODE_PRIVATE).getString("user_email", null);
            if (!TextUtils.isEmpty(savedEmail)) queryByEmail(savedEmail);
            else Toast.makeText(this, "No logged-in user found", Toast.LENGTH_SHORT).show();
        }
    }

    private void queryByEmail(String em) {
        Query q = db.collection("users").whereEqualTo("email", em).limit(1);
        q.get().addOnSuccessListener(snap -> {
            if (!snap.isEmpty()) {
                userRef = snap.getDocuments().get(0).getReference();
                loadProfile();
            } else {
                Map<String, Object> init = new HashMap<>();
                init.put("email", em);
                init.put("role", "Entrant");
                init.put("joinedCount", 0);
                init.put("winsCount", 0);
                userRef = db.collection("users").document();
                userRef.set(init, SetOptions.merge()).addOnSuccessListener(v -> loadProfile());
            }
        });
    }

    private void loadProfile() {
        userRef.get().addOnSuccessListener(doc -> {
            String n = doc.getString("name");
            String e = doc.getString("email");
            String p = doc.getString("phone");
            String role = doc.getString("role");
            Long joined = doc.getLong("joinedCount");
            Long wins = doc.getLong("winsCount");

            fullName.setText(n == null ? "" : n);
            email.setText(e == null ? "" : e);
            phone.setText(p == null ? "" : p);
            headerName.setText(TextUtils.isEmpty(n) ? "Entrant" : n);
            roleBadge.setText(TextUtils.isEmpty(role) ? "Entrant" : role);
            joinedCount.setText(String.valueOf(joined == null ? 0 : joined));
            winsCount.setText(String.valueOf(wins == null ? 0 : wins));
            setAvatarInitials(fullName.getText().toString());
        });
    }

    private void saveProfile() {
        String n = fullName.getText().toString().trim();
        String e = email.getText().toString().trim();
        String p = phone.getText().toString().trim();

        Map<String, Object> upd = new HashMap<>();
        upd.put("name", n);
        upd.put("email", e);
        upd.put("phone", p);
        upd.put("role", "Entrant");

        userRef.set(upd, SetOptions.merge()).addOnSuccessListener(v -> {
            headerName.setText(TextUtils.isEmpty(n) ? "Entrant" : n);
            roleBadge.setText("Entrant");
            setAvatarInitials(n);
            setEditing(false);
            Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e1 -> Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show());
    }

    private void setEditing(boolean editing) {
        fullName.setEnabled(editing);
        email.setEnabled(editing);
        phone.setEnabled(editing);
        btnSave.setVisibility(editing ? View.VISIBLE : View.GONE);
        editToggle.setVisibility(editing ? View.GONE : View.VISIBLE);
    }

    private void setAvatarInitials(String name) {
        String initials = "U";
        if (!TextUtils.isEmpty(name)) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1) initials = parts[0].substring(0, 1).toUpperCase();
            else initials = (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
        headerName.setTag(initials);
    }
}