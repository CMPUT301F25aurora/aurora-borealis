package com.example.aurora.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class EntrantNavigationActivity extends AppCompatActivity {

    private Button navEvents, navProfile, navAlerts;
    private ExtendedFloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_navigation_pager);

        // -------------------------------------------------
        // CHECK SESSION
        // -------------------------------------------------
        String userDocId = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_doc_id", null);

        if (userDocId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // -------------------------------------------------
        // UI HOOKS
        // -------------------------------------------------
        navEvents = findViewById(R.id.navEvents);
        navProfile = findViewById(R.id.navProfile);
        navAlerts = findViewById(R.id.navAlerts);
        fab = findViewById(R.id.roleSwitchFab);

        // -------------------------------------------------
        // LOAD DEFAULT SCREEN (EventsActivity)
        // -------------------------------------------------
        startActivity(new Intent(this, EventsActivity.class));
        overridePendingTransition(0,0);

        // -------------------------------------------------
        // BOTTOM NAVIGATION
        // -------------------------------------------------
        navEvents.setOnClickListener(v -> {
            startActivity(new Intent(this, EventsActivity.class));
            overridePendingTransition(0,0);
        });

        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0,0);
        });

        navAlerts.setOnClickListener(v -> {
            startActivity(new Intent(this, AlertsActivity.class));

            overridePendingTransition(0,0);
        });


        // -------------------------------------------------
        // ROLE SWITCH FAB
        // -------------------------------------------------
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    Boolean allowed = doc.getBoolean("organizer_allowed");

                    if (allowed != null && allowed) {
                        fab.show();
                    } else {
                        fab.hide();
                    }
                });

        fab.setOnClickListener(v -> {
            getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("user_last_mode", "organizer")
                    .apply();

            startActivity(new Intent(this, OrganizerActivity.class));
            finish();
        });
    }
}
