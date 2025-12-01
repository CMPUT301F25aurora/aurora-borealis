/**
 * EntrantNavigationActivity.java
 *
 * Entry point for entrant users. Handles:
 * - Validating active session
 * - Routing to Events, Profile, and Alerts screens
 * - Displaying a role-switch button if the user has organizer privileges
 */

package com.example.aurora.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * EntrantNavigationActivity
 *
 * Main navigation hub for entrant users.
 * Handles:
 *  - session check
 *  - navigation to Events, Profile, and Alerts screens
 *  - showing role-switch FAB if the user is allowed to be an organizer
 */
public class EntrantNavigationActivity extends AppCompatActivity {

    private Button navEvents, navProfile, navAlerts;
    private ExtendedFloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_navigation_pager);


        String userDocId = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_doc_id", null);

        if (userDocId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        navEvents = findViewById(R.id.navEvents);
        navProfile = findViewById(R.id.navProfile);
        navAlerts = findViewById(R.id.navAlerts);
        fab = findViewById(R.id.roleSwitchFab);

        startActivity(new Intent(this, EventsActivity.class));
        overridePendingTransition(0,0);

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
