/*
 * References for this screen:
 *
 * 1) source: Android Developers — "Understand the Activity lifecycle"
 *    https://developer.android.com/guide/components/activities/activity-lifecycle
 *    Used for setting up the first screen and cleanly moving to login or sign-up flows.
 *
 * 2) author: Stack Overflow user — "How to start new activity on button click"
 *    https://stackoverflow.com/questions/4186021/how-to-start-new-activity-on-button-click
 *    Used for wiring up buttons that open LoginActivity or SignUpActivity with Intents.
 *
 * 3) source: Android Developers — "Intent and Intent Filters"
 *    https://developer.android.com/guide/components/intents-filters
 *    Used for understanding how explicit Intents are used to navigate between screens.
 *
 * 4) source: Gemini Image Generator - To generate the aurora_logo.png
 *    Used to create a team logo (mainly to help to the overall vibe of the app
 */

package com.example.aurora.activities;

/**
 * WelcomeActivity serves as the app's splash or intro screen.
 * It is typically the first screen shown when the app launches
 * Displays a simple “tap anywhere” prompt to continue.
 * When the user taps anywhere on the screen, it navigates to LoginActivity.
 */


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.R;
import com.google.firebase.auth.FirebaseAuth;

public class WelcomeActivity extends AppCompatActivity {
    private View root;
    private View tapAnywhere;

    /**
     * Sets up the welcome screen, handles deep-link events,
     * checks whether the user is already logged in,
     * and routes them either to the correct home/dashboard or to LoginActivity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        FirebaseAuth.getInstance().signOut();
        root = findViewById(R.id.welcomeRoot);
        tapAnywhere = findViewById(R.id.tapAnywhere);

        Intent incomingIntent = getIntent();
        if (incomingIntent != null && incomingIntent.getData() != null) {
            Uri uri = incomingIntent.getData();
            if (uri != null && uri.toString().startsWith("aurora://event/")) {
                String eventId = uri.toString().substring("aurora://event/".length());
                getSharedPreferences("aurora", MODE_PRIVATE)
                        .edit()
                        .putString("pending_event", eventId)
                        .apply();
            }
        }

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
        String role = sp.getString("user_role", null);

        if (role != null && !role.isEmpty()) {
            // user is logged in
            String pending = getSharedPreferences("aurora", MODE_PRIVATE)
                    .getString("pending_event", null);

            if (pending != null) {
                getSharedPreferences("aurora", MODE_PRIVATE)
                        .edit().remove("pending_event").apply();

                Intent i = new Intent(this, EventDetailsActivity.class);
                i.putExtra("eventId", pending);
                startActivity(i);
                finish();
                return;
            }
            Intent next;
            if (role.equalsIgnoreCase("organizer")) next = new Intent(this, OrganizerActivity.class);
            else if (role.equalsIgnoreCase("admin")) next = new Intent(this, AdminActivity.class);
            else next = new Intent(this, EventsActivity.class);

            startActivity(next);
            finish();
            return;
        }
        View.OnClickListener goToLogin = v -> {
            Intent i = new Intent(WelcomeActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        };

        root.setOnClickListener(goToLogin);
        tapAnywhere.setOnClickListener(goToLogin);
    }
}
