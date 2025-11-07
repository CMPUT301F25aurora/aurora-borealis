/**
 * AlertsActivity.java
 *
 * This activity displays the alerts screen in the Aurora app.
 * It loads the layout defined in fragment_alerts.xml when the activity is created.
 * The class extends AppCompatActivity, meaning it uses Android's support library
 * features for backward compatibility and lifecycle management.
 */


package com.example.aurora;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class AlertsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_alerts);
    }
}
