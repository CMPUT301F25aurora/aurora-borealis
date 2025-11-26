/*
 * References for this fragment:
 *
 * 1) Android Developers — "Fragments"
 * 2) Firebase docs — "Get data with Cloud Firestore"
 * 3) Stack Overflow — onCreateView patterns
 */

package com.example.aurora.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.aurora.R;
import com.example.aurora.activities.LoginActivity;
import com.example.aurora.activities.UnifiedNavigationActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private FirebaseFirestore db;
    private DocumentReference userRef;

    private ImageView avatar;
    private TextView roleBadge, headerName, joinedCount, winsCount, editToggle;
    private EditText fullName, email, phone;
    private Button btnSave, btnEventHistory, btnNotifSettings, btnDelete;
    private Switch modeSwitch;

    private String currentMode = "entrant";
    private boolean isOrganizerApproved = true;

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
        modeSwitch = v.findViewById(R.id.modeSwitch);

        setEditing(false);
        resolveAndLoad();

        editToggle.setOnClickListener(x -> setEditing(true));
        btnSave.setOnClickListener(x -> saveProfile());
        btnEventHistory.setOnClickListener(x ->
                Toast.makeText(getContext(), "Event history coming soon", Toast.LENGTH_SHORT).show());

        btnNotifSettings.setOnClickListener(x -> toggleNotifications());

        ImageButton btnEditPhoto = v.findViewById(R.id.btnEditPhoto);
        btnEditPhoto.setOnClickListener(x ->
                Toast.makeText(getContext(), "Profile photo upload coming soon!", Toast.LENGTH_SHORT).show());

        btnDelete.setOnClickListener(x -> deleteAccount());
    }

    // ---------------------------------------------------------
    // LOAD PROFILE
    // ---------------------------------------------------------
    private void resolveAndLoad() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        String em;
        if (currentUser != null) {
            em = currentUser.getEmail();
        } else {
            em = requireActivity()
                    .getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE)
                    .getString("user_email", null);
        }

        if (em == null) {
            Toast.makeText(getContext(), "No user email found", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("email", em)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        userRef = snap.getDocuments().get(0).getReference();
                        loadProfile();
                    } else {
                        createProfile(em);
                    }
                });
    }

    private void createProfile(String email) {
        Map<String, Object> init = new HashMap<>();
        init.put("email", email);
        init.put("role", "entrant");
        init.put("mode", "entrant");
        init.put("joinedCount", 0);
        init.put("winsCount", 0);
        init.put("notificationsEnabled", true);
        init.put("isOrganizerApproved", false);

        db.collection("users")
                .add(init)
                .addOnSuccessListener(ref -> {
                    userRef = ref;
                    loadProfile();
                });
    }

    private void loadProfile() {
        userRef.get().addOnSuccessListener(doc -> {

            String n = doc.getString("name");
            String e = doc.getString("email");
            String p = doc.getString("phone");
            currentMode = doc.getString("mode");
            Boolean approved = doc.getBoolean("isOrganizerApproved");

            isOrganizerApproved = approved != null && approved;

            Long j = doc.getLong("joinedCount");
            Long w = doc.getLong("winsCount");

            fullName.setText(n == null ? "" : n);
            email.setText(e == null ? "" : e);
            phone.setText(p == null ? "" : p);
            headerName.setText(TextUtils.isEmpty(n) ? "Entrant" : n);
            joinedCount.setText(String.valueOf(j == null ? 0 : j));
            winsCount.setText(String.valueOf(w == null ? 0 : w));

            roleBadge.setText(currentMode.equals("organizer") ? "Organizer" : "Entrant");

            // Sync switch
            modeSwitch.setOnCheckedChangeListener(null);
            modeSwitch.setChecked(currentMode.equals("organizer"));
            modeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onModeToggled(isChecked));

            Boolean notifs = doc.getBoolean("notificationsEnabled");
            btnNotifSettings.setText((notifs == null || notifs) ? "Notifications Enabled" : "Notifications Disabled");

        });
    }

    // ---------------------------------------------------------
    // SAVE
    // ---------------------------------------------------------
    private void saveProfile() {
        if (userRef == null) return;

        Map<String, Object> upd = new HashMap<>();
        upd.put("name", fullName.getText().toString());
        upd.put("email", email.getText().toString());
        upd.put("phone", phone.getText().toString());

        userRef.set(upd, SetOptions.merge())
                .addOnSuccessListener(x -> {
                    headerName.setText(fullName.getText().toString());
                    setEditing(false);
                    Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                });
    }

    // ---------------------------------------------------------
    // ORGANIZER MODE TOGGLE
    // ---------------------------------------------------------
    private void onModeToggled(boolean wantOrganizer) {

        if (wantOrganizer) {
            if (!isOrganizerApproved) {
                Toast.makeText(getContext(),
                        "Your organizer access has been revoked.",
                        Toast.LENGTH_LONG).show();
                modeSwitch.setChecked(false);
                return;
            }

            saveAsOrganizer();
            launchOrganizerDashboard();

        } else {   // switch back to entrant
            saveAsEntrant();
            Toast.makeText(getContext(), "Switched to entrant mode", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAsOrganizer() {
        if (userRef == null) return;

        userRef.update("mode", "organizer");

        requireActivity().getSharedPreferences("aurora_prefs", 0)
                .edit()
                .putString("user_mode", "organizer")
                .apply();

        roleBadge.setText("Organizer");
    }

    private void saveAsEntrant() {
        if (userRef == null) return;

        userRef.update("mode", "entrant");

        requireActivity().getSharedPreferences("aurora_prefs", 0)
                .edit()
                .putString("user_mode", "entrant")
                .apply();

        roleBadge.setText("Entrant");
    }

    private void launchOrganizerDashboard() {
        Intent i = new Intent(requireContext(), UnifiedNavigationActivity.class);
        i.putExtra("openOrganizerTab", true);
        startActivity(i);
        requireActivity().finish();
    }

    // ---------------------------------------------------------
    // NOTIFICATIONS
    // ---------------------------------------------------------
    private void toggleNotifications() {
        if (userRef == null) return;

        userRef.get().addOnSuccessListener(doc -> {
            boolean curr = doc.getBoolean("notificationsEnabled") == null
                    || doc.getBoolean("notificationsEnabled");

            boolean newValue = !curr;

            userRef.update("notificationsEnabled", newValue);

            btnNotifSettings.setText(newValue ? "Notifications Enabled" : "Notifications Disabled");
        });
    }

    // ---------------------------------------------------------
    // DELETE ACCOUNT
    // ---------------------------------------------------------
    private void deleteAccount() {
        if (userRef == null) return;

        userRef.delete()
                .addOnSuccessListener(x -> {
                    Toast.makeText(getContext(), "Account deleted", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(requireContext(), LoginActivity.class));
                    requireActivity().finish();
                });
    }

    // ---------------------------------------------------------
    // EDITING UI
    // ---------------------------------------------------------
    private void setEditing(boolean editing) {
        fullName.setEnabled(editing);
        email.setEnabled(editing);
        phone.setEnabled(editing);
        btnSave.setVisibility(editing ? View.VISIBLE : View.GONE);
        editToggle.setVisibility(editing ? View.GONE : View.VISIBLE);
    }
}
