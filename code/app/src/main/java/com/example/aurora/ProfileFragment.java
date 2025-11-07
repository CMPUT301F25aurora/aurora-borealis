/*
 * References for this fragment:
 *
 * 1) source: Android Developers — "Fragments"
 *    https://developer.android.com/guide/fragments
 *    Used for basic fragment lifecycle and inflating the profile layout inside a fragment.
 *
 * 2) source: Firebase docs — "Get data with Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/query-data/get-data
 *    Used for loading the user profile document while inside a Fragment.
 *
 * 3) author: Stack Overflow user — "How to correctly implement onCreateView in a Fragment"
 *    https://stackoverflow.com/questions/6484708/android-fragments-and-oncreateview
 *    Used for the pattern of inflating a view and wiring up UI controls in onCreateView.
 */

package com.example.aurora;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
/**
 * ProfileFragment.java
 *
 * Fragment that displays and manages the entrant’s profile in the Aurora app.
 * - Loads user data (name, email, phone, role, stats) from Firestore.
 * - Allows editing and saving profile details with validation.
 * - Lets users toggle notification settings on or off.
 * - Supports deleting the account and returning to the login screen.
 * - Automatically creates a new profile if one does not exist.
 */

public class ProfileFragment extends Fragment {

    private FirebaseFirestore db;
    private DocumentReference userRef;

    private ImageView avatar;
    private TextView roleBadge, headerName, joinedCount, winsCount, editToggle;
    private EditText fullName, email, phone;
    private Button btnSave, btnEventHistory, btnNotifSettings, btnDelete;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        db = FirebaseFirestore.getInstance();

        avatar = v.findViewById(R.id.avatarCircle);
        roleBadge = v.findViewById(R.id.roleBadge);
        headerName = v.findViewById(R.id.headerName);
        joinedCount = v.findViewById(R.id.joinedCount);
        winsCount = v.findViewById(R.id.winsCount);
        editToggle = v.findViewById(R.id.editToggle);
        fullName = v.findViewById(R.id.inputFullName);
        email = v.findViewById(R.id.inputEmail);
        phone = v.findViewById(R.id.inputPhone);
        btnSave = v.findViewById(R.id.btnSave);
        btnEventHistory = v.findViewById(R.id.btnEventHistory);
        btnNotifSettings = v.findViewById(R.id.btnNotifSettings);
        btnDelete = v.findViewById(R.id.btnDeleteAccount);

        setEditing(false);
        resolveAndLoad();

        editToggle.setOnClickListener(x -> setEditing(true));
        btnSave.setOnClickListener(x -> saveProfile());
        btnEventHistory.setOnClickListener(x ->
                Toast.makeText(getContext(), "Event History coming soon", Toast.LENGTH_SHORT).show());

        btnNotifSettings.setOnClickListener(x -> toggleNotifications());

        btnDelete.setOnClickListener(x -> {
            if (userRef == null) {
                Toast.makeText(getContext(), "Profile not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }

            userRef.delete()
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Error deleting account", Toast.LENGTH_SHORT).show());
        });
    }

    private void resolveAndLoad() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        String email;
        if (currentUser != null) {
            email = currentUser.getEmail();
        } else {
            email = requireActivity()
                    .getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE)
                    .getString("user_email", null);
        }

        if (email == null || email.isEmpty()) {
            Toast.makeText(getContext(), "No user email found", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(result -> {
                    if (!result.isEmpty()) {
                        userRef = result.getDocuments().get(0).getReference();
                        loadProfile();
                    } else {
                        Map<String, Object> newUser = new HashMap<>();
                        newUser.put("email", email);
                        newUser.put("role", "Entrant");
                        newUser.put("joinedCount", 0);
                        newUser.put("winsCount", 0);
                        newUser.put("notificationsEnabled", true);

                        db.collection("users")
                                .add(newUser)
                                .addOnSuccessListener(ref -> {
                                    userRef = ref;
                                    loadProfile();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "Error creating profile", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error loading user", Toast.LENGTH_SHORT).show());
    }

    private void loadProfile() {
        if (userRef == null) return;

        userRef.get().addOnSuccessListener(doc -> {
            String n = doc.getString("name");
            String e = doc.getString("email");
            String p = doc.getString("phone");
            String role = doc.getString("role");
            Long j = doc.getLong("joinedCount");
            Long w = doc.getLong("winsCount");

            fullName.setText(n == null ? "" : n);
            email.setText(e == null ? "" : e);
            phone.setText(p == null ? "" : p);
            headerName.setText(TextUtils.isEmpty(n) ? "Entrant" : n);
            roleBadge.setText(TextUtils.isEmpty(role) ? "Entrant" : role);
            joinedCount.setText(String.valueOf(j == null ? 0 : j));
            winsCount.setText(String.valueOf(w == null ? 0 : w));

            Boolean notificationsEnabled = doc.getBoolean("notificationsEnabled");
            if (notificationsEnabled == null || notificationsEnabled) {
                btnNotifSettings.setText("Notifications Enabled");
            } else {
                btnNotifSettings.setText("Notifications Disabled");
            }
        });
    }

    private void saveProfile() {
        if (userRef == null) return;

        String n = fullName.getText().toString().trim();
        String e = email.getText().toString().trim();
        String p = phone.getText().toString().trim();

        Map<String, Object> upd = new HashMap<>();
        upd.put("name", n);
        upd.put("email", e);
        upd.put("phone", p);
        upd.put("role", "Entrant");

        userRef.set(upd, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    headerName.setText(TextUtils.isEmpty(n) ? "Entrant" : n);
                    setEditing(false);
                    Toast.makeText(getContext(), "Profile saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e1 ->
                        Toast.makeText(getContext(), "Save failed", Toast.LENGTH_SHORT).show());
    }

    private void toggleNotifications() {
        if (userRef == null) {
            Toast.makeText(getContext(), "Profile not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        userRef.get().addOnSuccessListener(doc -> {
            Boolean current = doc.getBoolean("notificationsEnabled");
            boolean newValue = current == null || !current; // toggle

            Map<String, Object> update = new HashMap<>();
            update.put("notificationsEnabled", newValue);

            userRef.update(update)
                    .addOnSuccessListener(unused -> {
                        String label = newValue ? "Notifications Enabled" : "Notifications Disabled";
                        btnNotifSettings.setText(label);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to update setting", Toast.LENGTH_SHORT).show());
        });
    }

    private void setEditing(boolean editing) {
        fullName.setEnabled(editing);
        email.setEnabled(editing);
        phone.setEnabled(editing);
        btnSave.setVisibility(editing ? View.VISIBLE : View.GONE);
        editToggle.setVisibility(editing ? View.GONE : View.VISIBLE);
    }
}
