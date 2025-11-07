package com.example.aurora;

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

import com.google.firebase.auth.FirebaseAuth;

public class WelcomeActivity extends AppCompatActivity {

    private View root;
    private View tapAnywhere;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        FirebaseAuth.getInstance().signOut();
        root = findViewById(R.id.welcomeRoot);
        tapAnywhere = findViewById(R.id.tapAnywhere);

        // HANDLE DEEP LINK if opened from QR
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

        // IF USER ALREADY LOGGED IN, GO STRAIGHT TO HOME / EVENT
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

            // route to correct dashboard if no pending event
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
