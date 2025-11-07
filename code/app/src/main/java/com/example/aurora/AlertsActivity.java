package com.example.aurora;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Displays the alerts screen for entrants in the Aurora app.
 *
 * <p>This activity shows important notifications or alerts that may
 * have been sent by organizers or the system. It includes a simple
 * back button for navigation and uses the <code>fragment_alerts</code>
 * layout to display its UI content.</p>
 *
 * <p>The screen is lightweight and mainly serves as a placeholder
 * for displaying in-app alerts or messages in future versions.</p>
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
