package com.example.aurora;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Simple alerts screen for entrants.
 */
public class AlertsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_alerts);

        ImageButton back = findViewById(R.id.backButtonAlerts);
        if (back != null) {
            back.setOnClickListener(v -> onBackPressed());
        }
    }
}
